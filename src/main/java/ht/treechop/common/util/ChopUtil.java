package ht.treechop.common.util;

import ht.treechop.TreeChopMod;
import ht.treechop.api.ChopEvent;
import ht.treechop.api.IChoppableBlock;
import ht.treechop.api.IChoppingItem;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.capabilities.ChopSettingsCapability;
import ht.treechop.common.config.ConfigHandler;
import ht.treechop.common.init.ModBlocks;
import ht.treechop.common.properties.BlockStateProperties;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.treechop.common.settings.ChopSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChopUtil {

    public static boolean isBlockChoppable(Level level, BlockPos pos, BlockState blockState) {
        return (blockState.getBlock() instanceof IChoppableBlock) ||
                (isBlockALog(blockState) && !(isBlockALog(level, pos.west()) && isBlockALog(level, pos.north()) && isBlockALog(level, pos.east()) && isBlockALog(level, pos.south())));
    }

    public static boolean isBlockChoppable(Level level, BlockPos pos) {
        return isBlockChoppable(level, pos, level.getBlockState(pos));
    }

    public static boolean isBlockALog(BlockState blockState) {
        return ConfigHandler.blockTagForDetectingLogs.contains(blockState.getBlock());
    }

    public static boolean isBlockALog(Level level, BlockPos pos) {
        return isBlockALog(level.getBlockState(pos));
    }

    public static boolean isBlockLeaves(Level level, BlockPos pos) {
        return isBlockLeaves(level.getBlockState(pos));
    }

    public static boolean isBlockLeaves(BlockState blockState) {
        if (ConfigHandler.blockTagForDetectingLeaves.contains(blockState.getBlock())) {
            return !ConfigHandler.ignorePersistentLeaves || !blockState.hasProperty(LeavesBlock.PERSISTENT) || !blockState.getValue(LeavesBlock.PERSISTENT);
        } else {
            return false;
        }
    }

    public static Set<BlockPos> getConnectedBlocks(Collection<BlockPos> startingPoints, Function<BlockPos, Stream<BlockPos>> searchOffsetsSupplier, int maxNumBlocks, AtomicInteger iterationCounter) {
        Set<BlockPos> connectedBlocks = new HashSet<>();
        List<BlockPos> newConnectedBlocks = new LinkedList<>(startingPoints);
        iterationCounter.set(0);
        do {
            connectedBlocks.addAll(newConnectedBlocks);
            if (connectedBlocks.size() >= maxNumBlocks) {
                break;
            }

            newConnectedBlocks = newConnectedBlocks.stream()
                    .flatMap(blockPos -> searchOffsetsSupplier.apply(blockPos)
                            .filter(pos1 -> !connectedBlocks.contains(pos1))
                    )
                    .limit(maxNumBlocks - connectedBlocks.size())
                    .collect(Collectors.toList());

            iterationCounter.incrementAndGet();
        } while (!newConnectedBlocks.isEmpty());

        return connectedBlocks;
    }

    public static Set<BlockPos> getConnectedBlocks(Collection<BlockPos> startingPoints, Function<BlockPos, Stream<BlockPos>> searchOffsetsSupplier, int maxNumBlocks) {
        return getConnectedBlocks(startingPoints, searchOffsetsSupplier, maxNumBlocks, new AtomicInteger());
    }

    public static boolean canChangeBlock(Level level, BlockPos blockPos, Player agent, GameType gameType) {
        return canChangeBlock(level, blockPos, agent, gameType, ItemStack.EMPTY);
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean canChangeBlock(Level level, BlockPos blockPos, Player agent, GameType gameType, ItemStack tool) {
        if (!agent.blockActionRestricted(level, blockPos, gameType)) { // TODO: get the player's game mode
            if (tool.isEmpty()) {
                return true;
            } else {
                return ConfigHandler.shouldOverrideItemBehavior(tool.getItem(), true) || !tool.getItem().onBlockStartBreak(tool, blockPos, agent);
            }
        }
        else {
            return false;
        }
    }

    public static List<BlockPos> getTreeLeaves(Level level, Collection<BlockPos> treeBlocks) {
        AtomicInteger iterationCounter = new AtomicInteger();
        Set<BlockPos> leaves = new HashSet<>();

        int maxNumLeavesBlocks = ConfigHandler.COMMON.maxNumLeavesBlocks.get();
        getConnectedBlocks(
                treeBlocks,
                pos1 -> {
                    BlockState blockState = level.getBlockState(pos1);
                    return ((isBlockLeaves(blockState) && !(blockState.getBlock() instanceof LeavesBlock))
                                ? BlockNeighbors.ADJACENTS_AND_BELOW_ADJACENTS // Red mushroom caps can be connected diagonally downward
                                : BlockNeighbors.ADJACENTS)
                        .asStream(pos1)
                        .filter(pos2 -> markLeavesToDestroyAndKeepLooking(level, pos2, iterationCounter, leaves));
                },
                maxNumLeavesBlocks,
                iterationCounter
        );

        if (leaves.size() >= maxNumLeavesBlocks) {
            TreeChopMod.LOGGER.warn(String.format("Max number of leaves reached: %d >= %d blocks", leaves.size(), maxNumLeavesBlocks));
        }

        return new ArrayList<>(leaves);
    }

    private static boolean markLeavesToDestroyAndKeepLooking(Level level, BlockPos pos, AtomicInteger iterationCounter, Set<BlockPos> leavesToDestroy) {
        BlockState blockState = level.getBlockState(pos);
        if (isBlockLeaves(blockState)) {
            if (blockState.getBlock() instanceof LeavesBlock) {
                if (iterationCounter.get() + 1 > blockState.getValue(LeavesBlock.DISTANCE)) {
                    return false;
                }
            } else if (iterationCounter.get() >= ConfigHandler.maxBreakLeavesDistance) {
                return false;
            }

            leavesToDestroy.add(pos);
            return true;
        }
        return false;
    }

    public static int numChopsToFell(int numBlocks) {
        return ConfigHandler.COMMON.chopCountingAlgorithm.get().calculate(numBlocks);
    }

    public static ChopResult getChopResult(Level level, BlockPos blockPos, Player agent, int numChops, boolean fellIfPossible, Predicate<BlockPos> logCondition) {
        return fellIfPossible
                ? getChopResult(level, blockPos, agent, numChops, logCondition)
                : tryToChopWithoutFelling(level, blockPos, numChops);
    }

    private static ChopResult getChopResult(Level level, BlockPos blockPos, Player agent, int numChops, Predicate<BlockPos> logCondition) {
        Set<BlockPos> supportedBlocks = getTreeBlocks(level, blockPos, logCondition, getPlayerChopSettings(agent).getTreesMustHaveLeaves());
        return chopTree(level, blockPos, supportedBlocks, numChops);
    }

    private static Set<BlockPos> getTreeBlocks(Level level, BlockPos blockPos, Predicate<BlockPos> logCondition, boolean mustHaveLeaves) {
        AtomicBoolean hasLeaves = new AtomicBoolean(!mustHaveLeaves);
        Set<BlockPos> treeBlocks = getTreeBlocks(level, blockPos, logCondition, hasLeaves);
        return hasLeaves.get() ? treeBlocks : Collections.emptySet();
    }

    private static Set<BlockPos> getTreeBlocks(Level level, BlockPos blockPos, Predicate<BlockPos> logCondition, AtomicBoolean inHasLeaves) {
        if (!logCondition.test(blockPos)) {
            return Collections.emptySet();
        }

        AtomicBoolean overrideHasLeaves = new AtomicBoolean(inHasLeaves.get());
        ChopEvent.DetectTreeEvent detectEvent = new ChopEvent.DetectTreeEvent(level, null, blockPos, level.getBlockState(blockPos), inHasLeaves, overrideHasLeaves);
        boolean valueToOverrideHasLeaves = inHasLeaves.get();

        boolean canceled = MinecraftForge.EVENT_BUS.post(detectEvent);
        if (canceled) {
            return Collections.emptySet();
        }

        int maxNumTreeBlocks = ConfigHandler.COMMON.maxNumTreeBlocks.get();

        AtomicBoolean trueHasLeaves = new AtomicBoolean(false);
        Set<BlockPos> supportedBlocks = getConnectedBlocks(
                Collections.singletonList(blockPos),
                somePos -> BlockNeighbors.HORIZONTAL_AND_ABOVE.asStream(somePos)
                        .peek(pos -> trueHasLeaves.compareAndSet(false, isBlockLeaves(level, pos)))
                        .filter(logCondition),
                maxNumTreeBlocks
        );

        if (supportedBlocks.size() >= maxNumTreeBlocks) {
            TreeChopMod.LOGGER.warn(String.format("Max tree size reached: %d >= %d blocks (not including leaves)", supportedBlocks.size(), maxNumTreeBlocks));
        }

        inHasLeaves.set(overrideHasLeaves.get() ? valueToOverrideHasLeaves : trueHasLeaves.get());

        return supportedBlocks;
    }

    private static ChopResult chopTree(Level level, BlockPos target, Set<BlockPos> supportedBlocks, int numChops) {
        if (supportedBlocks.isEmpty()) {
            return ChopResult.IGNORED;
        }

        BlockState blockState = level.getBlockState(target);
        int currentNumChops = getNumChops(blockState);
        int numChopsToFell = numChopsToFell(supportedBlocks.size());

        if (currentNumChops + numChops < numChopsToFell) {
            Set<BlockPos> nearbyChoppableBlocks;
            nearbyChoppableBlocks = ChopUtil.getConnectedBlocks(
                    Collections.singletonList(target),
                    pos -> BlockNeighbors.ADJACENTS_AND_DIAGONALS.asStream(pos)
                            .filter(checkPos -> Math.abs(checkPos.getY() - target.getY()) < 4 && isBlockChoppable(level, checkPos)),
                    64
            );

            int totalNumChops = getNumChops(level, nearbyChoppableBlocks) + numChops;

            if (totalNumChops >= numChopsToFell) {
                List<BlockPos> choppedLogsSortedByY = nearbyChoppableBlocks.stream()
                        .filter(pos1 -> level.getBlockState(pos1).getBlock() instanceof IChoppableBlock)
                        .sorted(Comparator.comparingInt(Vec3i::getY))
                        .collect(Collectors.toList());

                // Consume nearby chopped blocks that contributed even if they're at a lower Y, but prefer higher ones
                for (BlockPos pos : choppedLogsSortedByY) {
                    int chops = getNumChops(level, pos);
                    supportedBlocks.add(pos);
                    if (chops > numChopsToFell) {
                        break;
                    }
                }
            } else {
                nearbyChoppableBlocks.remove(target);
                return gatherChops(level, target, numChops, nearbyChoppableBlocks);
            }
        }

        supportedBlocks.remove(target);
        return new ChopResult(level, Collections.singletonList(target), supportedBlocks);
    }

    /**
     * Adds chops to the targeted block without destroying it. Overflow chops spill to nearby blocks.
     * @param nearbyChoppableBlocks must not include {@code target}
     */
    private static ChopResult gatherChops(Level level, BlockPos target, int numChops, Set<BlockPos> nearbyChoppableBlocks) {
        List<TreeBlock> choppedBlocks = new LinkedList<>();
        int numChopsLeft = gatherChopAndGetNumChopsRemaining(level, target, numChops, choppedBlocks);

        if (numChopsLeft > 0) {
            List<BlockPos> sortedChoppableBlocks = nearbyChoppableBlocks.stream()
                    .filter(blockPos1 -> {
                        BlockState blockState1 = level.getBlockState(blockPos1);
                        Block block1 = blockState1.getBlock();
                        if (block1 instanceof IChoppableBlock) {
                            return getNumChops(blockState1) < getMaxNumChops(level, blockPos1, blockState1);
                        } else {
                            return blockPos1.getY() >= target.getY();
                        }
                    })
                    .sorted(Comparator.comparingInt(a -> chopDistance(target, a)))
                    .collect(Collectors.toList());

            if (sortedChoppableBlocks.size() > 0) {
                int nextChoiceDistance = chopDistance(target, sortedChoppableBlocks.get(0));
                int candidateStartIndex = 0;
                for (int i = 0, n = sortedChoppableBlocks.size(); i <= n; ++i) {
                    if (i == n || chopDistance(target, sortedChoppableBlocks.get(i)) > nextChoiceDistance) {
                        List<BlockPos> candidates = sortedChoppableBlocks.subList(candidateStartIndex, i);
                        Collections.shuffle(candidates);

                        for (BlockPos nextTarget : candidates) {
                            numChopsLeft = gatherChopAndGetNumChopsRemaining(level, nextTarget, numChopsLeft, choppedBlocks);
                            if (numChopsLeft <= 0) {
                                break;
                            }
                        }

                        if (numChopsLeft <= 0) {
                            break;
                        }
                        candidateStartIndex = i;
                    }
                }

            }
        }

        return new ChopResult(choppedBlocks);
    }

    private static int gatherChopAndGetNumChopsRemaining(Level level, BlockPos target, int numChops, List<TreeBlock> choppedBlocks) {
        BlockState blockStateBeforeChopping = level.getBlockState(target);
        BlockState blockStateAfterChopping = getBlockStateAfterChops(level, target, numChops, false);

        if (blockStateBeforeChopping != blockStateAfterChopping) {
            choppedBlocks.add(new TreeBlock(level, target, blockStateAfterChopping, true));
        }

        return numChops - (getNumChops(blockStateAfterChopping) - getNumChops(blockStateBeforeChopping));
    }

    public static BlockState getBlockStateAfterChops(Level level, BlockPos blockPos, int numChops, boolean destructive) {
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof IChoppableBlock) {
            return getBlockStateAfterChops((IChoppableBlock) block, blockState, numChops, destructive);
        } else {
            if (isBlockChoppable(level, blockPos, blockState)) {
                IChoppableBlock choppedBlock = getChoppedBlock(blockState);
                if (choppedBlock instanceof Block) {
                    ChoppedLogShape shape = ChoppedLogBlock.getPlacementShape(level, blockPos);
                    BlockState defaultChoppedState = ((Block) choppedBlock).defaultBlockState().setValue(BlockStateProperties.CHOPPED_LOG_SHAPE, shape);
                    return getBlockStateAfterChops(
                            choppedBlock,
                            defaultChoppedState,
                            numChops - getNumChops(defaultChoppedState),
                            destructive
                    );
                } else {
                    throw new IllegalArgumentException(String.format("Block \"%s\" is not choppable", block.getRegistryName()));
                }
            } else {
                return blockState;
            }
        }
    }

    public static BlockState getBlockStateAfterChops(IChoppableBlock choppableBlock, BlockState blockState, int numChops, boolean destructive) {
        int currentNumChops = getNumChops(blockState);
        int maxNumChops = choppableBlock.getMaxNumChops();
        int newNumChops = currentNumChops + numChops;

        if (newNumChops <= maxNumChops) {
            return choppableBlock.withChops(blockState, newNumChops);
        } else {
            return (destructive)
                    ? Blocks.AIR.defaultBlockState()
                    : choppableBlock.withChops(blockState, maxNumChops);
        }
    }

    public static int getMaxNumChops(Level level, BlockPos blockPos, BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof IChoppableBlock) {
            return ((IChoppableBlock) block).getMaxNumChops();
        } else {
            if (isBlockChoppable(level, blockPos, level.getBlockState(blockPos))) {
                IChoppableBlock choppedBlock = getChoppedBlock(blockState);
                return (choppedBlock != null) ? choppedBlock.getMaxNumChops() : 0;
            } else {
                return 0;
            }
        }
    }

    public static IChoppableBlock getChoppedBlock(BlockState blockState) {
        if (isBlockALog(blockState)) {
            return (IChoppableBlock) (blockState.getBlock() instanceof IChoppableBlock ? blockState.getBlock() : ModBlocks.CHOPPED_LOG.get());
        } else {
            return null;
        }
    }

    public static int getNumChops(Level level, BlockPos pos) {
        return getNumChops(level.getBlockState(pos));
    }

    public static int getNumChops(BlockState blockState) {
        Block block = blockState.getBlock();
        return block instanceof IChoppableBlock ? ((IChoppableBlock) block).getNumChops(blockState) : 0;
    }

    public static int getNumChops(Level level, Set<BlockPos> positions) {
        return positions.stream()
                .map(level::getBlockState)
                .map(blockState1 -> blockState1.getBlock() instanceof IChoppableBlock
                        ? ((IChoppableBlock) blockState1.getBlock()).getNumChops(blockState1)
                        : 0
                )
                .reduce(Integer::sum)
                .orElse(0);
    }

    private static ChopResult tryToChopWithoutFelling(Level level, BlockPos blockPos, int numChops) {
        return (isBlockChoppable(level, blockPos))
                ? new ChopResult(
                        Collections.singletonList(new TreeBlock(
                                level,
                                blockPos,
                                getBlockStateAfterChops(level, blockPos, numChops, true),
                                true
                        )),
                        false
                )
                : ChopResult.IGNORED;
    }

    public static int chopDistance(BlockPos a, BlockPos b) {
        return a.distManhattan(b);
    }

    public static boolean canChopWithTool(ItemStack tool) {
        return ConfigHandler.canChopWithItem(tool.getItem());
    }

    public static int getNumChopsByTool(ItemStack tool, BlockState blockState) {
        Item toolItem = tool.getItem();

        Integer overrideChops = ConfigHandler.getNumChopsOverride(tool.getItem());
        if (overrideChops != null) {
            return overrideChops;
        } else if (toolItem instanceof IChoppingItem) {
            return ((IChoppingItem) toolItem).getNumChops(tool, blockState);
        } else {
            return 1;
        }
    }

    public static boolean playerWantsToChop(Player player) {
        ChopSettings chopSettings = getPlayerChopSettings(player);
        return playerWantsToChop(player, chopSettings);
    }

    public static boolean playerWantsToChop(Player player, ChopSettings chopSettings) {
        if (!player.isCreative() || chopSettings.getChopInCreativeMode()) {
            return chopSettings.getChoppingEnabled() ^ chopSettings.getSneakBehavior().shouldChangeChopBehavior(player);
        } else {
            return false;
        }
    }

    public static boolean playerWantsToFell(Player player) {
        ChopSettings chopSettings = getPlayerChopSettings(player);
        return playerWantsToFell(player, chopSettings);
    }

    public static boolean playerWantsToFell(Player player, ChopSettings chopSettings) {
        return chopSettings.getFellingEnabled() ^ chopSettings.getSneakBehavior().shouldChangeFellBehavior(player);
    }

    public static ChopSettings getPlayerChopSettings(Player player) {
        LazyOptional<ChopSettings> playerSettings = ChopSettingsCapability.forPlayer(player).cast();
        return playerSettings.orElse(ConfigHandler.fakePlayerChopSettings);
    }

    public static void doItemDamage(ItemStack itemStack, Level level, BlockState blockState, BlockPos blockPos, Player agent) {
        ItemStack mockItemStack = itemStack.copy();
        itemStack.mineBlock(level, blockState, blockPos, agent);
        if (itemStack.isEmpty() && !mockItemStack.isEmpty()) {
            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(agent, mockItemStack, InteractionHand.MAIN_HAND);
        }
    }

    public static void dropExperience(Level level, BlockPos blockPos, int amount) {
        if (level instanceof ServerLevel) {
            Blocks.AIR.popExperience((ServerLevel) level, blockPos, amount);
        }
    }

    public static boolean isPartOfATree(Level level, BlockPos pos, boolean mustHaveLeaves) {
        AtomicBoolean hasLeaves = new AtomicBoolean(false);
        Set<BlockPos> treeBlocks = getTreeBlocks(level, pos, blockPos -> isBlockALog(level, blockPos), hasLeaves);

        if (treeBlocks.isEmpty()) {
            return false;
        } else {
            if (mustHaveLeaves) {
                return hasLeaves.get();
            } else {
                return treeBlocks.size() >= (hasLeaves.get() ? 1 : 2);
            }
        }
    }
}
