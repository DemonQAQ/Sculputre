package yiseyo.sculpture.core.controller.pose.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraftforge.registries.ForgeRegistries;
import yiseyo.sculpture.core.controller.FieldUtil;
import yiseyo.sculpture.core.controller.pose.IEntityInfoAccessor;

public class GeneralEntityInfoAccessor implements IEntityInfoAccessor
{
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
            tag.putFloat("RunPos", FieldUtil.RUN_F.getFloat(entity));
            tag.putFloat("RunPosO", FieldUtil.ORUN_F.getFloat(entity));

            // 提取实体自带的行走动画状态对象中的字段
            WalkAnimationState was = entity.walkAnimation;
            tag.putFloat("WalkPos", FieldUtil.WALK_POS_F.getFloat(was));
            tag.putFloat("WalkSpd", FieldUtil.WALK_SPD_F.getFloat(was));
            tag.putFloat("WalkSpdO", FieldUtil.WALK_SPDOLD_F.getFloat(was));
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to access animation fields", e);
        }
    }

    @Override
    public void readEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        // 备份位置与朝向，防止 load(tag) 覆盖
        double prevX = entity.getX();
        double prevY = entity.getY();
        double prevZ = entity.getZ();
        float prevYRot = entity.getYRot();
        float prevXRot = entity.getXRot();
        float prevBody = entity.yBodyRot;
        float prevHead = entity.yHeadRot;

        entity.load(tag);

        entity.setPos(prevX, prevY, prevZ);
        entity.setYRot(prevYRot);
        entity.setXRot(prevXRot);
        entity.yBodyRot = prevBody;
        entity.yHeadRot = prevHead;

        try
        {
            if (tag.contains("RunPos", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                FieldUtil.RUN_F.setFloat(entity, tag.getFloat("RunPos"));
            }
            if (tag.contains("RunPosO", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                FieldUtil.ORUN_F.setFloat(entity, tag.getFloat("RunPosO"));
            }

            WalkAnimationState was = entity.walkAnimation;
            if (tag.contains("WalkPos", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                FieldUtil.WALK_POS_F.setFloat(was, tag.getFloat("WalkPos"));
            }
            if (tag.contains("WalkSpd", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                FieldUtil.WALK_SPD_F.setFloat(was, tag.getFloat("WalkSpd"));
            }
            if (tag.contains("WalkSpdO", net.minecraft.nbt.Tag.TAG_FLOAT))
            {
                FieldUtil.WALK_SPDOLD_F.setFloat(was, tag.getFloat("WalkSpdO"));
            }
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to restore animation fields", e);
        }
    }

    @Override
    public boolean isApplicableTo(LivingEntity entity)
    {
        return true;
    }
}
