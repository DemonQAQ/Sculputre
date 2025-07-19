package yiseyo.sculpture.core.controller.capture.accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;
import yiseyo.sculpture.core.controller.capture.IEntityInfoAccessor;

import java.lang.reflect.Field;

import static net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField;

public class SquidInfoAccessor implements IEntityInfoAccessor
{
    private static final Field X_BODY_ROT_F;
    private static final Field PREV_X_BODY_ROT_F;
    private static final Field Z_BODY_ROT_F;
    private static final Field PREV_Z_BODY_ROT_F;
    private static final Field TENTACLE_MOV_F;
    private static final Field PREV_TENTACLE_MOV_F;
    private static final Field TENTACLE_ANGLE_F;
    private static final Field PREV_TENTACLE_ANGLE_F;

    static
    {
        try
        {
            X_BODY_ROT_F = findField(Squid.class, "xBodyRot");
            PREV_X_BODY_ROT_F = findField(Squid.class, "xBodyRotO");
            Z_BODY_ROT_F = findField(Squid.class, "zBodyRot");
            PREV_Z_BODY_ROT_F = findField(Squid.class, "zBodyRotO");
            TENTACLE_MOV_F = findField(Squid.class, "tentacleMovement");
            PREV_TENTACLE_MOV_F = findField(Squid.class, "oldTentacleMovement");
            TENTACLE_ANGLE_F = findField(Squid.class, "tentacleAngle");
            PREV_TENTACLE_ANGLE_F = findField(Squid.class, "oldTentacleAngle");
        } catch (Exception e)
        {
            throw new RuntimeException("反射鱿鱼类失败", e);
        }
    }

    @Override
    public void writeEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        if (!(entity instanceof Squid squid)) return;

        try
        {
            tag.putFloat("BodyRotX", X_BODY_ROT_F.getFloat(squid));
            tag.putFloat("BodyRotZ", Z_BODY_ROT_F.getFloat(squid));
            tag.putFloat("TentacleMove", TENTACLE_MOV_F.getFloat(squid));
            tag.putFloat("TentacleAngle", TENTACLE_ANGLE_F.getFloat(squid));
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("写入鱿鱼特有字段失败", e);
        }
    }

    @Override
    public void readEntityInfo(LivingEntity entity, CompoundTag tag)
    {
        if (!(entity instanceof Squid squid)) return;
        if (!tag.contains("BodyRotX")) return;

        float xRot = tag.getFloat("BodyRotX");
        float zRot = tag.getFloat("BodyRotZ");
        float tentacleMove = tag.getFloat("TentacleMove");
        float tentacleAng = tag.getFloat("TentacleAngle");

        try
        {
            X_BODY_ROT_F.setFloat(squid, xRot);
            PREV_X_BODY_ROT_F.setFloat(squid, xRot);

            Z_BODY_ROT_F.setFloat(squid, zRot);
            PREV_Z_BODY_ROT_F.setFloat(squid, zRot);

            TENTACLE_MOV_F.setFloat(squid, tentacleMove);
            PREV_TENTACLE_MOV_F.setFloat(squid, tentacleMove);

            TENTACLE_ANGLE_F.setFloat(squid, tentacleAng);
            PREV_TENTACLE_ANGLE_F.setFloat(squid, tentacleAng);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("读取鱿鱼特有字段失败", e);
        }
    }

    @Override
    public boolean isApplicableTo(LivingEntity entity)
    {
        return entity instanceof Squid;
    }
}
