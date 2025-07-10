package yiseyo.sculpture.core.controller.pose.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import yiseyo.sculpture.core.controller.pose.IEntityInfoAccessor;

public class ShulkerInfoAccessor implements IEntityInfoAccessor
{
    @Override
    public void writeEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        if (!(entity instanceof Shulker)) return;  // 确认实体类型
        Shulker shulker = (Shulker) entity;
        // 获取潜影贝的张开程度 (0.0 ~ 1.0 或对应的刻度值)
        float peek = shulker.getClientPeekAmount();  // 假设存在此方法获取开壳进度
        tag.putFloat("ShulkerPeek", peek);
        // 如有其它特殊状态也在此处写入，如附着面的朝向等
    }

    @Override
    public void readEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        if (!(entity instanceof Shulker)) return;
        Shulker shulker = (Shulker) entity;
        if (tag.contains("ShulkerPeek"))
        {
            float peek = tag.getFloat("ShulkerPeek");
            // 将NBT中的开壳进度应用回潜影贝实体
            shulker.setPeekAmount(peek);  // 假设存在设置方法或通过数据管理器设置
        }
        // 恢复其他潜影贝特殊状态
    }

    @Override
    public boolean isApplicableTo(LivingEntity entity)
    {
        return entity instanceof Shulker;
    }
}
