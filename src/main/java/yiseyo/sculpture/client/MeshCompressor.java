package yiseyo.sculpture.client;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MeshCompressor
{
    public static byte[] compress(MeshCapture.CaptureResult res) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        var meshMap = res.meshByTexture();
        buf.writeVarInt(meshMap.size());
        meshMap.forEach((tex, vertices) -> {
            buf.writeResourceLocation(tex);
            buf.writeVarInt(vertices.size());
            for (MeshCapture.Vertex v : vertices) {
                buf.writeFloat(v.x()); buf.writeFloat(v.y()); buf.writeFloat(v.z());
                buf.writeFloat(v.u()); buf.writeFloat(v.v());
                buf.writeInt(v.colorARGB());
                buf.writeInt(v.lightPacked());
                buf.writeInt(v.overlayPacked());
                buf.writeFloat(v.nx()); buf.writeFloat(v.ny()); buf.writeFloat(v.nz());
            }
        });
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static MeshCapture.CaptureResult decompress(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int texCount = buf.readVarInt();
        Map<ResourceLocation, List<MeshCapture.Vertex>> meshMap = new HashMap<>();
        for (int i = 0; i < texCount; i++) {
            ResourceLocation tex = buf.readResourceLocation();
            int count = buf.readVarInt();
            List<MeshCapture.Vertex> list = new ArrayList<>(count);
            for (int j = 0; j < count; j++) {
                // 按写入顺序依次读出顶点属性
                float x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat();
                float u = buf.readFloat(), v = buf.readFloat();
                int color = buf.readInt(), light = buf.readInt(), overlay = buf.readInt();
                float nx = buf.readFloat(), ny = buf.readFloat(), nz = buf.readFloat();
                list.add(new MeshCapture.Vertex(x, y, z, u, v, color, light, overlay, nx, ny, nz));
            }
            meshMap.put(tex, list);
        }
        return new MeshCapture.CaptureResult(meshMap);
    }
}