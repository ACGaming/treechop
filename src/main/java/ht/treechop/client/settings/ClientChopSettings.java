package ht.treechop.client.settings;

import ht.treechop.client.Client;
import ht.treechop.common.settings.ChopSettings;
import ht.treechop.common.settings.SettingsField;
import net.minecraft.client.Minecraft;

public class ClientChopSettings extends ChopSettings {

    @Override
    public void set(SettingsField field, Object value) {
        if (Minecraft.getMinecraft().getConnection() == null) {
            super.set(field, value);
        } else if (!get(field).equals(value)) {
            Client.requestSetting(field, value);
        }
    }

    public void accept(SettingsField field, Object value) {
        super.set(field, value);
    }

}
