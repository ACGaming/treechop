package ht.treechop.client;

import ht.treechop.TreeChopMod;
import ht.treechop.common.capabilities.ChopSettings;
import ht.treechop.common.config.ConfigHandler;
import ht.treechop.common.network.PacketEnableChopping;
import ht.treechop.common.network.PacketEnableFelling;
import ht.treechop.common.network.PacketHandler;
import ht.treechop.common.network.PacketSetSneakBehavior;
import ht.treechop.common.network.PacketSyncChopSettings;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class Client {

    private static final ChopSettings chopSettings = new ChopSettings();

    public static void onClientSetup(FMLClientSetupEvent event) {
        IEventBus eventBus = MinecraftForge.EVENT_BUS;
        eventBus.addListener(Client::onConnect);
        eventBus.addListener(KeyBindings::buttonPressed);
        KeyBindings.clientSetup(event);
    }

    // TODO: is this working?
    public static void onConnect(ClientPlayerNetworkEvent.LoggedInEvent event) {
        chopSettings.copyFrom(ConfigHandler.CLIENT.getChopSettings());
        TreeChopMod.LOGGER.info("Sending chop settings sync request");
        PacketHandler.sendToServer(new PacketSyncChopSettings(chopSettings));
    }

    public static void toggleChopping() {
        chopSettings.toggleChopping();
        PacketHandler.sendToServer(new PacketEnableChopping(chopSettings.getChoppingEnabled()));
    }

    public static void toggleFelling() {
        chopSettings.toggleFelling();
        PacketHandler.sendToServer(new PacketEnableFelling(chopSettings.getFellingEnabled()));
    }

    public static void cycleSneakBehavior() {
        chopSettings.cycleSneakBehavior();
        PacketHandler.sendToServer(new PacketSetSneakBehavior(chopSettings.getSneakBehavior()));
    }

    public static ChopSettings getChopSettings() {
        return chopSettings;
    }

}
