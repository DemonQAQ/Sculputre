package yiseyo.sculpture.core.controller.render.accessor;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import yiseyo.sculpture.core.controller.render.LayerHeaderAccessor;
import yiseyo.sculpture.utils.RenderTextureUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class VanillaLayerAccessor implements LayerHeaderAccessor
{

    /* =============== 编码 =============== */
    @Override
    public boolean supports(RenderType rt)
    {
        String n = rt.toString();
        return n.contains("cutout") || n.contains("translucent") || n.contains("emissive") || n.contains("eyes");
    }

    @Override
    public void encode(RenderType rt, FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(RenderTextureUtil.textureOf(rt));
        buf.writeByte(layerFlag(rt));
    }

    /* =============== 解码 =============== */
    @Override
    public RenderType decode(FriendlyByteBuf buf)
    {
        ResourceLocation tex = buf.readResourceLocation();
        byte flag = buf.readByte();
        return switch (flag)
        {
            case 1 -> RenderType.entityTranslucent(tex);
            case 2 -> RenderType.entityTranslucentEmissive(tex);
            default -> RenderType.entityCutoutNoCull(tex);
        };
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

}