package ht.treechop.common.util;

import ht.treechop.TreeChopMod;
import ht.treechop.client.Client;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.block.IChoppable;
import ht.treechop.common.capabilities.ChopSettings;
import ht.treechop.common.capabilities.ChopSettingsCapability;
import ht.treechop.common.config.ConfigHandler;
import ht.treechop.common.init.ModBlocks;
import ht.treechop.common.properties.BlockStateProperties;
import ht.treechop.common.properties.ChoppedLogShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChopUtil {

    static public boolean isBlockChoppable(IWorld world, BlockPos pos, Block block) {
        return (block instanceof IChoppable) ||
                (isBlockALog(block) && !(isBlockALog(world, pos.west()) && isBlockALog(world, pos.north()) && isBlockALog(world, pos.east()) && isBlockALog(world, pos.south())));
    }

    static public boolean isBlockChoppable(IWorld world, BlockPos pos) {
        return isBlockChoppable(world, pos, world.getBlockState(pos).getBlock());
    }

    static public boolean isBlockALog(Block block) {
        return block.getTags().contains(ConfigHandler.blockTagForDetectingLogs);
    }

    static public boolean isBlockALog(IWorld world, BlockPos pos) {
        return isBlockALog(world.getBlockState(pos).getBlock());
    }

    static public boolean isBlockLeaves(IWorld world, BlockPos pos) {
        return isBlockLeaves(world.getBlockState(pos).getBlock());
    }

    private static boolean isBlockLeaves(Block block) {
        return block.getBlock().getTags().contains(ConfigHandler.blockTagForDetectingLeaves);
    }

    static public Set<BlockPos> getConnectedBlocks(Collection<BlockPos> startingPoints, Function<BlockPos, Stream<BlockPos>> searchOffsetsSupplier, int maxNumBlocks, AtomicInteger iterationCounter) {
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

    static public Set<BlockPos> getConnectedBlocks(Collection<BlockPos> startingPoints, Function<BlockPos, Stream<BlockPos>> searchOffsetsSupplier, int maxNumBlocks) {
        return getConnectedBlocks(startingPoints, searchOffsetsSupplier, maxNumBlocks, new AtomicInteger());
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean canChangeBlock(World world, BlockPos blockPos, PlayerEntity agent) {
        return !agent.blockActionRestricted(world, blockPos, agent.getServer().getGameType());
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean canChangeBlock(World world, BlockPos blockPos, PlayerEntity agent, ItemStack tool) {
        return !(agent.blockActionRestricted(world, blockPos, agent.getServer().getGameType()) || (!tool.isEmpty() && tool.getItem().onBlockStartBreak(tool, blockPos, agent)));
    }

    public static List<BlockPos> getTreeLeaves(World world, Collection<BlockPos> treeBlocks) {
        AtomicInteger iterationCounter = new AtomicInteger();
        Set<BlockPos> leaves = new HashSet<>();

        int maxNumLeavesBlocks = ConfigHandler.COMMON.maxNumLeavesBlocks.get();
        getConnectedBlocks(
                treeBlocks,
                pos1 -> {
                    Block block = world.getBlockState(pos1).getBlock();
                    return ((isBlockLeaves(block) && !(block instanceof LeavesBlock))
                                ? BlockNeighbors.ADJACENTS_AND_BELOW_ADJACENTS // Red mushroom caps can be connected diagonally downward
                                : BlockNeighbors.ADJACENTS)
                        .asStream(pos1)
                        .filter(pos2 -> markLeavesToDestroyAndKeepLooking(world, pos2, iterationCounter, leaves));
                },
                maxNumLeavesBlocks,
                iterationCounter
        );

        if (leaves.size() >= maxNumLeavesBlocks) {
            TreeChopMod.LOGGER.warn(String.format("Max number of leaves reached: %d >= %d blocks", leaves.size(), maxNumLeavesBlocks));
        }

        return new ArrayList<>(leaves);
    }

    public static boolean markLeavesToDestroyAndKeepLooking(IWorld world, BlockPos pos, AtomicInteger iterationCounter, Set<BlockPos> leavesToDestroy) {
        BlockState blockState = world.getBlockState(pos);
        if (isBlockLeaves(blockState.getBlock())) {
            if (blockState.getBlock() instanceof LeavesBlock) {
                if (iterationCounter.get() + 1 > blockState.get(LeavesBlock.DISTANCE)) {
                    return false;
                }
                else if (blockState.get(LeavesBlock.PERSISTENT)) {
                    return true;
                }
            } else if (iterationCounter.get() >= ConfigHandler.maxBreakLeavesDistance) {
                return false;
            }

            leavesToDestroy.add(pos);
            return true;
        }
        return false;
    }

    static public int numChopsToFell(int numBlocks) {
        return (int) (ConfigHandler.COMMON.chopCountingAlgorithm.get().calculate(numBlocks) * ConfigHandler.COMMON.chopCountScale.get());
    }

    public static ChopResult getChopResult(World world, BlockPos blockPos, PlayerEntity agent, int numChops, ItemStack tool, boolean fellIfPossible, Predicate<BlockPos> logCondition) {
        return fellIfPossible
                ? getChopResult(world, blockPos, agent, numChops, logCondition)
                : tryToChopWithoutFelling(world, blockPos, agent, numChops, tool);
    }

    public static ChopResult getChopResult(World world, final BlockPos blockPos, PlayerEntity agent, int numChops, Predicate<BlockPos> logCondition) {
        if (!isBlockChoppable(world, blockPos, world.getBlockState(blockPos).getBlock())) {
            return ChopResult.IGNORED;
        }

        int maxNumTreeBlocks = ConfigHandler.COMMON.maxNumTreeBlocks.get();

        AtomicBoolean hasLeaves = new AtomicBoolean(!getPlayerChopSettings(agent).getOnlyChopTreesWithLeaves());
        Set<BlockPos> supportedBlocks = getConnectedBlocks(
                Collections.singletonList(blockPos),
                somePos -> BlockNeighbors.HORIZONTAL_AND_ABOVE.asStream(somePos)
                        .peek(pos -> hasLeaves.compareAndSet(false, isBlockLeaves(world, pos)))
                        .filter(logCondition),
                maxNumTreeBlocks
        );

        if (!hasLeaves.get()) {
            return ChopResult.IGNORED;
        }

        if (supportedBlocks.size() >= maxNumTreeBlocks) {
            TreeChopMod.LOGGER.warn(String.format("Max tree size reached: %d >= %d blocks (not including leaves)", supportedBlocks.size(), maxNumTreeBlocks));
        }

        return chopTree(world, blockPos, supportedBlocks, numChops);
    }

    public static ChopResult chopTree(World world, BlockPos target, Set<BlockPos> supportedBlocks, int numChops) {
        BlockState blockState = world.getBlockState(target);
        int currentNumChops = getNumChops(blockState);
        int numChopsToFell = numChopsToFell(supportedBlocks.size());

        if (currentNumChops + numChops >= numChopsToFell) {
            return new ChopResult(world, supportedBlocks);
        } else {
            Set<BlockPos> nearbyChoppableBlocks;
            nearbyChoppableBlocks = ChopUtil.getConnectedBlocks(
                    Collections.singletonList(target),
                    pos -> BlockNeighbors.ADJACENTS_AND_DIAGONALS.asStream(pos)
                            .filter(checkPos -> Math.abs(checkPos.getY() - target.getY()) < 4 && isBlockChoppable(world, checkPos)),
                    64
            );

            int totalNumChops = getNumChops(world, nearbyChoppableBlocks) + numChops;

            if (totalNumChops >= numChopsToFell) {
                List<BlockPos> choppedLogsSortedByY = nearbyChoppableBlocks.stream()
                        .filter(pos1 -> world.getBlockState(pos1).getBlock() instanceof IChoppable)
                        .sorted(Comparator.comparingInt(Vector3i::getY))
                        .collect(Collectors.toList());

                // Consume nearby chopped blocks that contributed even if they're at a lower Y, but prefer higher ones
                for (BlockPos pos : choppedLogsSortedByY) {
                    int chops = getNumChops(world, pos);
                    supportedBlocks.add(pos);
                    if (chops > numChopsToFell) {
                        break;
                    }
                }

                return new ChopResult(world, supportedBlocks);
            } else {
                return gatherChops(world, target, numChops, nearbyChoppableBlocks);
            }
        }
    }

    /**
     * Adds chops to the targeted block without destroying it. Overflow chops spill to nearby blocks.
     * @param nearbyChoppableBlocks must not include {@code target}
     */
    public static ChopResult gatherChops(World world, BlockPos target, int numChops, Set<BlockPos> nearbyChoppableBlocks) {
        List<WorldBlock> choppedBlocks = new LinkedList<>();
        int numChopsLeft = gatherChopAndGetNumChopsRemaining(world, target, numChops, choppedBlocks);

        if (numChopsLeft > 0) {
            List<BlockPos> sortedChoppableBlocks = nearbyChoppableBlocks.stream()
                    .filter(blockPos1 -> {
                        BlockState blockState1 = world.getBlockState(blockPos1);
                        Block block1 = blockState1.getBlock();
                        if (block1 instanceof IChoppable) {
                            return getNumChops(blockState1) < getMaxNumChops(blockState1);
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
                            numChopsLeft = gatherChopAndGetNumChopsRemaining(world, nextTarget, numChopsLeft, choppedBlocks);
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

    private static int gatherChopAndGetNumChopsRemaining(World world, BlockPos target, int numChops, List<WorldBlock> choppedBlocks) {
        BlockState blockStateBeforeChopping = world.getBlockState(target);
        BlockState blockStateAfterChopping = getBlockStateAfterChops(world, target, numChops, false);

        if (blockStateBeforeChopping != blockStateAfterChopping) {
            choppedBlocks.add(new WorldBlock(world, target, blockStateAfterChopping));
        }

        return numChops - (getNumChops(blockStateAfterChopping) - getNumChops(blockStateBeforeChopping));
    }

    public static Integer getNumChops(World world, Set<BlockPos> nearbyChoppableBlocks) {
        return nearbyChoppableBlocks.stream()
                .map(world::getBlockState)
                .map(blockState1 -> blockState1.getBlock() instanceof IChoppable
                        ? ((IChoppable) blockState1.getBlock()).getNumChops(blockState1)
                        : 0
                )
                .reduce(Integer::sum)
                .orElse(0);
    }

    private static BlockState getBlockStateAfterChops(World world, BlockPos blockPos, int numChops, boolean destructive) {
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof IChoppable) {
            return getBlockStateAfterChops(blockState, numChops, destructive);
        } else {
            IChoppable choppedBlock = getChoppedBlock(blockState);
            if (choppedBlock instanceof Block) {
                ChoppedLogShape shape = ChoppedLogBlock.getPlacementShape(world, blockPos);
                BlockState defaultChoppedState = ((Block) choppedBlock).getDefaultState().with(BlockStateProperties.CHOPPED_LOG_SHAPE, shape);
                return getBlockStateAfterChops(
                        defaultChoppedState,
                        numChops - getNumChops(defaultChoppedState),
                        destructive
                );
            } else {
                throw new IllegalArgumentException(String.format("Block \"%s\" is not choppable", block.getRegistryName()));
            }
        }
    }

    private static BlockState getBlockStateAfterChops(BlockState blockState, int numChops, boolean destructive) {
        Block block = blockState.getBlock();
        int currentNumChops = getNumChops(blockState);
        int maxNumChops = getMaxNumChops(blockState);
        int newNumChops = currentNumChops + numChops;

        if (newNumChops <= maxNumChops) {
            return ((IChoppable) block).withChops(blockState, newNumChops);
        } else {
            return (destructive)
                    ? Blocks.AIR.getDefaultState()
                    : ((IChoppable) block).withChops(blockState, maxNumChops);
        }
    }

    private static int getMaxNumChops(BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof IChoppable) {
            return ((IChoppable) block).getMaxNumChops();
        } else {
            IChoppable choppedBlock = getChoppedBlock(blockState);
            return (choppedBlock != null) ? choppedBlock.getMaxNumChops() : 0;
        }
    }

    private static IChoppable getChoppedBlock(BlockState blockState) {
        if (isBlockALog(blockState.getBlock())) {
            // TODO: look up appropriate chopped block type
            return (IChoppable) (blockState.getBlock() instanceof IChoppable ? blockState.getBlock() : ModBlocks.CHOPPED_LOG.get());
        } else {
            return null;
        }
    }

    public static int getNumChops(World world, BlockPos pos) {
        return getNumChops(world.getBlockState(pos));
    }

    public static int getNumChops(BlockState blockState) {
        Block block = blockState.getBlock();
        return block instanceof IChoppable ? ((IChoppable) block).getNumChops(blockState) : 0;
    }

    private static ChopResult tryToChopWithoutFelling(World world, BlockPos blockPos, PlayerEntity agent, int numChops, ItemStack tool) {
        return (isBlockChoppable(world, blockPos))
                ? new ChopResult(Collections.singletonList(
                        new WorldBlock(world, blockPos, getBlockStateAfterChops(world, blockPos, numChops, true))
                ), false)
                : ChopResult.IGNORED;
    }

    public static int chopDistance(BlockPos a, BlockPos b) {
        return a.manhattanDistance(b);
    }

    public static boolean canChopWithTool(ItemStack tool) {
        return !(ConfigHandler.choppingToolItemsBlacklist.contains(tool.getItem().getRegistryName()) ||
                tool.getItem().getTags().stream().anyMatch(ConfigHandler.choppingToolTagsBlacklist::contains));
    }

    public static int getNumChopsByTool(ItemStack tool) {
        return 1;
    }

    public static boolean playerWantsToChop(PlayerEntity player) {
        ChopSettings chopSettings = getPlayerChopSettings(player);
        if (ConfigHandler.COMMON.canChooseNotToChop.get()) {
            return chopSettings.getChoppingEnabled() ^ chopSettings.getSneakBehavior().shouldChangeChopBehavior(player);
        } else {
            return true;
        }
    }

    public static boolean playerWantsToFell(PlayerEntity player) {
        ChopSettings chopSettings = getPlayerChopSettings(player);
        return chopSettings.getFellingEnabled() ^ chopSettings.getSneakBehavior().shouldChangeFellBehavior(player);
    }

    private static boolean isLocalPlayer(PlayerEntity player) {
        return !player.isServerWorld() && Minecraft.getInstance().player == player;
    }

    @SuppressWarnings("ConstantConditions")
    private static ChopSettings getPlayerChopSettings(PlayerEntity player) {
        return isLocalPlayer(player) ? Client.getChopSettings() : player.getCapability(ChopSettingsCapability.CAPABILITY).orElseThrow(() -> new IllegalArgumentException("LazyOptional must not be empty"));
    }

    public static void doItemDamage(ItemStack itemStack, World world, BlockState blockState, BlockPos blockPos, PlayerEntity agent) {
        ItemStack mockItemStack = itemStack.copy();
        itemStack.onBlockDestroyed(world, blockState, blockPos, agent);
        if (itemStack.isEmpty() && !mockItemStack.isEmpty()) {
            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(agent, mockItemStack, Hand.MAIN_HAND);
        }
    }

    public static void dropExperience(World world, BlockPos blockPos, int amount) {
        if (world instanceof ServerWorld) {
            Blocks.AIR.dropXpOnBlockBreak((ServerWorld) world, blockPos, amount);
        }
    }

}
