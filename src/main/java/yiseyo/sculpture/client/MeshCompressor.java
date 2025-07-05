package yiseyo.sculpture.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.netty.buffer.Unpooled;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Raw binary compressor / decompressor for MeshCapture results.
 * <p>
 * Wire-format:<br>
 * {@code VarInt layerCount}<br>
 * repeat layerCount × {<br>
 * &nbsp;&nbsp;{@code ResourceLocation texture}<br>
 * &nbsp;&nbsp;{@code VarInt vertexCount}<br>
 * &nbsp;&nbsp;vertexCount × packed Vertex<br>
 * }<br>
 * <br>
 * Vertex fields are stored in the exact order declared in {@link MeshCapture.Vertex}.
 */
public final class MeshCompressor {

    /* ───────── main API ───────── */

    public static byte[] compress(MeshCapture.CaptureResult res) {

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        Map<RenderType, List<MeshCapture.Vertex>> mesh = res.mesh();
        buf.writeVarInt(mesh.size());

        mesh.forEach((rt, list) -> {
            ResourceLocation tex = textureOf(rt);
            buf.writeResourceLocation(tex);
            buf.writeVarInt(list.size());
            for (MeshCapture.Vertex v : list) {
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

        FriendlyByteBuf buf   = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int layerCount        = buf.readVarInt();
        Map<RenderType, List<MeshCapture.Vertex>> mesh = new HashMap<>(layerCount);

        for (int i = 0; i < layerCount; i++) {
            ResourceLocation tex = buf.readResourceLocation();
            int vCount           = buf.readVarInt();
            List<MeshCapture.Vertex> list = new ArrayList<>(vCount);

            for (int j = 0; j < vCount; j++) {
                float x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat();
                float u = buf.readFloat(), v = buf.readFloat();
                int   c = buf.readInt(),    light = buf.readInt(), ovl = buf.readInt();
                float nx = buf.readFloat(), ny = buf.readFloat(), nz = buf.readFloat();
                list.add(new MeshCapture.Vertex(x, y, z, u, v, c, light, ovl, nx, ny, nz));
            }

            // Re-create a RenderType for this layer (entity cut-out no-cull is good enough here)
            RenderType rt = RenderType.entityCutoutNoCull(tex);
            mesh.put(rt, list);
        }
        return new MeshCapture.CaptureResult(mesh);
    }

    /* ───────── private helpers ───────── */

    /** Try to access the main texture bound to a RenderType via reflection. */
    private static ResourceLocation textureOf(RenderType rt) {
        try {
            // CompositeRenderType -> CompositeState -> TextureStateShard -> location
            Field stateF = ObfuscationReflectionHelper.findField(RenderType.class, "f_289963_");
            Object state = stateF.get(rt);
            Field texF   = ObfuscationReflectionHelper.findField(state.getClass(), "f_173254_");
            Object texState = texF.get(state);
            Field locF  = ObfuscationReflectionHelper.findField(texState.getClass(), "f_78917_");
            return (ResourceLocation) locF.get(texState);
        }
        catch (Exception e) {
            /* ───── Fallback: build a safe dummy RL from RenderType.toString() ───── */
            String raw  = rt.toString().toLowerCase(Locale.ROOT);
            // 只保留合法字符，其余全部替换成 '_'
            String safe = raw.replaceAll("[^a-z0-9/._-]", "_");
            return new ResourceLocation("dummy", safe);
        }
    }

    // Prevent instantiation
    private MeshCompressor() {}
}
