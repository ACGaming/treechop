package ht.treechoptests.registry;

import ht.treechoptests.block.SkinnyLog;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class FabricModBlocks {
    public static final Block SKINNY_LOG = new SkinnyLog(
            FabricBlockSettings.of(
                    Material.WOOD,
                    MaterialColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
    );
}
