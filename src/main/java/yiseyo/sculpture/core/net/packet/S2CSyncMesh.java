package yiseyo.sculpture.core.net.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.core.StatueBlockEntity;

import java.util.function.Supplier;

public record S2CSyncMesh(BlockPos pos, byte[] mesh)
{
    public static void encode(S2CSyncMesh msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeByteArray(msg.mesh);
    }

    public static S2CSyncMesh decode(FriendlyByteBuf buf)
    {
        return new S2CSyncMesh(buf.readBlockPos(), buf.readByteArray());
    }

    public static void handle(S2CSyncMesh msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            Sculpture.LOGGER.info("4");
            var level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(msg.pos) instanceof StatueBlockEntity be)
            {
                be.acceptMesh(msg.mesh);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}