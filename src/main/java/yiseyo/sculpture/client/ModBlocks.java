package yiseyo.sculpture.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    public static final DeferredRegister<Block> REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCKS, StatueMod.MODID);
    public static final RegistryObject<StatueBlock> STATUE =
            REGISTRY.register("statue", StatueBlock::new);

    public static final DeferredRegister<BlockEntityType<?>> BE_REG =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StatueMod.MODID);
    public static final RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE =
            BE_REG.register("statue",
                    () -> BlockEntityType.Builder.of(StatueBlockEntity::new, STATUE.get()).build(null));
}