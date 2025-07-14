package yiseyo.sculpture.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;
import yiseyo.sculpture.core.data.capture.CaptureResult;
import yiseyo.sculpture.core.data.capture.Vertex;
import yiseyo.sculpture.core.world.StatueBlockEntity;
import yiseyo.sculpture.core.net.MeshCompressor;

import java.util.List;
import java.util.Map;

/** Block-entity renderer that draws the baked mesh stored inside {@link StatueBlockEntity}. */
public class StatueBER implements BlockEntityRenderer<StatueBlockEntity> {

    public StatueBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(StatueBlockEntity be, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {

        if (!be.hasMesh()) return;

        CaptureResult mesh = MeshCompressor.decompress(be.meshBytes());
        if (mesh == null || mesh.isEmpty()) return;

        Matrix4f matrix = poseStack.last().pose();

        // 新接口：按 RenderType 分层
        for (Map.Entry<RenderType, List<Vertex>> entry : mesh.mesh().entrySet()) {

            RenderType rt                  = entry.getKey();
            List<Vertex> verts = entry.getValue();
            VertexConsumer vc              = buffer.getBuffer(rt);

            for (Vertex v : verts) {

                int argb = v.colorARGB();
                int a = (argb >>> 24) & 0xFF,
                        r = (argb >>> 16) & 0xFF,
                        g = (argb >>>  8) & 0xFF,
                        b =  argb         & 0xFF;

                int ovl = v.overlayPacked();
                int ovlU =  ovl        & 0xFFFF,
                        ovlV = (ovl >>> 16) & 0xFFFF;

                int ltU =  v.lightPacked()        & 0xFFFF,
                        ltV = (v.lightPacked() >>> 16) & 0xFFFF;

                vc.vertex(matrix, v.x(), v.y(), v.z())
                        .color(r, g, b, a)
                        .uv(v.u(), v.v())
                        .overlayCoords(ovlU, ovlV)
                        .uv2(ltU, ltV)
                        .normal(v.nx(), v.ny(), v.nz())
                        .endVertex();
            }
        }
    }
}
