package yiseyo.sculpture.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import yiseyo.sculpture.Sculpture;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.UUID;

import static yiseyo.sculpture.client.FieldUtil.*;

public final class StatufierItem extends Item
{
    private record PendingCapture(ServerLevel level, BlockPos pos, int ticks) {}

    private static final Object2ObjectOpenHashMap<UUID, PendingCapture> PENDING =
            new Object2ObjectOpenHashMap<>();

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
            nbt.putFloat("RunPos",  RUN_F .getFloat(target));
            nbt.putFloat("RunPosO", ORUN_F.getFloat(target));

            WalkAnimationState was = target.walkAnimation;
            nbt.putFloat("WalkPos",  WALK_POS_F   .getFloat(was));
            nbt.putFloat("WalkSpd",  WALK_SPD_F   .getFloat(was));
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

            // 装入延迟队列
            PENDING.put(player.getUUID(),
                    new PendingCapture(level, pos, 20));
        }
        // 4. 耗损物品
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResult.CONSUME;

    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Object2ObjectMap.Entry<UUID, PendingCapture>> it = PENDING.object2ObjectEntrySet().iterator();
        while (it.hasNext())
        {
            var entry = it.next();
            PendingCapture pc = entry.getValue();

            if (pc.ticks() - 1 <= 0)
            {
                ServerPlayer target = pc.level().getServer()
                        .getPlayerList()
                        .getPlayer(entry.getKey());
                if (target != null)
                {
                    ModNet.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> target),
                            new S2CRequestCapture(pc.pos()));
                }
                it.remove();               // 任务完成
            }
            else
            {
                // 更新剩余 tick
                entry.setValue(new PendingCapture(pc.level(), pc.pos(), pc.ticks() - 1));
            }
        }
    }
}