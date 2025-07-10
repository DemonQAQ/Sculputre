package yiseyo.sculpture;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import yiseyo.sculpture.core.manager.CaptureManager;
import yiseyo.sculpture.core.net.ModNet;
import yiseyo.sculpture.render.StatueBER;
import yiseyo.sculpture.common.ModBlocks;
import yiseyo.sculpture.common.ModItems;

@Mod(Sculpture.MODID)
public class Sculpture
{
    public static final String MODID = "sculpture";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public Sculpture()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.REGISTRY.register(modEventBus);
        ModBlocks.BE_REG.register(modEventBus);
        ModItems.REGISTRY.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(CaptureManager.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        ModNet.init();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
        {
            event.accept(ModItems.STATUFIER.get());
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                BlockEntityRenderers.register(ModBlocks.STATUE_BE.get(), StatueBER::new));
    }
}
