package ht.treechop.api;

import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Deprecated
public interface TreeDataImmutable {
    @Deprecated
    Optional<Set<BlockPos>> getLogBlocks();

    @Deprecated
    Set<BlockPos> getLogBlocksOrEmpty();

    Stream<BlockPos> streamLogs();

    Stream<BlockPos> streamLeaves();

    boolean hasLeaves();

    boolean isAProperTree(boolean mustHaveLeaves);

    boolean readyToFell(int numChops);
}
