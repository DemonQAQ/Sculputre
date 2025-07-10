package yiseyo.sculpture.core.controller.pose;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import yiseyo.sculpture.core.controller.pose.accessor.GeneralEntityInfoAccessor;
import yiseyo.sculpture.core.controller.pose.accessor.ShulkerInfoAccessor;

import java.util.List;

public class EntityInfoController
{
    private static final List<IEntityInfoAccessor> ACCESSORS = List.of(
            new GeneralEntityInfoAccessor(),
            new ShulkerInfoAccessor()
    );

    public static CompoundTag serializeEntity(LivingEntity entity)
    {
        CompoundTag tag = new CompoundTag();
        for (IEntityInfoAccessor accessor : ACCESSORS)
        {
            if (accessor.isApplicableTo(entity))
            {
                accessor.writeEntityInfo(entity, tag);
            }
        }
        return tag;
    }

    /**
     * 将CompoundTag中的数据读取并应用到给定实体
     */
    public static void deserializeEntity(LivingEntity entity, CompoundTag tag)
    {
        for (IEntityInfoAccessor accessor : ACCESSORS)
        {
            if (accessor.isApplicableTo(entity))
            {
                accessor.readEntityInfo(entity, tag);
            }
        }
    }
}
