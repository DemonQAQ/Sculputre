package yiseyo.sculpture.client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SUploadMesh(BlockPos pos, byte[] mesh)
{
    public static void encode(C2SUploadMesh msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
        buf.writeByteArray(msg.mesh);
    }

    public static C2SUploadMesh decode(FriendlyByteBuf buf)
    {
        return new C2SUploadMesh(buf.readBlockPos(), buf.readByteArray());
    }

    public static void handle(C2SUploadMesh msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            var player = ctx.get().getSender();
            var level = player.serverLevel();
            if (level.getBlockEntity(msg.pos) instanceof StatueBlockEntity be && !be.hasMesh())
            {
                be.acceptMesh(msg.mesh);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}