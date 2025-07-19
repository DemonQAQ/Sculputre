// TestCommand.java
package yiseyo.sculpture.common;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import yiseyo.sculpture.core.controller.capture.EntityInfoController;
import yiseyo.sculpture.core.manager.capture.CaptureManager;
import yiseyo.sculpture.core.world.StatueBlockEntity;

public final class TestCommand
{
    private static final double REACH = 20.0D;   // 射线最大距离

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                Commands.literal("capture")
                        .requires(cs -> cs.hasPermission(2)) // OP
                        .executes(ctx -> capture(ctx.getSource()))
        );
    }

    private static int capture(CommandSourceStack source)
    {
        ServerPlayer player;
        try
        {
            player = source.getPlayerOrException();
        } catch (Exception e)
        {
            source.sendFailure(Component.literal("§c必须由玩家执行此命令！"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 reachEnd = eye.add(look.scale(REACH));

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                level, player, eye, reachEnd,
                player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0D),
                e -> e.isPickable() && (e instanceof LivingEntity || e instanceof EnderDragonPart));

        if (hit == null)
        {
            source.sendFailure(Component.literal("§e未捕捉到任何实体。"));
            return 0;
        }

        LivingEntity target = (LivingEntity) hit.getEntity();
        BlockPos pos = target.blockPosition();

        CompoundTag entityData = EntityInfoController.serializeEntity(target);
        Pose pose = target.getPose();
        float bodyYaw = target.yBodyRot;
        float headYaw = target.yHeadRot;

        BlockState state = ModBlocks.STATUE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);

        if (level.getBlockEntity(pos) instanceof StatueBlockEntity be)
        {
            be.setEntityData(entityData, pose, bodyYaw, headYaw);
            be.setChanged();
            CaptureManager.pendingCapturePacket(player, level, pos);
        }

        source.sendSuccess(
                () -> Component.literal("§a已捕捉实体并生成雕像: ").append(target.getName()), true);
        return 1;
    }

    private TestCommand()
    {
    }
}
