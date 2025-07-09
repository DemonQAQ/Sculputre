package yiseyo.sculpture.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import yiseyo.sculpture.net.ModNet;
import yiseyo.sculpture.net.packet.S2CRequestCapture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public abstract class CaptureManager
{
    private record PendingCapture(ServerLevel level, BlockPos pos, int ticks)
    {
    }

    private static final HashMap<UUID, PendingCapture> PENDING = new HashMap<>();

    public static void pendingCapturePacket(Player player, ServerLevel level, BlockPos pos)
    {
        PENDING.put(player.getUUID(), new PendingCapture(level, pos, 20));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<UUID, PendingCapture>> it = PENDING.entrySet().iterator();
        while (it.hasNext())
        {
            var entry = it.next();
            PendingCapture pc = entry.getValue();

            if (pc.ticks() - 1 <= 0)
            {
                ServerPlayer target = pc.level().getServer().getPlayerList().getPlayer(entry.getKey());
                if (target != null)
                {
                    ModNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new S2CRequestCapture(pc.pos()));
                }
                it.remove();
            }
            else
            {
                entry.setValue(new PendingCapture(pc.level(), pc.pos(), pc.ticks() - 1));
            }
        }
    }
}
