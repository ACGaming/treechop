package ht.treechop.client.model;

import ht.treechop.TreeChop;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.treechop.common.registry.ForgeModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ForgeChoppedLogBakedModel extends ChoppedLogBakedModel implements IDynamicBakedModel {
    private static final RandomSource RANDOM = RandomSource.create();
    public static ModelProperty<Map<Direction, BlockState>> NEIGHBORS = new ModelProperty<>();
    public static ModelProperty<BlockState> STRIPPED_BLOCK_STATE = new ModelProperty<>();
    public static ModelProperty<Integer> CHOP_COUNT = new ModelProperty<>();
    public static ModelProperty<ChoppedLogShape> CHOPPED_LOG_SHAPE = new ModelProperty<>();

    public static void overrideBlockStateModels(ModelEvent.BakingCompleted event) {
        for (BlockState blockState : ForgeModBlocks.CHOPPED_LOG.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation variantMRL = BlockModelShaper.stateToModelLocation(blockState);
            BakedModel existingModel = event.getModelManager().getModel(variantMRL);
            if (existingModel == event.getModelManager().getMissingModel()) {
                TreeChop.LOGGER.warn("Did not find the expected vanilla baked model(s) for treechop:chopped_log in registry");
            } else if (existingModel instanceof ForgeChoppedLogBakedModel) {
                TreeChop.LOGGER.warn("Tried to replace ChoppedLogBakedModel twice");
            } else {
                BakedModel customModel = new ForgeChoppedLogBakedModel().bake(event.getModelBakery(), event.getModelBakery().getAtlasSet()::getSprite, null, null);
                event.getModels().put(variantMRL, customModel);
            }
        }
    }

    @Override
    @Nonnull
    public ModelData getModelData(
            @Nonnull BlockAndTintGetter level,
            @Nonnull BlockPos pos,
            @Nonnull BlockState state,
            @Nonnull ModelData tileData
    ) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            ModelData.Builder builder = ModelData.builder();
            builder.with(NEIGHBORS, getNeighborStates(level, pos, entity));
            builder.with(STRIPPED_BLOCK_STATE, ChopUtil.getStrippedState(level, pos, entity.getOriginalState()));
            builder.with(CHOP_COUNT, entity.getChops() + (ChoppedLogBlock.DEFAULT_UNCHOPPED_RADIUS - entity.getUnchoppedRadius()));
            builder.with(CHOPPED_LOG_SHAPE, entity.getShape());
            return builder.build();
        } else {
            return ModelData.EMPTY;
        }
    }

    private Map<Direction, BlockState> getNeighborStates(BlockAndTintGetter level, BlockPos pos, ChoppedLogBlock.MyEntity entity) {
        return entity.getShape().getSolidSides(level, pos).stream().collect(Collectors.toMap(
                side -> side,
                side -> level.getBlockState(pos.relative(side))
        ));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        if (side == null && state != null) {
            BlockState strippedState = (extraData.has(STRIPPED_BLOCK_STATE))
                    ? extraData.get(STRIPPED_BLOCK_STATE)
                    : Blocks.STRIPPED_OAK_LOG.defaultBlockState();

            Map<Direction, BlockState> neighbors = extraData.get(NEIGHBORS);
            if (neighbors == null) {
                neighbors = Collections.emptyMap();
            }

            ChoppedLogShape shape = extraData.get(CHOPPED_LOG_SHAPE);
            if (shape == null) {
                shape = ChoppedLogShape.PILLAR_Y;
            }

            Integer chops = extraData.get(CHOP_COUNT);
            if (chops == null) {
                chops = 1;
            }

            return getQuads(strippedState,
                    shape,
                    chops,
                    neighbors.keySet(),
                    rand,
                    neighbors::get
            ).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

}