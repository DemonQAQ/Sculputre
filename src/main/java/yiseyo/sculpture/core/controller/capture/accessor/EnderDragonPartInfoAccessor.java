package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

public class EnderDragonPartInfoAccessor extends PartEntityInfoAccessor implements IEntityInfoAccessor
{
    private static final String KEY_DRAGON_PART_NAME = "DragonPartName";

    @Override
    public void writeEntityInfo(Entity entity, CompoundTag tag)
    {
        if (!(entity instanceof EnderDragonPart part)) return;
        super.writeEntityInfo(entity, tag);
        tag.putString(KEY_DRAGON_PART_NAME, part.name);
    }

    @Override
    public void readEntityInfo(Entity entity, CompoundTag tag)
    {
        if (!(entity instanceof EnderDragonPart part)) return;
        super.readEntityInfo(entity, tag);

        if (tag.contains(KEY_DRAGON_PART_NAME, Tag.TAG_STRING))
        {
            // 可在需要时做一致性检查或日志输出。
        }
    }

    @Override
    public boolean isApplicableTo(Entity entity)
    {
        return entity instanceof EnderDragonPart;
    }
}
