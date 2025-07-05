package yiseyo.sculpture.client;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

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
        ServerPlayer sPlayer = (ServerPlayer) player;

        // 1. 采集实体 NBT + 姿态
        CompoundTag nbt = new CompoundTag();
        target.saveWithoutId(nbt);
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
            // 3. 仅告诉触发客户端去做 MeshCapture
            ModNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sPlayer),
                    new S2CRequestCapture(pos));
        }

        // 4. 耗损物品
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.CONSUME;
    }
}