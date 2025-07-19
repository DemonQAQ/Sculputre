package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

import java.lang.reflect.Field;

public class GeneralEntityInfoAccessor implements IEntityInfoAccessor
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
    public void writeEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        // 利用Minecraft提供的方法保存通用实体数据（不包括ID）
        entity.saveWithoutId(tag);
        // 写入实体类型ID以备反序列化使用
        tag.putString("id", ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());

        // 提取通用运动动画状
        try
        {
            tag.putFloat("RunPos", RUN_F.getFloat(entity));
            tag.putFloat("RunPosO", ORUN_F.getFloat(entity));

            // 提取实体自带的行走动画状态对象中的字段
            WalkAnimationState was = entity.walkAnimation;
            tag.putFloat("WalkPos", WALK_POS_F.getFloat(was));
            tag.putFloat("WalkSpd", WALK_SPD_F.getFloat(was));
            tag.putFloat("WalkSpdO", WALK_SPDOLD_F.getFloat(was));
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("写入实体字段失败", e);
        }
    }

    @Override
    public void readEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        // 备份位置与朝向，防止 load(tag) 覆盖
        double baseX = entity.getX();
        double baseY = entity.getY();
        double baseZ = entity.getZ();
        float prevYRot = entity.getYRot();
        float prevXRot = entity.getXRot();
        float prevBody = entity.yBodyRot;
        float prevHead = entity.yHeadRot;

        entity.load(tag);

        double capturedYOffset = entity.getY() - Math.floor(entity.getY());
        entity.setPos(baseX, baseY + capturedYOffset, baseZ);
        entity.setYRot(prevYRot);
        entity.setXRot(prevXRot);
        entity.yBodyRot = prevBody;
        entity.yHeadRot = prevHead;

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

            WalkAnimationState was = entity.walkAnimation;
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
            throw new RuntimeException("读取实体字段失败", e);
        }
    }

    @Override
    public boolean isApplicableTo(LivingEntity entity)
    {
        return true;
    }
}
