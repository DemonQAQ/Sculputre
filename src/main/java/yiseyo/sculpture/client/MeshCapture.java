package yiseyo.sculpture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static yiseyo.sculpture.client.FieldUtil.*;

/**
 * Utility for grabbing an entity's baked mesh at runtime.
 * <p>
 * The capturer works by rendering the entity into a fake {@link VertexConsumer}
 * that records every vertex it receives, grouped by {@link RenderType}.
 * After the render pass finishes we return a {@link CaptureResult} containing
 * all vertices organised per RenderType, ready for compression or direct rendering.
 */
public class MeshCapture
{

    /* ───────── Vertex record ───────── */
    public record Vertex(
            float x, float y, float z,
            float u, float v,
            int colorARGB,
            int lightPacked,
            int overlayPacked,
            float nx, float ny, float nz)
    {
    }

    /* ───────── Result wrapper ───────── */
    public static class CaptureResult
    {
        private final Map<RenderType, List<Vertex>> byRenderType;

        public CaptureResult(Map<RenderType, List<Vertex>> map)
        {
            this.byRenderType = Map.copyOf(map);
        }

        public Map<RenderType, List<Vertex>> mesh()
        {
            return byRenderType;
        }

        public boolean isEmpty()
        {
            return byRenderType.isEmpty();
        }
    }

    /* ───────── Capturing consumer ───────── */
    public static class CapturingConsumer implements VertexConsumer
    {
        private final RenderType renderType;
        private final List<Vertex> out = new ArrayList<>();
        // working vars
        private float x, y, z, u, v;
        private int colorARGB = 0xFFFFFFFF, lightPacked, overlayPacked;
        private float nx = 0, ny = 1, nz = 0;

        public CapturingConsumer(RenderType rt)
        {
            this.renderType = rt;
        }

        public RenderType renderType()
        {
            return renderType;
        }

        public List<Vertex> vertices()
        {
            return out;
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
        public VertexConsumer color(int r, int g, int b, int a)
        {
            this.colorARGB = FastColor.ARGB32.color(a, r, g, b);
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
        public VertexConsumer overlayCoords(int u, int v)
        {
            this.overlayPacked = (v << 16) | (u & 0xFFFF);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v)
        {
            this.lightPacked = (v << 16) | (u & 0xFFFF);
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
            out.add(new Vertex(x, y, z, u, v, colorARGB, lightPacked, overlayPacked, nx, ny, nz));
        }

        @Override
        public void defaultColor(int r, int g, int b, int a)
        {
        }

        @Override
        public void unsetDefaultColor()
        {
        }
    }

    /* ───────── capture() ───────── */
    public static CaptureResult capture(CompoundTag nbt,
                                        ClientLevel level,
                                        Pose pose,
                                        float bodyYaw,
                                        float headYaw)
    {

        Entity entity = EntityTypeByNbt.load(nbt, level);
        if (entity == null) return new CaptureResult(Map.of());

        entity.moveTo(Vec3.ZERO);
        entity.setPose(pose);
        entity.setYRot(bodyYaw);
        if (entity instanceof LivingEntity living)
        {

            /* ── 1. 同步身体 / 头旋转 ─────────────────────────────── */
            living.yBodyRot = living.yBodyRotO = bodyYaw;
            living.setYHeadRot(headYaw);
            living.yHeadRotO = headYaw;

            /* ── 2. 反射写入 run / oRun ──────────────────────────── */
            try
            {
                if (nbt.contains("RunPos")) RUN_F.setFloat(living, nbt.getFloat("RunPos"));
                if (nbt.contains("RunPosO")) ORUN_F.setFloat(living, nbt.getFloat("RunPosO"));

                // WalkAnimationState
                WalkAnimationState was = living.walkAnimation;
                if (nbt.contains("WalkPos")) WALK_POS_F.setFloat(was, nbt.getFloat("WalkPos"));
                if (nbt.contains("WalkSpd")) WALK_SPD_F.setFloat(was, nbt.getFloat("WalkSpd"));
                if (nbt.contains("WalkSpdO")) WALK_SPDOLD_F.setFloat(was, nbt.getFloat("WalkSpdO"));
            } catch (Exception ignored)
            {
            }  // 字段变动时仍继续流程

            /* ── 4. 可选：推进几 tick 让腿保持动态 ──────────────── */
            // for (int i = 0; i < 2; i++) living.tick();
        }

        MeshBufferSource recorder = new MeshBufferSource();
        PoseStack ps = new PoseStack();
        ps.translate(0.5, 0, 0.5);

        Minecraft mc = Minecraft.getInstance();
        mc.getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, mc.getFrameTime(), ps, recorder, 0xF000F0);

        return new CaptureResult(recorder.freeze());
    }

    /* ───────── helper buffer source ───────── */
    private static class MeshBufferSource implements MultiBufferSource
    {
        private final Map<RenderType, CapturingConsumer> map = new HashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType type)
        {
            return map.computeIfAbsent(type, CapturingConsumer::new);
        }

        public Map<RenderType, List<Vertex>> freeze()
        {
            Map<RenderType, List<Vertex>> out = new HashMap<>();
            map.forEach((rt, cc) -> out.put(rt, List.copyOf(cc.vertices())));
            return out;
        }
    }

    /* ───────── spawn entity helper ───────── */
    private static class EntityTypeByNbt
    {
        @Nullable
        static Entity load(CompoundTag tag, ClientLevel lvl)
        {
            try
            {
                return EntityType.loadEntityRecursive(tag, lvl, e -> e);
            } catch (Exception e)
            {
                return null;
            }
        }
    }
}
