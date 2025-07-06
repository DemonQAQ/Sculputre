package yiseyo.sculpture.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

public abstract class FieldUtil
{
    public static final Field RUN_F, ORUN_F;
    public static final Field WALK_POS_F, WALK_SPD_F, WALK_SPDOLD_F;
    static {
        try {
            RUN_F  = LivingEntity.class.getDeclaredField("run");
            ORUN_F = LivingEntity.class.getDeclaredField("oRun");
            RUN_F.setAccessible(true);
            ORUN_F.setAccessible(true);

            WALK_POS_F    = ObfuscationReflectionHelper.findField(WalkAnimationState.class, "f_267358_");
            WALK_SPD_F    = ObfuscationReflectionHelper.findField(WalkAnimationState.class, "f_267371_");
            WALK_SPDOLD_F = ObfuscationReflectionHelper.findField(WalkAnimationState.class, "f_267406_");
            WALK_POS_F.setAccessible(true);
            WALK_SPD_F.setAccessible(true);
            WALK_SPDOLD_F.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve animation fields", e);
        }
    }
}
