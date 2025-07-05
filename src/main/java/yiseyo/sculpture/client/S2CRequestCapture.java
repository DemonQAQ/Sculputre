package yiseyo.sculpture.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CRequestCapture(BlockPos pos)
{
    public static void encode(S2CRequestCapture msg, FriendlyByteBuf buf)
    {
        buf.writeBlockPos(msg.pos);
    }

    public static S2CRequestCapture decode(FriendlyByteBuf buf)
    {
        return new S2CRequestCapture(buf.readBlockPos());
    }

    public static void handle(S2CRequestCapture msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            System.out.println("114514");
            var level = Minecraft.getInstance().level;
            if (level.getBlockEntity(msg.pos) instanceof StatueBlockEntity be && !be.hasMesh())
            {
                var result = MeshCapture.capture(
                        be.entityNbt(), level, be.pose(), be.bodyYaw(), be.headYaw());
                byte[] bytes = MeshCompressor.compress(result);
                be.acceptMesh(bytes);
                ModNet.CHANNEL.sendToServer(new C2SUploadMesh(msg.pos(), bytes));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}