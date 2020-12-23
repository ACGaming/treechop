package ht.treechop.common.network;

import ht.treechop.client.Client;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestChopSettings extends PacketSyncChopSettings {

    public PacketRequestChopSettings() {
        super();
    }

    public static class Handler implements IMessageHandler<PacketRequestChopSettings, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestChopSettings message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() -> {
                Client.updateChopSettings(Client.getChopSettings());
            });
            return null;
        }
    }

}
