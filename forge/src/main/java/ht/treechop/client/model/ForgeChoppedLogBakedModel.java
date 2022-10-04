package ht.treechop.client.model;

import ht.treechop.TreeChop;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.treechop.common.registry.ForgeModBlocks;
import ht.treechop.common.util.ChopUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ForgeChoppedLogBakedModel extends ChoppedLogBakedModel implements IDynamicBakedModel {
    public static ModelProperty<Set<Direction>> SOLID_SIDES = new ModelProperty<>();
    public static ModelProperty<BlockState> STRIPPED_BLOCK_STATE = new ModelProperty<>();
    public static ModelProperty<Integer> CHOP_COUNT = new ModelProperty<>();
    public static ModelProperty<ChoppedLogShape> CHOPPED_LOG_SHAPE = new ModelProperty<>();

    public static void overrideBlockStateModels(ModelBakeEvent event) {
        for (BlockState blockState : ForgeModBlocks.CHOPPED_LOG.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation variantMRL = BlockModelShaper.stateToModelLocation(blockState);
            BakedModel existingModel = event.getModelManager().getModel(variantMRL);
            if (existingModel == event.getModelManager().getMissingModel()) {
                TreeChop.LOGGER.warn("Did not find the expected vanilla baked model(s) for treechop:chopped_log in registry");
            } else if (existingModel instanceof ForgeChoppedLogBakedModel) {
                TreeChop.LOGGER.warn("Tried to replace ChoppedLogBakedModel twice");
            } else {
                BakedModel customModel = new ForgeChoppedLogBakedModel().bake(event.getModelLoader(), ForgeModelBakery.defaultTextureGetter(), null, null);
                event.getModelRegistry().put(variantMRL, customModel);
            }
        }
    }

    @Override
    @Nonnull
    public IModelData getModelData(
            @Nonnull BlockAndTintGetter level,
            @Nonnull BlockPos pos,
            @Nonnull BlockState state,
            @Nonnull IModelData tileData
    ) {
        ModelDataMap.Builder builder = new ModelDataMap.Builder();
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            builder.withInitial(SOLID_SIDES, entity.getShape().getSolidSides(level, pos));
            builder.withInitial(STRIPPED_BLOCK_STATE, ChopUtil.getStrippedState(entity.getOriginalState()));
            builder.withInitial(CHOP_COUNT, entity.getChops());
            builder.withInitial(CHOPPED_LOG_SHAPE, entity.getShape());
        }
        return builder.build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random rand, @NotNull IModelData extraData) {
        if (side == null && state != null) {
            BlockState strippedState = (extraData.hasProperty(STRIPPED_BLOCK_STATE))
                    ? extraData.getData(STRIPPED_BLOCK_STATE)
                    : Blocks.STRIPPED_OAK_LOG.defaultBlockState();

            Set<Direction> solidSides = extraData.getData(SOLID_SIDES);
            if (solidSides == null) {
                solidSides = Collections.emptySet();
            }

            ChoppedLogShape shape = extraData.getData(CHOPPED_LOG_SHAPE);
            if (shape == null) {
                shape = ChoppedLogShape.PILLAR_Y;
            }

            Integer chops = extraData.getData(CHOP_COUNT);
            if (chops == null) {
                chops = 1;
            }

            return getQuads(strippedState, shape, chops, solidSides, rand).collect(Collectors.toList());
        }
        else {
            return Collections.emptyList();
        }
    }

}