package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

import java.lang.reflect.Field;

public class LivingEntityInfoAccessor implements IEntityInfoAccessor
{
    private static final Field RUN_F;
    private static final Field ORUN_F;

    private static final Field WALK_POS_F;
    private static final Field WALK_SPD_F;
    private static final Field WALK_SPDOLD_F;

    static
    {
        try
        {
            RUN_F = LivingEntity.class.getDeclaredField("run");
            ORUN_F = LivingEntity.class.getDeclaredField("oRun");
            RUN_F.setAccessible(true);
            ORUN_F.setAccessible(true);
            WALK_POS_F = ObfuscationReflectionHelper.findField(WalkAnimationState.class, "f_267358_");
            WALK_SPD_F = ObfuscationReflectionHelper.findField(WalkAnimationState.class, "f_267371_");
            WALK_SPDOLD_F = ObfuscationReflectionHelper.findField(WalkAnimationState.class, "f_267406_");
            WALK_POS_F.setAccessible(true);
            WALK_SPD_F.setAccessible(true);
            WALK_SPDOLD_F.setAccessible(true);
        } catch (Exception e)
        {
            throw new RuntimeException("反射实体类失败", e);
        }
    }

    @Override
    public void writeEntityInfo(Entity entity, CompoundTag tag)
    {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        try
        {
            tag.putFloat("RunPos", RUN_F.getFloat(entity));
            tag.putFloat("RunPosO", ORUN_F.getFloat(entity));

            WalkAnimationState was = livingEntity.walkAnimation;
            tag.putFloat("WalkPos", WALK_POS_F.getFloat(was));
            tag.putFloat("WalkSpd", WALK_SPD_F.getFloat(was));
            tag.putFloat("WalkSpdO", WALK_SPDOLD_F.getFloat(was));
        } catch (IllegalAccessException e)
        {
            Sculpture.LOGGER.warn(e.getMessage());
        }
    }

    @Override
    public void readEntityInfo(Entity entity, CompoundTag tag)
    {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        float prevBody = livingEntity.yBodyRot;
        float prevHead = livingEntity.yHeadRot;

        livingEntity.yBodyRot = prevBody;
        livingEntity.yHeadRot = prevHead;

        try
        {
            if (tag.contains("RunPos", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                RUN_F.setFloat(entity, tag.getFloat("RunPos"));
            }
            if (tag.contains("RunPosO", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                ORUN_F.setFloat(entity, tag.getFloat("RunPosO"));
            }

            WalkAnimationState was = livingEntity.walkAnimation;
            if (tag.contains("WalkPos", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                WALK_POS_F.setFloat(was, tag.getFloat("WalkPos"));
            }
            if (tag.contains("WalkSpd", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                WALK_SPD_F.setFloat(was, tag.getFloat("WalkSpd"));
            }
            if (tag.contains("WalkSpdO", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                WALK_SPDOLD_F.setFloat(was, tag.getFloat("WalkSpdO"));
            }
        } catch (IllegalAccessException e)
        {
            Sculpture.LOGGER.warn(e.getMessage());
        }
    }

    @Override
    public boolean isApplicableTo(Entity entity)
    {
        return entity instanceof LivingEntity;
    }
}
