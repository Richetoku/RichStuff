package com.richetoku.richstuff;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A material-coloured amethyst-style bud or cluster. The vanilla cluster class
 * supplies directional placement, waterlogging, support checks, and a shape
 * that follows the face the crystal grows from.
 */
public final class RichCrystalGrowthBlock extends AmethystClusterBlock {
    private final String material;
    private final int stage;

    public RichCrystalGrowthBlock(float height, float aabbOffset, BlockBehaviour.Properties properties,
                                  String material, int stage) {
        super(height, aabbOffset, properties);
        this.material = material;
        this.stage = stage;
    }

    public String material() { return material; }
    public int stage() { return stage; }
}
