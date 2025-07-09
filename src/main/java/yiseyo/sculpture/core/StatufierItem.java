package yiseyo.sculpture.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import yiseyo.sculpture.common.ModBlocks;

import static yiseyo.sculpture.util.FieldUtil.*;

public final class StatufierItem extends Item
{

    public StatufierItem(Properties props)
    {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand)
    {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = target.blockPosition();

        // 1. 采集实体 NBT + 姿态
        CompoundTag nbt = new CompoundTag();
        target.saveWithoutId(nbt);

        nbt.putString("id",              // ★ 关键行：写回实体类型
                ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString());

        // run / oRun
        try
        {
            nbt.putFloat("RunPos", RUN_F.getFloat(target));
            nbt.putFloat("RunPosO", ORUN_F.getFloat(target));

            WalkAnimationState was = target.walkAnimation;
            nbt.putFloat("WalkPos", WALK_POS_F.getFloat(was));
            nbt.putFloat("WalkSpd", WALK_SPD_F.getFloat(was));
            nbt.putFloat("WalkSpdO", WALK_SPDOLD_F.getFloat(was));

        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }

        Pose pose = target.getPose();
        float bodyYaw = target.yBodyRot;
        float headYaw = target.yHeadRot;

        // 2. 放置雕像方块
        BlockState state = ModBlocks.STATUE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);

        if (level.getBlockEntity(pos) instanceof StatueBlockEntity be)
        {
            be.setEntityData(nbt, pose, bodyYaw, headYaw);
            be.setChanged();
            CaptureManager.pendingCapturePacket(player, level, pos);
        }
        // 4. 耗损物品
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.CONSUME;

    }

}