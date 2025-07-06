package yiseyo.sculpture.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.core.StatueBlock;
import yiseyo.sculpture.core.StatueBlockEntity;

public final class ModBlocks
{
    public static final DeferredRegister<Block> REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Sculpture.MODID);
    public static final RegistryObject<StatueBlock> STATUE =
            REGISTRY.register("statue", StatueBlock::new);

    public static final DeferredRegister<BlockEntityType<?>> BE_REG =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Sculpture.MODID);
    public static final RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE =
            BE_REG.register("statue",
                    () -> BlockEntityType.Builder.of(StatueBlockEntity::new, STATUE.get()).build(null));
}