package yiseyo.sculpture.core.manager.capture;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import yiseyo.sculpture.core.controller.capture.EntityInfoController;
import yiseyo.sculpture.core.data.capture.CaptureResult;
import yiseyo.sculpture.core.data.capture.MeshBufferSource;
import yiseyo.sculpture.core.net.ModNet;
import yiseyo.sculpture.core.net.packet.S2CRequestCapture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public abstract class CaptureManager
{
    private static final HashMap<UUID, PendingCapture> PENDING = new HashMap<>();

    public static CaptureResult capture(CompoundTag nbt, ClientLevel level, Pose pose, float bodyYaw, float headYaw)
    {
        Entity entity = load(nbt, level);
        if (entity == null) return new CaptureResult(Map.of());

        if (entity instanceof LivingEntity living)
        {
            EntityInfoController.deserializeEntity(living, nbt);
        }

        entity.moveTo(Vec3.ZERO);
        entity.setPose(pose);
        entity.setYRot(bodyYaw);
        if (entity instanceof LivingEntity living)
        {
            living.yBodyRot = living.yBodyRotO = bodyYaw;
            living.setYHeadRot(headYaw);
            living.yHeadRotO = headYaw;
        }

        MeshBufferSource recorder = new MeshBufferSource();
        PoseStack ps = new PoseStack();
        ps.translate(0.5, 0, 0.5);

        Minecraft mc = Minecraft.getInstance();
        mc.getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, mc.getFrameTime(), ps, recorder, 0xF000F0);

        return new CaptureResult(recorder.freeze());
    }

    private static Entity load(CompoundTag tag, ClientLevel lvl)
    {
        try
        {
            return EntityType.loadEntityRecursive(tag, lvl, e -> e);
        } catch (Exception e)
        {
            return null;
        }
    }

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
