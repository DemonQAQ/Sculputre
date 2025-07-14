package yiseyo.sculpture.core.controller.capture;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public interface IEntityInfoAccessor
{
    /**
     * 将指定实体的相关信息写入到 CompoundTag 中。
     */
    void writeEntityInfo(LivingEntity entity, CompoundTag tag);

    /**
     * 从 CompoundTag 中读取相关信息，并应用到指定实体上。
     */
    void readEntityInfo(LivingEntity entity, CompoundTag tag);

    /**
     * 判断该访问器是否适用于给定实体（用于选择特殊信息处理）。
     */
    boolean isApplicableTo(LivingEntity entity);
}
