// PartEntity 专用（仅记录父关系与相对偏移）
package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.entity.PartEntity;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

import java.util.UUID;

public class PartEntityInfoAccessor implements IEntityInfoAccessor
{
    private static final String KEY_PARENT_UUID = "ParentUUID";
    private static final String KEY_PARENT_TYPE = "ParentType";
    private static final String KEY_REL_X = "RelX";
    private static final String KEY_REL_Y = "RelY";
    private static final String KEY_REL_Z = "RelZ";

    @Override
    public void writeEntityInfo(Entity entity, CompoundTag tag)
    {
        if (!(entity instanceof PartEntity<?> part)) return;

        Entity parent = part.getParent();
        if (parent != null)
        {
            tag.putUUID(KEY_PARENT_UUID, parent.getUUID());
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(parent.getType());
            if (key != null) tag.putString(KEY_PARENT_TYPE, key.toString());
            tag.putDouble(KEY_REL_X, entity.getX() - parent.getX());
            tag.putDouble(KEY_REL_Y, entity.getY() - parent.getY());
            tag.putDouble(KEY_REL_Z, entity.getZ() - parent.getZ());
        }
    }

    @Override
    public void readEntityInfo(Entity entity, CompoundTag tag)
    {
        if (!(entity instanceof PartEntity<?> part)) return;

        Entity parent = part.getParent();
        if (parent == null) return;

        boolean uuidOk = true;
        if (tag.contains(KEY_PARENT_UUID, Tag.TAG_INT_ARRAY))
        {
            UUID expect = tag.getUUID(KEY_PARENT_UUID);
            uuidOk = parent.getUUID().equals(expect);
        }

        if (!uuidOk) return;

        if (tag.contains(KEY_REL_X, Tag.TAG_DOUBLE)
                && tag.contains(KEY_REL_Y, Tag.TAG_DOUBLE)
                && tag.contains(KEY_REL_Z, Tag.TAG_DOUBLE))
        {
            double x = parent.getX() + tag.getDouble(KEY_REL_X);
            double y = parent.getY() + tag.getDouble(KEY_REL_Y);
            double z = parent.getZ() + tag.getDouble(KEY_REL_Z);
            entity.setPos(x, y, z);
        }
    }

    @Override
    public boolean isApplicableTo(Entity entity)
    {
        return entity instanceof PartEntity<?>;
    }
}
