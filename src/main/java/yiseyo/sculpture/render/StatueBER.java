package yiseyo.sculpture.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import yiseyo.sculpture.core.data.capture.CaptureResult;
import yiseyo.sculpture.core.data.capture.Vertex;
import yiseyo.sculpture.core.net.MeshCompressor;
import yiseyo.sculpture.core.world.StatueBlockEntity;
import yiseyo.sculpture.utils.RenderTextureUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Block-entity renderer that draws the baked mesh stored inside {@link StatueBlockEntity}. */
public class StatueBER implements BlockEntityRenderer<StatueBlockEntity>
{

    public StatueBER(BlockEntityRendererProvider.Context ctx)
    {
    }

    private static int layerPriority(RenderType rt) {
        String n = rt.toString().toLowerCase();
        // glint 最后，保证 depth==EQUAL 通过
        if (n.contains("glint"))        return 3;
        // 发光 / 眼睛 次之
        if (n.contains("emissive") || n.contains("eyes"))
            return 2;
        // 半透明再往后
        if (n.contains("translucent"))
            return 1;
        // 实体基础层最先绘制
        return 0;
    }

    @Override
    public void render(StatueBlockEntity be, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {

        if (!be.hasMesh()) return;
        CaptureResult mesh = MeshCompressor.decompress(be.meshBytes());
        if (mesh == null || mesh.isEmpty()) return;

        Matrix4f matrix = poseStack.last().pose();
        long time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0L);
        float swirl = (time + partialTicks) * 0.01F;

        // ---------- ① 先排序 ----------
        mesh.mesh().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> layerPriority(e.getKey())))
                .forEach(entry -> {

                    RenderType original = entry.getKey();
                    RenderType rt = original;                       // 默认保持

                    String n = original.toString().toLowerCase();
                    if (n.contains("swirl")) {                     // 动态能量层
                        ResourceLocation tex = RenderTextureUtil.textureOf(original);
                        rt = RenderType.energySwirl(tex, swirl, swirl);
                    } else if (n.contains("armor_glint") || n.contains("armor_entity_glint")) {
                        rt = RenderType.armorGlint();              // 正确的盔甲泛光 RT
                    }

                    VertexConsumer vc = buffer.getBuffer(rt);

                    for (Vertex v : entry.getValue()) {
                        int argb = v.colorARGB();
                        int a = (argb >>> 24) & 0xFF,
                                r = (argb >>> 16) & 0xFF,
                                g = (argb >>>  8) & 0xFF,
                                b =  argb         & 0xFF;

                        int ovl = v.overlayPacked();
                        vc.vertex(matrix, v.x(), v.y(), v.z())
                                .color(r, g, b, a)
                                .uv(v.u(), v.v())
                                .overlayCoords(ovl & 0xFFFF, (ovl >>> 16) & 0xFFFF)
                                .uv2(v.lightPacked() & 0xFFFF, (v.lightPacked() >>> 16) & 0xFFFF)
                                .normal(v.nx(), v.ny(), v.nz())
                                .endVertex();
                    }
                });
    }
}
