package ht.treechop.config;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigHandler {

    public static Boolean enabled;
    public static Integer maxNumTreeBlocks;
    public static Integer maxNumLeavesBlocks;
    public static Boolean breakLeaves;
    public static ChopCountingAlgorithm chopCountingAlgorithm;
    public static Double chopCountScale;
    public static ResourceLocation blockTagForDetectingLogs;
    public static ResourceLocation blockTagForDetectingLeaves;

    public static void onConfigLoad() {
        ConfigHandler.bakeConfig();
    }

    public static void bakeConfig() {
        enabled = COMMON.breakLeaves.get();
        maxNumTreeBlocks = COMMON.maxNumTreeBlocks.get();
        maxNumLeavesBlocks = COMMON.maxNumLeavesBlocks.get();
        breakLeaves = COMMON.breakLeaves.get();
        chopCountingAlgorithm = COMMON.chopCountingAlgorithm.get();
        chopCountScale = COMMON.chopCountScale.get();
        blockTagForDetectingLogs = new ResourceLocation(COMMON.blockTagForDetectingLogs.get());
        blockTagForDetectingLeaves = new ResourceLocation(COMMON.blockTagForDetectingLeaves.get());
    }

    public static class Common {

        private final ForgeConfigSpec.BooleanValue enabled;
        private final ForgeConfigSpec.IntValue maxNumTreeBlocks;
        private final ForgeConfigSpec.IntValue maxNumLeavesBlocks;
        private final ForgeConfigSpec.BooleanValue breakLeaves;
        private final ForgeConfigSpec.EnumValue<ChopCountingAlgorithm> chopCountingAlgorithm;
        private final ForgeConfigSpec.DoubleValue chopCountScale;
        private final ForgeConfigSpec.ConfigValue<String> blockTagForDetectingLogs;
        private final ForgeConfigSpec.ConfigValue<String> blockTagForDetectingLeaves;

        public Common(ForgeConfigSpec.Builder builder) {
            enabled = builder
                    .comment("Whether this mod is enabled or not")
                    .define("enabled", true);
            maxNumTreeBlocks = builder
                    .comment("Maximum number of log blocks that can be detected to belong to one tree")
                    .defineInRange("maxTreeBlocks", 256, 1, 8096);
            maxNumLeavesBlocks = builder
                    .comment("Maximum number of leaves blocks that can destroyed when a tree is felled")
                    .defineInRange("maxTreeBlocks", 1024, 1, 8096);
            breakLeaves = builder
                    .comment("Whether to destroy leaves when a tree is felled")
                    .define("breakLeaves", true);
            chopCountingAlgorithm = builder
                    .comment("Method to use for computing the number of chops needed to fell a tree")
                    .defineEnum("chopCountingMethod", ChopCountingAlgorithm.LOGARITHMIC);
            chopCountScale = builder
                    .comment("Scales the number of chops (rounding down) required to fell a tree; with chopCountingMethod=LINEAR, this is exactly the number of chops per block")
                    .defineInRange("chopCountScale", 1.0, 0.0, 1024.0);
            blockTagForDetectingLogs = builder
                    .comment("The tag that blocks must have to be considered choppable (default: treechop:choppables)")
                    .define("blockTagForDetectingLogs", "treechop:choppables");
            blockTagForDetectingLeaves = builder
                    .comment("The tag that blocks must have to be considered leaves (default: treechop:leaves_like)")
                    .define("blockTagForDetectingLeaves", "treechop:leaves_like");
        }
    }

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

}
