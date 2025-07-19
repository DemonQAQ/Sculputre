package yiseyo.sculpture.core.net;

import io.netty.buffer.Unpooled;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import yiseyo.sculpture.core.data.capture.CaptureResult;
import yiseyo.sculpture.core.data.capture.Vertex;
import yiseyo.sculpture.core.manager.render.LayerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MeshCompressor
{
    public static byte[] compress(CaptureResult res)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        Map<RenderType, List<Vertex>> mesh = res.mesh();

        buf.writeVarInt(mesh.size());                           // 1. 图层数量

        mesh.forEach((rt, verts) ->
        {
            LayerManager.writeHeader(rt, buf);            // 2. RenderType 头部
            buf.writeVarInt(verts.size());                      // 3. 顶点计数
            verts.forEach(v -> writeVertex(buf, v));            // 4. 顶点序列
        });

        // 拷贝结果
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static CaptureResult decompress(byte[] data)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int layerCount = buf.readVarInt();

        Map<RenderType, List<Vertex>> mesh = new HashMap<>(layerCount);

        for (int i = 0; i < layerCount; i++)
        {
            RenderType rt = LayerManager.readHeader(buf); // 1. 读头部 → 得到 RenderType
            int vCount = buf.readVarInt();                   // 2. 顶点计数
            List<Vertex> vs = new ArrayList<>(vCount);

            for (int j = 0; j < vCount; j++) vs.add(readVertex(buf)); // 3. 顶点序列

            mesh.put(rt, vs);
        }

        return new CaptureResult(mesh);
    }

    private static void writeVertex(FriendlyByteBuf buf, Vertex v)
    {
        buf.writeFloat(v.x());
        buf.writeFloat(v.y());
        buf.writeFloat(v.z());
        buf.writeFloat(v.u());
        buf.writeFloat(v.v());
        buf.writeInt(v.colorARGB());
        buf.writeInt(v.lightPacked());
        buf.writeInt(v.overlayPacked());
        buf.writeFloat(v.nx());
        buf.writeFloat(v.ny());
        buf.writeFloat(v.nz());
    }

    private static Vertex readVertex(FriendlyByteBuf buf)
    {
        float x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat();
        float u = buf.readFloat(), v = buf.readFloat();
        int c = buf.readInt(), light = buf.readInt(), ovl = buf.readInt();
        float nx = buf.readFloat(), ny = buf.readFloat(), nz = buf.readFloat();
        return new Vertex(x, y, z, u, v, c, light, ovl, nx, ny, nz);
    }
}
