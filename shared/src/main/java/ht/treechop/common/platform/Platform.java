package ht.treechop.common.platform;

import ht.treechop.api.ChopData;
import ht.treechop.api.TreeData;
import ht.treechop.common.network.ClientRequestSettingsPacket;
import ht.treechop.common.network.CustomPacket;
import ht.treechop.common.network.ServerConfirmSettingsPacket;
import ht.treechop.common.settings.EntityChopSettings;
import ht.treechop.common.settings.SettingsField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public interface Platform {

    boolean onStartBlockBreak(Player player, ItemStack tool, BlockPos blockPos);

    Optional<EntityChopSettings> getPlayerChopSettings(Player player);

    TreeData detectTreeEvent(Level level, ServerPlayer agent, BlockPos blockPos, BlockState blockState, boolean overrideLeaves);

    boolean startChopEvent(ServerPlayer agent, ServerLevel level, BlockPos pos, BlockState blockState, ChopData chopData);

    void finishChopEvent(ServerPlayer agent, ServerLevel level, BlockPos pos, BlockState blockState, ChopData chopData);

    Block getChoppedLogBlock();

    BlockEntityType<?> getChoppedLogBlockEntity();

    boolean doItemDamage(ItemStack tool, Level level, BlockState blockState, BlockPos pos, Player agent);

}
