package ht.treechop.common.network;

import ht.treechop.common.capabilities.ChopSettings;
import ht.treechop.common.capabilities.ChopSettingsCapability;
import ht.treechop.common.config.SneakBehavior;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.EnumUtils;

import java.util.function.Supplier;

public class PacketSetSneakBehavior {

    private final SneakBehavior sneakBehavior;

    public PacketSetSneakBehavior(SneakBehavior sneakBehavior) {
        this.sneakBehavior = sneakBehavior;
    }

    public static void encode(PacketSetSneakBehavior message, PacketBuffer buffer) {
        buffer.writeString(message.sneakBehavior.name());
    }

    public static PacketSetSneakBehavior decode(PacketBuffer buffer) {
        final SneakBehavior defaultBehavior = new ChopSettings().getSneakBehavior();
        SneakBehavior sneakBehavior = EnumUtils.getEnum(SneakBehavior.class, buffer.readString(SneakBehavior.maxNameLength));
        return new PacketSetSneakBehavior((sneakBehavior != null) ? sneakBehavior : defaultBehavior);
    }

    @SuppressWarnings("ConstantConditions")
    public static void handle(PacketSetSneakBehavior message, Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection().getReceptionSide().isServer()) {
            context.get().enqueueWork(() -> {
                ServerPlayerEntity player = context.get().getSender();
                ChopSettingsCapability chopSettings = player.getCapability(ChopSettingsCapability.CAPABILITY).orElseThrow(() -> new IllegalArgumentException("Player missing chop settings for " + player.getScoreboardName()));
                chopSettings.setSneakBehavior(message.sneakBehavior);
                player.sendMessage(new StringTextComponent("[TreeChop] ").mergeStyle(TextFormatting.GRAY).append(new StringTextComponent("Sneak behavior " + message.sneakBehavior.getString()).mergeStyle(TextFormatting.WHITE)), Util.DUMMY_UUID);
            });
            context.get().setPacketHandled(true);
        }
    }

}
