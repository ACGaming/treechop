package ht.treechop.common.network;

import ht.treechop.TreeChop;
import ht.treechop.client.settings.ClientChopSettings;
import ht.treechop.common.settings.ChopSettings;
import ht.treechop.common.settings.EntityChopSettings;
import ht.treechop.common.settings.Setting;
import ht.treechop.common.settings.SettingsField;
import ht.treechop.server.Server;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClientRequestSettingsPacket implements CustomPacket {
    private static final ResourceLocation id = TreeChop.resource("client_request_settings");
    private final List<Setting> settings;
    private final Event event;

    public ClientRequestSettingsPacket(final List<Setting> settings, Event event) {
        this.settings = settings;
        this.event = event;
    }

    public ClientRequestSettingsPacket(SettingsField field, Object value) {
        this(Collections.singletonList(new Setting(field, value)), Event.REQUEST);
    }

    public ClientRequestSettingsPacket(ClientChopSettings chopSettings) {
        this(chopSettings.getAll(), Event.FIRST_TIME_SYNC);
    }

    public static void encode(ClientRequestSettingsPacket message, FriendlyByteBuf buffer) {
        message.event.encode(buffer);
        buffer.writeInt(message.settings.size());
        message.settings.forEach(setting -> setting.encode(buffer));
    }

    public static ClientRequestSettingsPacket decode(FriendlyByteBuf buffer) {
        Event event = Event.decode(buffer);
        int numSettings = buffer.readInt();
        List<Setting> settings = IntStream.range(0, numSettings)
                .mapToObj($ -> Setting.decode(buffer))
                .collect(Collectors.toList());

        return new ClientRequestSettingsPacket(settings, event);
    }

    public static void handle(ClientRequestSettingsPacket message, ServerPlayer sender) {
        processSettingsRequest(message, sender);
    }

    private static <T> void processSettingsRequest(ClientRequestSettingsPacket message, ServerPlayer player) {
        TreeChop.platform.getPlayerChopSettings(player).ifPresent(chopSettings -> processSettingsRequest(chopSettings, message, player));
    }

    private static void processSettingsRequest(EntityChopSettings chopSettings, ClientRequestSettingsPacket message, ServerPlayer player) {
        List<Setting> settings = (message.event == Event.FIRST_TIME_SYNC && chopSettings.isSynced())
                ? chopSettings.getAll()
                : message.settings;

        List<ConfirmedSetting> confirmedSettings = settings.stream()
                .map(setting -> processSingleSettingRequest(setting, player, chopSettings, message.event))
                .collect(Collectors.toList());;

        TreeChop.platform.sendTo(player, new ServerConfirmSettingsPacket(confirmedSettings));

        if (message.event == Event.FIRST_TIME_SYNC) {
            if (!chopSettings.isSynced()) {
                chopSettings.setSynced();
            }

            TreeChop.platform.sendTo(player, new ServerPermissionsPacket(Server.getPermissions()));
        }
    }

    private static ConfirmedSetting processSingleSettingRequest(Setting setting, ServerPlayer player, ChopSettings chopSettings, Event requestEvent) {
        ConfirmedSetting.Event confirmEvent;
        if (playerHasPermission(player, setting)) {
            chopSettings.set(setting);
            confirmEvent = ConfirmedSetting.Event.ACCEPT;
        } else {
            Setting defaultSetting = getDefaultSetting(player, setting);
            chopSettings.set(defaultSetting);
            confirmEvent = ConfirmedSetting.Event.DENY;
        }

        if (requestEvent == Event.FIRST_TIME_SYNC) {
            confirmEvent = ConfirmedSetting.Event.SILENT;
        }

        SettingsField field = setting.getField();
        return new ConfirmedSetting(new Setting(field, chopSettings.get(field)), confirmEvent);
    }

    private static Setting getDefaultSetting(ServerPlayer player, Setting setting) {
        return Server.getDefaultPlayerSettings().getSetting(setting.getField());
    }

    private static boolean playerHasPermission(Player player, Setting setting) {
        return Server.getPermissions().isPermitted(setting);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    private enum Event {
        FIRST_TIME_SYNC,
        REQUEST
        ;

        private static final Event[] values = Event.values();

        public static Event decode(FriendlyByteBuf buffer) {
            int ordinal = buffer.readByte() % values.length;
            return Event.values[ordinal];
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeByte(ordinal());
        }
    }
}
