package ht.treechop;

import ht.treechop.common.Common;
import ht.treechop.common.ForgePlatform;
import ht.treechop.common.config.ConfigHandler;
import ht.treechop.common.registry.ForgeModBlocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TreeChop.MOD_ID)
public class TreeChopForge extends TreeChop {
    public TreeChopForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHandler.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigHandler.CLIENT_SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener((FMLCommonSetupEvent e) -> ConfigHandler.onReload());
        modBus.addListener((ModConfigEvent.Reloading e) -> ConfigHandler.onReload());

        modBus.addListener(Common::onCommonSetup);

        ForgeModBlocks.BLOCKS.register(modBus);
        ForgeModBlocks.ENTITIES.register(modBus);

        platform = new ForgePlatform();
    }
}
