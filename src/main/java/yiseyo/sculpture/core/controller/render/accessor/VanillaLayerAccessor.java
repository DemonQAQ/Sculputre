package yiseyo.sculpture.core.controller.render.accessor;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import yiseyo.sculpture.core.controller.render.LayerHeaderAccessor;
import yiseyo.sculpture.utils.RenderTextureUtil;

/**
 * 不再静态冻结 swirl / glint 层，完全恢复原版的动态效果，
 * 同时支持捕获闪电苦力怕能量层与实体装备的附魔泛光。
 */
public final class VanillaLayerAccessor implements LayerHeaderAccessor {

    /* ---------- 渲染层标记 ---------- */
    private static final byte FLAG_CUTOUT      = 0;
    private static final byte FLAG_TRANSLUCENT = 1;
    private static final byte FLAG_EMISSIVE    = 2;
    private static final byte FLAG_ITEM_GLINT  = 3;   // entity_glint，用于武器/工具
    private static final byte FLAG_ARMOR_GLINT = 4;   // armor_entity_glint
    private static final byte FLAG_SWIRL       = 5;   // 闪电苦力怕
    private static final byte FLAG_SOLID       = 6;   // 潜影贝

    /* =============== 编码 =============== */
    @Override
    public boolean supports(RenderType rt) {
        String n = rt.toString().toLowerCase();
        return n.contains("cutout")
                || n.contains("translucent")
                || n.contains("emissive")
                || n.contains("solid")
                || n.contains("eyes")
                || n.contains("glint")          // 附魔效果层
                || n.contains("swirl");         // 能量/闪电苦力怕层
    }

    @Override
    public void encode(RenderType rt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(RenderTextureUtil.textureOf(rt));
        buf.writeByte(layerFlag(rt));
    }

    /* =============== 解码 =============== */
    @Override
    public RenderType decode(FriendlyByteBuf buf) {
        ResourceLocation tex = buf.readResourceLocation();
        byte flag = buf.readByte();
        return switch (flag) {
            case FLAG_TRANSLUCENT -> RenderType.entityTranslucent(tex);
            case FLAG_EMISSIVE    -> RenderType.entityTranslucentEmissive(tex);
            case FLAG_ITEM_GLINT  -> RenderType.entityGlint();          // 手持物品
            case FLAG_ARMOR_GLINT -> RenderType.armorEntityGlint();     // 身上的盔甲
            case FLAG_SWIRL       -> RenderType.energySwirl(tex, 0.0F, 0.0F);
            case FLAG_SOLID       -> RenderType.entitySolid(tex);
            default               -> RenderType.entityCutoutNoCull(tex);        // cutout / solid
        };
    }

    /* --------------- 工具方法 --------------- */
    private static byte layerFlag(RenderType rt) {
        String name = rt.toString().toLowerCase();
        if (name.contains("solid") && !name.contains("cutout"))
            return FLAG_SOLID;
        if (name.contains("translucent"))
            return FLAG_TRANSLUCENT;
        if (name.contains("emissive") || name.contains("eyes"))
            return FLAG_EMISSIVE;
        if (name.contains("armor_glint"))         return FLAG_ARMOR_GLINT;
        if (name.contains("glint"))               return FLAG_ITEM_GLINT;
        if (name.contains("swirl"))               return FLAG_SWIRL;
        return FLAG_CUTOUT;
    }
}
