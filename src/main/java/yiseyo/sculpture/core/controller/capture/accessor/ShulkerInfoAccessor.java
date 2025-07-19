package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField;

public class ShulkerInfoAccessor implements IEntityInfoAccessor
{
    private static final Field PEEK_AMOUNT_F;
    private static final Field PREV_PEEK_AMOUNT_F;
    private static final Method SET_RAW_PEEK;
    private static final Method GET_RAW_PEEK;
    private static final EntityDataAccessor<Byte> DATA_PEEK_ID =
            ObfuscationReflectionHelper.getPrivateValue(Shulker.class, null, "DATA_PEEK_ID");

    static
    {
        try
        {
            PEEK_AMOUNT_F = findField(Shulker.class, "currentPeekAmount");
            PREV_PEEK_AMOUNT_F = findField(Shulker.class, "currentPeekAmountO");

            SET_RAW_PEEK = ObfuscationReflectionHelper.findMethod(Shulker.class, "setRawPeekAmount", int.class);
            GET_RAW_PEEK = ObfuscationReflectionHelper.findMethod(Shulker.class, "getRawPeekAmount");
            SET_RAW_PEEK.setAccessible(true);
            GET_RAW_PEEK.setAccessible(true);
        } catch (Exception e)
        {
            throw new RuntimeException("反射潜影贝类失败", e);
        }
    }

    @Override
    public void writeEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        if (!(entity instanceof Shulker shulker)) return;

        int rawPeek;
        try
        {
            rawPeek = (int) GET_RAW_PEEK.invoke(shulker);
        } catch (Throwable ignored)
        {
            rawPeek = shulker.getEntityData().get(DATA_PEEK_ID);
        }

        tag.putByte("ShulkerPeekRaw", (byte) rawPeek);
        tag.putFloat("ShulkerPeek", rawPeek / 100F);
    }

    /* ---------- 从 NBT 还原 ---------- */
    @Override
    public void readEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        if (!(entity instanceof Shulker shulker)) return;
        if (!tag.contains("ShulkerPeekRaw")) return;

        int rawPeek = tag.getByte("ShulkerPeekRaw") & 0xFF; // byte → 无符号

        boolean applied = false;
        try
        {
            SET_RAW_PEEK.invoke(shulker, rawPeek);
            applied = true;
        } catch (Throwable ignored)
        { /* fallback below */ }

        if (!applied)
        {
            shulker.getEntityData().set(DATA_PEEK_ID, (byte) rawPeek);
        }

        float opened = rawPeek / 100F;
        try
        {
            PEEK_AMOUNT_F.setFloat(shulker, opened);
            PREV_PEEK_AMOUNT_F.setFloat(shulker, opened);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("读取潜影贝特有字段失败", e);
        }

        // 立即刷新 HitBox，避免碰撞箱仍处于闭合尺寸
        shulker.refreshDimensions();
    }

    @Override
    public boolean isApplicableTo(LivingEntity entity)
    {
        return entity instanceof Shulker;
    }
}
