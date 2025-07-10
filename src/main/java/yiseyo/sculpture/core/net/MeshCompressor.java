package yiseyo.sculpture.core.net;

import io.netty.buffer.Unpooled;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.core.MeshCapture;

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
public final class MeshCompressor
{
    public static byte[] compress(MeshCapture.CaptureResult res)
    {

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        Map<RenderType, List<MeshCapture.Vertex>> mesh = res.mesh();
        buf.writeVarInt(mesh.size());

        mesh.forEach((rt, list) ->
        {
            ResourceLocation tex = textureOf(rt);

            Sculpture.LOGGER.info("tex = " + tex.getPath());

            buf.writeResourceLocation(tex);
            buf.writeByte(layerFlag(rt));
            buf.writeVarInt(list.size());
            for (MeshCapture.Vertex v : list)
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
        });

        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static MeshCapture.CaptureResult decompress(byte[] data)
    {

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int layerCount = buf.readVarInt();
        Map<RenderType, List<MeshCapture.Vertex>> mesh = new HashMap<>(layerCount);

        for (int i = 0; i < layerCount; i++)
        {
            ResourceLocation tex = buf.readResourceLocation();
            byte flag = buf.readByte();
            int vCount = buf.readVarInt();
            List<MeshCapture.Vertex> list = new ArrayList<>(vCount);

            for (int j = 0; j < vCount; j++)
            {
                float x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat();
                float u = buf.readFloat(), v = buf.readFloat();
                int c = buf.readInt(), light = buf.readInt(), ovl = buf.readInt();
                float nx = buf.readFloat(), ny = buf.readFloat(), nz = buf.readFloat();
                list.add(new MeshCapture.Vertex(x, y, z, u, v, c, light, ovl, nx, ny, nz));
            }

            // Re-create a RenderType for this layer (entity cut-out no-cull is good enough here)
            RenderType rt = switch (flag)
            {
                case 1 -> RenderType.entityTranslucent(tex);
                case 2 -> RenderType.entityTranslucentEmissive(tex); // 1.20.1
                default -> RenderType.entityCutoutNoCull(tex);
            };
            mesh.put(rt, list);
        }
        return new MeshCapture.CaptureResult(mesh);
    }


    /** Try to access the main texture bound to a RenderType via reflection. */
    private static ResourceLocation textureOf(RenderType rt)
    {
        try
        {
            /* 1. CompositeState */
            Object composite = Arrays.stream(rt.getClass().getDeclaredFields())
                    .filter(f -> f.getType().getSimpleName().endsWith("CompositeState"))
                    .peek(f -> f.setAccessible(true))
                    .map(f ->
                    {
                        try
                        {
                            return f.get(rt);
                        } catch (IllegalAccessException e)
                        {
                            return null;
                        }
                    })
                    .findFirst().orElseThrow();

            /* 2. TextureStateShard */
            Object texState = Arrays.stream(composite.getClass().getDeclaredFields())
                    .filter(f -> f.getType().getSimpleName().endsWith("TextureStateShard"))
                    .peek(f -> f.setAccessible(true))
                    .map(f ->
                    {
                        try
                        {
                            return f.get(composite);
                        } catch (IllegalAccessException e)
                        {
                            return null;
                        }
                    })
                    .findFirst().orElseThrow();

            /* 3. ResourceLocation OR Optional<ResourceLocation> – 向父类递归 */
            Class<?> c = texState.getClass();
            while (c != null)
            {
                for (Field f : c.getDeclaredFields())
                {
                    f.setAccessible(true);
                    Object v = f.get(texState);

                    if (v instanceof ResourceLocation rl)          // 直接拿到
                        return rl;

                    if (v instanceof Optional<?> opt               // Optional 包装
                            && opt.orElse(null) instanceof ResourceLocation rl)
                        return rl;
                }
                c = c.getSuperclass();
            }
            throw new IllegalStateException("no ResourceLocation");
        } catch (Exception e)
        {
            // fallback: 合法化 RenderType.toString()
            String safe = rt.toString()
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9/._-]", "_");
            return new ResourceLocation("dummy", safe);
        }
    }

    private static byte layerFlag(RenderType rt)
    {
        String name = rt.toString();
        if (name.contains("translucent"))
            return 1;                         // 1 = translucent
        if (name.contains("emissive") || name.contains("eyes"))
            return 2;                         // 2 = emissive/eyes (全亮)
        return 0;                             // 0 = cutout/solid
    }

    // Prevent instantiation
    private MeshCompressor()
    {
    }
}
