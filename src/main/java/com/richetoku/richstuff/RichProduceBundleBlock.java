package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Eight-piece produce bundle. Each state piece is one full melon or pumpkin represented as a
 * half-scale 1x1x1 cube in the visible 2x2x2 arrangement. Starting to break the block removes one
 * piece and returns one full produce block; breaking the remainder drops every piece still stored.
 */
public final class RichProduceBundleBlock extends Block {
    public static final IntegerProperty PIECES = IntegerProperty.create("pieces", 1, 8);
    private static final VoxelShape[] SHAPES = makeShapes();
    private final Supplier<Item> produce;

    public RichProduceBundleBlock(Properties properties, Supplier<Item> produce) {
        super(properties.noOcclusion());
        this.produce = produce;
        registerDefaultState(stateDefinition.any().setValue(PIECES, 8));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIECES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PIECES)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PIECES)];
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            int pieces = state.getValue(PIECES);
            popResource(level, pos, new ItemStack(produce.get()));
            if (pieces <= 1) level.removeBlock(pos, false);
            else level.setBlock(pos, state.setValue(PIECES, pieces - 1), Block.UPDATE_ALL);
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        int pieces = state.getValue(PIECES);
        List<ItemStack> drops = new ArrayList<>(pieces);
        for (int i = 0; i < pieces; i++) drops.add(new ItemStack(produce.get()));
        return drops;
    }

    private static VoxelShape[] makeShapes() {
        VoxelShape[] result = new VoxelShape[9];
        result[0] = Shapes.empty();
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < 8; i++) {
            int x = i & 1;
            int z = (i >> 1) & 1;
            int y = (i >> 2) & 1;
            shape = Shapes.or(shape, Block.box(x * 8.0D, y * 8.0D, z * 8.0D,
                    x * 8.0D + 8.0D, y * 8.0D + 8.0D, z * 8.0D + 8.0D));
            result[i + 1] = shape;
        }
        return result;
    }
}
