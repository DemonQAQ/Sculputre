package yiseyo.sculpture.client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yiseyo.sculpture.Sculpture;

import java.util.function.Supplier;

public record C2SUploadMesh(BlockPos pos, byte[] mesh)
{
    public static void encode(C2SUploadMesh msg, FriendlyByteBuf buf) {
        try {
            buf.writeBlockPos(msg.pos);
            buf.writeByteArray(msg.mesh);
            Sculpture.LOGGER.info("encode mesh {} bytes", msg.mesh.length);
        } catch (Exception e) {
            Sculpture.LOGGER.error("encode fail", e);
        }
    }

    public static C2SUploadMesh decode(FriendlyByteBuf buf) {
        try {
            BlockPos pos = buf.readBlockPos();
            byte[] mesh  = buf.readByteArray();
            Sculpture.LOGGER.info("decode mesh {} bytes", mesh.length);
            return new C2SUploadMesh(pos, mesh);
        } catch (Exception e) {
            Sculpture.LOGGER.error("decode fail", e);
            throw e;
        }
    }

    public static void handle(C2SUploadMesh msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            Sculpture.LOGGER.info("3");
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