package yiseyo.sculpture.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.net.C2SUploadMesh;
import yiseyo.sculpture.net.S2CRequestCapture;
import yiseyo.sculpture.net.S2CSyncMesh;

public final class ModNet
{
    public static final String PROTO = "1";
    public static SimpleChannel CHANNEL;

    private static int id = 0;

    private static int next()
    {
        return id++;
    }

    @SuppressWarnings("removal")
    public static void init()
    {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Sculpture.MODID, "net"),
                () -> PROTO, PROTO::equals, PROTO::equals);

        CHANNEL.registerMessage(next(), S2CRequestCapture.class,
                S2CRequestCapture::encode, S2CRequestCapture::decode,
                S2CRequestCapture::handle);
        CHANNEL.registerMessage(next(), C2SUploadMesh.class,
                C2SUploadMesh::encode, C2SUploadMesh::decode,
                C2SUploadMesh::handle);
        CHANNEL.registerMessage(next(), S2CSyncMesh.class,
                S2CSyncMesh::encode, S2CSyncMesh::decode,
                S2CSyncMesh::handle);
    }
}