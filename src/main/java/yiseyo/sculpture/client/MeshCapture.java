package yiseyo.sculpture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MeshCapture
{

    /** 单个顶点数据结构 —— 可根据需要精简字段 */
    public record Vertex(
            float x, float y, float z,
            float u, float v,
            int colorARGB,
            int lightPacked,
            int overlayPacked,
            float nx, float ny, float nz
    )
    {
    }

    /** 返回值：贴图 → 顶点列表 */
    public record CaptureResult(
            Map<ResourceLocation, List<Vertex>> meshByTexture
    )
    {
    }

    // ------------------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------------------

    /**
     * @param entityNbt 来自服务端同步的完整实体 NBT
     * @param level     当前客户端世界 (Minecraft.level)
     * @param pose      冻结姿势
     * @param bodyYaw   身体朝向
     * @param headYaw   头部朝向
     */
    public static CaptureResult capture(
            CompoundTag entityNbt,
            Level level,
            Pose pose,
            float bodyYaw,
            float headYaw
    )
    {
        // 1) 复原实体
        LivingEntity dummy = (LivingEntity) EntityType.loadEntityRecursive(
                entityNbt, level
        ).orElseThrow(() -> new IllegalStateException("Entity restore failed"));
        dummy.setPose(pose);
        dummy.yBodyRot = bodyYaw;
        dummy.yHeadRot = headYaw;
        dummy.setNoAi(true);

        // 2) 创建顶点收集器
        CapturingConsumer collector = new CapturingConsumer();

        // 3) 让所有 RenderType 共享同一个 VertexConsumer
        MultiBufferSource buf = new DelegatingBuffer(collector);

        // 4) 调用原版渲染器“离屏”渲染一帧
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PoseStack ps = new PoseStack();
        ps.translate(0.5, 0.0, 0.5);              // 以方块中心为原点
        dispatcher.render(dummy, 0.0F, 0.0F, ps, buf, 0xF000F0);
        // 无需调用 buf.endBatch(); → 所有流向同一个 consumer

        // 5) 把结果按贴图分组
        ResourceLocation tex = ((LivingEntityRenderer<?, ?>) dispatcher.getRenderer(dummy))
                .getTextureLocation(dummy);
        Map<ResourceLocation, List<Vertex>> out = Map.of(tex, collector.vertices());

        return new CaptureResult(out);
    }

    // ------------------------------------------------------------
    // INTERNAL CLASSES
    // ------------------------------------------------------------

    /**
     * 将 VertexConsumer 接口的逐属性调用打包成 Vertex 记录
     */
    private static final class CapturingConsumer implements VertexConsumer
    {

        private final List<Vertex> list = new ArrayList<>();
        // 临时累加器
        private float x, y, z, u, v, nx, ny, nz;
        private int color = -1;
        private int light, overlay;

        public List<Vertex> vertices()
        {
            return list;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z)
        {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v)
        {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a)
        {
            this.color = FastColor.ARGB32.color(a, r, g, b);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v)
        {
            this.overlay = OverlayTexture.pack(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int lightU, int lightV)
        {
            this.light = LightTexture.pack(lightU, lightV);
            return this;
        }

        @Override
        public VertexConsumer normal(float nx, float ny, float nz)
        {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            return this;
        }

        @Override
        public void endVertex()
        {
            list.add(new Vertex(x, y, z, u, v, color, light, overlay, nx, ny, nz));
        }

        // —— 其余 VertexConsumer 默认实现已经够用，无需重写 ——
    }

    /**
     * 一个极简 MultiBufferSource：
     * 不论渲染器请求哪种 RenderType，都返回同一个 VertexConsumer。
     */
    private record DelegatingBuffer(VertexConsumer single) implements MultiBufferSource
    {

        @Override
        public VertexConsumer getBuffer(RenderType type)
        {
            return single;
        }
    }
}