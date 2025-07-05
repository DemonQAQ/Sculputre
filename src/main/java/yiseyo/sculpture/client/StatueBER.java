package yiseyo.sculpture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

public class StatueBER implements BlockEntityRenderer<StatueBlockEntity>
{
    public StatueBER(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(StatueBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!be.hasMesh()) return; // 若当前还没有捕获的网格数据则不渲染
        MeshCapture.CaptureResult mesh = MeshCompressor.decompress(be.meshBytes());
        if (mesh == null) return;
        // 遍历每个纹理层的顶点列表（通常只有一层）
        Matrix4f matrix = poseStack.last().pose();
        for (Map.Entry<ResourceLocation, List<MeshCapture.Vertex>> entry : mesh.meshByTexture().entrySet()) {
            ResourceLocation texture = entry.getKey();
            List<MeshCapture.Vertex> vertices = entry.getValue();
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
            for (MeshCapture.Vertex v : vertices) {
                // 提取颜色和光照信息
                int argb = v.colorARGB();
                int a = (argb >> 24) & 0xFF, r = (argb >> 16) & 0xFF,
                        g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                int overlayUV = v.overlayPacked();
                int overlayU = overlayUV & 0xFFFF, overlayV = (overlayUV >> 16) & 0xFFFF;
                int lightUV = packedLight;
                int lightU = lightUV & 0xFFFF, lightV = (lightUV >> 16) & 0xFFFF;
                // 输出每个顶点到渲染缓冲
                vc.vertex(matrix, v.x(), v.y(), v.z())
                        .color(r, g, b, a)
                        .uv(v.u(), v.v())
                        .overlayCoords(overlayU, overlayV)
                        .uv2(lightU, lightV)
                        .normal(v.nx(), v.ny(), v.nz())
                        .endVertex();
            }
        }
    }
}