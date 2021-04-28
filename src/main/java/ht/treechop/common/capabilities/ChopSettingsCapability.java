package ht.treechop.common.capabilities;

import ht.treechop.TreeChopMod;
import ht.treechop.common.settings.ChopSettings;
import ht.treechop.common.settings.SneakBehavior;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;
import java.util.Optional;

public class ChopSettingsCapability extends ChopSettings {
    @CapabilityInject(ChopSettingsCapability.class)
    public static final Capability<ChopSettingsCapability> CAPABILITY = null;

    private boolean isSynced = false;

    public ChopSettingsCapability() {
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced() {
        this.isSynced = true;
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(
                ChopSettingsCapability.class,
                new ChopSettingsCapability.Storage(),
                ChopSettingsCapability::new
        );
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<ChopSettingsCapability> forPlayer(EntityPlayer player) {
        Optional<ChopSettingsCapability> lazyCapability = Optional.of(player.getCapability(CAPABILITY, null));
        if (!lazyCapability.isPresent()) {
            TreeChopMod.LOGGER.warn("Player " + player + " is missing chop settings");
        }

        return Optional.of(player.getCapability(CAPABILITY, null));
    }

    public static class Storage implements Capability.IStorage<ChopSettingsCapability> {

        private static final String CHOPPING_ENABLED_KEY = "choppingEnabled";
        private static final String FELLING_ENABLED_KEY = "fellingEnabled";
        private static final String SNEAK_BEHAVIOR_KEY = "sneakBehavior";
        private static final String TREES_MUST_HAVE_LEAVES_KEY = "treesMustHaveLeaves";
        private static final String CHOP_IN_CREATIVE_MODE_KEY = "chopInCreativeMode";
        private static final String IS_SYNCED_KEY = "isSynced";

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<ChopSettingsCapability> capability, ChopSettingsCapability instance, EnumFacing side) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean(CHOPPING_ENABLED_KEY, instance.getChoppingEnabled());
            nbt.setBoolean(FELLING_ENABLED_KEY, instance.getFellingEnabled());
            nbt.setString(SNEAK_BEHAVIOR_KEY, instance.getSneakBehavior().name());
            nbt.setBoolean(TREES_MUST_HAVE_LEAVES_KEY, instance.getTreesMustHaveLeaves());
            nbt.setBoolean(CHOP_IN_CREATIVE_MODE_KEY, instance.getChopInCreativeMode());
            nbt.setBoolean(IS_SYNCED_KEY, instance.isSynced());
            return nbt;
        }

        @Override
        public void readNBT(Capability<ChopSettingsCapability> capability, ChopSettingsCapability instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) {
                NBTTagCompound compoundNbt = (NBTTagCompound) nbt;
                Optional<Boolean> choppingEnabled = getBoolean(compoundNbt, CHOPPING_ENABLED_KEY);
                Optional<Boolean> fellingEnabled = getBoolean(compoundNbt, FELLING_ENABLED_KEY);
                SneakBehavior sneakBehavior;
                try {
                    sneakBehavior = SneakBehavior.valueOf(compoundNbt.getString(SNEAK_BEHAVIOR_KEY));
                } catch (IllegalArgumentException e) {
                    TreeChopMod.LOGGER.warn(String.format("NBT contains bad sneak behavior value \"%s\"; using default value instead", compoundNbt.getString(SNEAK_BEHAVIOR_KEY)));
                    sneakBehavior = SneakBehavior.INVERT_CHOPPING;
                }
                Optional<Boolean> onlyChopTreesWithLeaves = getBoolean(compoundNbt, TREES_MUST_HAVE_LEAVES_KEY);
                Optional<Boolean> chopInCreativeMode = getBoolean(compoundNbt, CHOP_IN_CREATIVE_MODE_KEY);
                Optional<Boolean> isSynced = getBoolean(compoundNbt, IS_SYNCED_KEY);

                instance.setChoppingEnabled(choppingEnabled.orElse(instance.getChoppingEnabled()));
                instance.setFellingEnabled(fellingEnabled.orElse(instance.getFellingEnabled()));
                instance.setSneakBehavior(sneakBehavior);
                instance.setTreesMustHaveLeaves(onlyChopTreesWithLeaves.orElse(instance.getTreesMustHaveLeaves()));
                instance.setChopInCreativeMode(chopInCreativeMode.orElse(instance.getChopInCreativeMode()));

                if (isSynced.orElse(false)) {
                    instance.setSynced();
                }
            } else {
                TreeChopMod.LOGGER.warn("Failed to read ChopSettingsCapability NBT");
            }
        }

        private Optional<Boolean> getBoolean(NBTTagCompound compoundNbt, String key) {
            return (compoundNbt.hasKey(key))
                    ? Optional.of(compoundNbt.getBoolean(key))
                    : Optional.empty();
        }
    }
}
