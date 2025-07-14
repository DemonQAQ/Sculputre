package yiseyo.sculpture.core.controller.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;

public interface LayerHeaderAccessor
{
    boolean supports(RenderType rt);

    /** 将 rt 的「头部信息」写入 buf。调用者会在后面继续写顶点数据。 */
    void encode(RenderType rt, FriendlyByteBuf buf);

    /** 从 buf 读取头部并构造 RenderType，buf 的读指针应停在顶点计数之前。 */
    RenderType decode(FriendlyByteBuf buf);
}
