package ht.treechop.server;

import ht.treechop.TreeChopMod;
import ht.treechop.common.Common;
import ht.treechop.common.capabilities.ChopSettingsCapability;
import ht.treechop.common.settings.ChopSettings;
import ht.treechop.common.settings.Permissions;
import ht.treechop.common.settings.Setting;
import ht.treechop.common.settings.SettingsField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.Optional;

@EventBusSubscriber(modid = TreeChopMod.MOD_ID)
public class Server extends Common {

    private static final Permissions permissions = new Permissions();
    private static final ChopSettings defaultPlayerSettings = new ChopSettings();

    @SubscribeEvent
    public void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            EntityPlayer oldPlayer = event.getOriginal();
            EntityPlayer newPlayer = event.getEntityPlayer();
            Optional<ChopSettingsCapability> lazyOldSettings = ChopSettingsCapability.forPlayer(oldPlayer);
            Optional<ChopSettingsCapability> lazyNewSettings = ChopSettingsCapability.forPlayer(newPlayer);

            lazyOldSettings.ifPresent(
                    oldSettings -> lazyNewSettings.ifPresent(
                            newSettings -> newSettings.copyFrom(oldSettings)
                    )
            );
        }
    }

    public static Permissions getPermissions() {
        return permissions;
    }

    public static ChopSettings getDefaultPlayerSettings() {
        return defaultPlayerSettings;
    }

    public static void updatePermissions(Permissions permissions) {
        Server.permissions.copy(permissions);
        updateDefaultPlayerSettings();
    }

    private static void updateDefaultPlayerSettings() {
        Arrays.stream(SettingsField.values())
                .map(Server::getDefaultPlayerSetting)
                .forEach(defaultPlayerSettings::set);
    }

    private static Setting getDefaultPlayerSetting(SettingsField field) {
        Setting defaultSettingIgnoringPermissions = new Setting(field, field.getDefaultValue());
        if (permissions.isPermitted(defaultSettingIgnoringPermissions)) {
            return defaultSettingIgnoringPermissions;
        } else {
            Object bestValue = field.getValues().stream()
                    .filter(value -> permissions.isPermitted(new Setting(field, value)))
                    .findFirst()
                    .orElse(defaultSettingIgnoringPermissions);
            return new Setting(field, bestValue);
        }
    }

}
