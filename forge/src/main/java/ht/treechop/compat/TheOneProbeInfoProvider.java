package ht.treechop.compat;

import ht.treechop.TreeChop;
import ht.treechop.api.TreeData;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.settings.EntityChopSettings;
import ht.treechop.common.util.ChopUtil;
import ht.treechop.common.util.TreeCache;
import mcjty.theoneprobe.api.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TheOneProbeInfoProvider implements IProbeInfoProvider {
    private static final boolean SHOW_TREE_BLOCKS = true;
    private static final boolean SHOW_NUM_CHOPS_REMAINING = true;

    public static Void register(ITheOneProbe probe) {
        probe.registerProvider(new TheOneProbeInfoProvider());
        return null;
    }

    private static final TreeCache treeCache = new TreeCache();

    @Override
    public ResourceLocation getID() {
        return TreeChop.resource("tree_info");
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo builder, Player player, Level level, BlockState blockState, IProbeHitData iProbeHitData) {
        BlockPos blockPos = iProbeHitData.getPos();
        EntityChopSettings chopSettings = ChopUtil.getPlayerChopSettings(player);

        if (ChopUtil.playerWantsToChop(player, chopSettings)
                && ChopUtil.isBlockChoppable(level, blockPos, blockState)
                && SHOW_TREE_BLOCKS || SHOW_NUM_CHOPS_REMAINING) {
            AtomicInteger numChops = new AtomicInteger(0);

            TreeData tree = treeCache.getTree(level, blockPos);
            if (tree.isAProperTree(chopSettings.getTreesMustHaveLeaves())) {
                tree.getLogBlocks().ifPresent(
                        treeBlocks -> {
                            if (SHOW_NUM_CHOPS_REMAINING) {
                                treeBlocks.forEach((BlockPos pos) -> numChops.getAndAdd(ChopUtil.getNumChops(level, pos)));
                                builder.text(Component.translatable("treechop.waila.x_out_of_y_chops", numChops.get(), ChopUtil.numChopsToFell(treeBlocks.size())));
                            }

                            if (SHOW_TREE_BLOCKS) {
                                TreeChop.LOGGER.info(("show tree blocks"));
                                IItemStyle itemStyle = builder.defaultItemStyle();
                                IProbeInfo tiles = builder.horizontal();
                                treeBlocks.stream()
                                        .collect(Collectors.groupingBy((BlockPos pos) -> {
                                            BlockState state = level.getBlockState(pos);
                                            return getLogState(level, pos, state).getBlock();
                                        }, Collectors.counting()))
                                        .forEach((block, count) -> {
                                            ItemStack stack = block.asItem().getDefaultInstance();
                                            stack.setCount(count.intValue());
                                            TreeChop.LOGGER.info(stack.toString());
                                            tiles.item(stack, itemStyle);
                                        });
                            }
                        });
            }
        }
    }

    private static BlockState getLogState(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            return entity.getOriginalState();
        } else {
            return state;
        }
    }
}
