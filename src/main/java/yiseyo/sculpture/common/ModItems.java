package yiseyo.sculpture.common;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yiseyo.sculpture.Sculpture;
import yiseyo.sculpture.core.world.StatufierItem;

public final class ModItems
{
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, Sculpture.MODID);

    public static final RegistryObject<Item> STATUFIER = REGISTRY.register(
            "statufier", () -> new StatufierItem(new Item.Properties().stacksTo(1)));
}
