package yiseyo.sculpture.core.manager.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import yiseyo.sculpture.core.controller.render.LayerHeaderAccessor;
import yiseyo.sculpture.core.controller.render.accessor.VanillaLayerAccessor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class LayerManager
{
    private static final List<LayerHeaderAccessor> CODECS = new CopyOnWriteArrayList<>();

    static
    {
        register(new VanillaLayerAccessor());
    }

    public static void register(LayerHeaderAccessor codec)
    {
        CODECS.add(codec);
    }

    public static void writeHeader(RenderType rt, FriendlyByteBuf buf)
    {
        int id = find(rt);
        buf.writeByte(id);                    // <‑‑ 先写出“使用第几个 Codec”
        CODECS.get(id).encode(rt, buf);       // 再交给 Codec 写具体头部
    }

    public static RenderType readHeader(FriendlyByteBuf buf)
    {
        int id = buf.readUnsignedByte();
        return CODECS.get(id).decode(buf);
    }

    /* ---------- 内部 ---------- */

    private static int find(RenderType rt)
    {
        for (int i = 0; i < CODECS.size(); i++)
            if (CODECS.get(i).supports(rt))
                return i;
        throw new IllegalStateException("No LayerHeaderCodec for " + rt);
    }
}
