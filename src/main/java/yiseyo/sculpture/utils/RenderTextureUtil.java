package yiseyo.sculpture.utils;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public abstract class RenderTextureUtil
{
    /* ---------- 私有工具 ---------- */

    public static ResourceLocation textureOf(RenderType rt)
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
}
