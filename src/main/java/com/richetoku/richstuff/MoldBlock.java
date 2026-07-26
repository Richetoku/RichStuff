package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Placeable mold with collision/selection shapes matching the original model families. */
public final class MoldBlock extends Block {
    public static final MapCodec<MoldBlock> CODEC = simpleCodec(p -> new MoldBlock(p, "generic_mold"));

    private static final VoxelShape STANDARD = Shapes.or(
            box(1, 0, 1, 15, 2, 15),
            box(1, 2, 1, 3, 5, 15), box(13, 2, 1, 15, 5, 15),
            box(3, 2, 1, 13, 5, 3), box(3, 2, 13, 13, 5, 15));
    private static final VoxelShape LOW_NARROW = Shapes.or(
            box(2, 0, 5, 14, 2, 11), box(2, 2, 5, 3, 4, 11),
            box(13, 2, 5, 14, 4, 11), box(3, 2, 5, 13, 4, 6), box(3, 2, 10, 13, 4, 11));
    private static final VoxelShape BLOCK_MOLD = Shapes.or(
            box(1, 0, 1, 15, 2, 15),
            box(1, 2, 1, 3, 8, 15), box(13, 2, 1, 15, 8, 15),
            box(3, 2, 1, 13, 8, 3), box(3, 2, 13, 13, 8, 15));

    // Exact original uncut blank dimensions: width is the first number, length the second.
    private static final VoxelShape BLANK_10X10 = Shapes.or(
            box(3, 0, 3, 13, 10, 13),
            box(2, 9, 4, 3, 10, 5), box(2, 9, 11, 3, 10, 12),
            box(1, 9, 4, 2, 10, 12), box(13, 9, 4, 14, 10, 5),
            box(13, 9, 11, 14, 10, 12), box(14, 9, 4, 15, 10, 12));
    private static final VoxelShape BLANK_10X6 = Shapes.or(
            box(3, 0, 5, 13, 4, 11),
            box(2, 3, 6, 3, 4, 10), box(1, 3, 6, 2, 4, 10),
            box(13, 3, 6, 14, 4, 10), box(14, 3, 6, 15, 4, 10));
    private static final VoxelShape BLANK_6X6 = Shapes.or(
            box(5, 0, 5, 11, 3, 11),
            box(4, 2, 6, 5, 3, 10), box(3, 2, 6, 4, 3, 10),
            box(11, 2, 6, 12, 3, 10), box(12, 2, 6, 13, 3, 10));

    private final String moldId;

    public MoldBlock(Properties properties, String moldId) {
        super(properties);
        this.moldId = moldId;
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    private VoxelShape modelShape() {
        if (moldId.contains("blank_10x10_mold")) return BLANK_10X10;
        if (moldId.contains("blank_10x6_mold")) return BLANK_10X6;
        if (moldId.contains("blank_6x6_mold")) return BLANK_6X6;
        if (moldId.contains("block_mold")) return BLOCK_MOLD;
        if (moldId.contains("rod_mold") || moldId.contains("wire_mold") || moldId.contains("spring_mold")) {
            return LOW_NARROW;
        }
        return STANDARD;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context) {
        return modelShape();
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                      CollisionContext context) {
        return modelShape();
    }
}
