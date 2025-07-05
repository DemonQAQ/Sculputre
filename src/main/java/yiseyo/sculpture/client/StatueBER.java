package yiseyo.sculpture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class StatueBER implements BlockEntityRenderer<StatueBlockEntity>
{
    @Override
    public void render(StatueBlockEntity be, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buf, int light, int overlay) {
        if (be.renderEntity == null) {
            // 构造一次，放进虚拟世界以获得正确的模型层级资源
            be.renderEntity = (LivingEntity) EntityType.loadEntityRecursive(
                    be.cachedEntityTag, Minecraft.getInstance().level);
            be.renderEntity.setPos(be.getBlockPos().getX() + 0.5,
                    be.getBlockPos().getY(),
                    be.getBlockPos().getZ() + 0.5);
            be.renderEntity.setPose(be.cachedPose);
            be.renderEntity.yBodyRot = be.bodyYaw;
            be.renderEntity.yHeadRot = be.headYaw;
            be.renderEntity.setNoAi(true);         // 彻底静止
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);          // 方块中心
        Minecraft.getInstance().getEntityRenderDispatcher()
                .render(be.renderEntity, 0, 0, 0, 0, 0,
                        poseStack, buf, light);
        poseStack.popPose();
    }
}