package yiseyo.sculpture.core.controller.capture;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import yiseyo.sculpture.core.controller.capture.accessor.*;

import java.util.List;

public class EntityInfoController
{
    private static final List<IEntityInfoAccessor> ACCESSORS = List.of(
            new EntityInfoAccessor(),
            new PartEntityInfoAccessor(),
            new EnderDragonPartInfoAccessor(),
            new LivingEntityInfoAccessor(),
            new ShulkerInfoAccessor(),
            new SquidInfoAccessor()
    );

    public static CompoundTag serializeEntity(Entity entity)
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
    public static void deserializeEntity(Entity entity, CompoundTag tag)
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
