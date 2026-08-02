package com.richetoku.richstuff.rikumimita.ai.schematic;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable, registry-friendly block-by-block construction plan. */
public record RikumiSchematic(
        ResourceLocation id,
        String displayName,
        String sourceFormat,
        List<Placement> placements) {

    public RikumiSchematic {
        placements = List.copyOf(placements);
    }

    public record Placement(BlockPos offset, BlockState state, boolean consumesItem) {}
}
