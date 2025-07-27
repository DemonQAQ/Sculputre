package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

import java.lang.reflect.Field;

public class EntityInfoAccessor implements IEntityInfoAccessor
{

    @Override
    public void writeEntityInfo(Entity entity, CompoundTag tag)
    {
        // 利用Minecraft提供的方法保存通用实体数据（不包括ID）
        entity.saveWithoutId(tag);
        // 写入实体类型ID以备反序列化使用
        tag.putString("id", ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());
    }

    @Override
    public void readEntityInfo(Entity entity, CompoundTag tag)
    {
        // 备份位置与朝向，防止 load(tag) 覆盖
        double baseX = entity.getX();
        double baseY = entity.getY();
        double baseZ = entity.getZ();
        float prevYRot = entity.getYRot();
        float prevXRot = entity.getXRot();


        entity.load(tag);

        double capturedYOffset = entity.getY() - Math.floor(entity.getY());
        entity.setPos(baseX, baseY + capturedYOffset, baseZ);
        entity.setYRot(prevYRot);
        entity.setXRot(prevXRot);
    }

    @Override
    public boolean isApplicableTo(Entity entity)
    {
        return true;
    }
}
