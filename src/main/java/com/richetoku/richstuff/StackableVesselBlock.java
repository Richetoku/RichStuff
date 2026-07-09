package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StackableVesselBlock extends Block {
    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);
    private final String vesselKind;

    // Full-height jar shapes (16 pixels tall)
    private static final VoxelShape JAR_ONE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape JAR_TWO = Shapes.or(Block.box(2, 0, 4, 8, 16, 12), Block.box(8, 0, 4, 14, 16, 12));
    private static final VoxelShape JAR_THREE = Shapes.or(JAR_TWO, Block.box(4, 0, 2, 12, 16, 10));
    private static final VoxelShape JAR_FOUR = Shapes.or(JAR_TWO, Block.box(2, 0, 2, 8, 16, 10), Block.box(8, 0, 2, 14, 16, 10));

    // Jug shapes (taller, narrower)
    private static final VoxelShape JUG_ONE = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape JUG_TWO = Shapes.or(Block.box(1, 0, 3, 7, 16, 13), Block.box(9, 0, 3, 15, 16, 13));
    private static final VoxelShape JUG_THREE = Shapes.or(JUG_TWO, Block.box(3, 0, 1, 13, 16, 11));
    private static final VoxelShape JUG_FOUR = Shapes.or(JUG_TWO, Block.box(1, 0, 1, 7, 16, 11), Block.box(9, 0, 1, 15, 16, 11));

    public StackableVesselBlock(Properties properties, String vesselKind) {
        super(properties);
        this.vesselKind = vesselKind;
        this.registerDefaultState(this.stateDefinition.any().setValue(COUNT, 1));
    }

    public String vesselKind() { return vesselKind; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COUNT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int count = state.getValue(COUNT);
        if ("jar".equals(vesselKind)) {
            return switch (count) {
                case 1 -> JAR_ONE; case 2 -> JAR_TWO; case 3 -> JAR_THREE; default -> JAR_FOUR;
            };
        } else if ("jug".equals(vesselKind)) {
            return switch (count) {
                case 1 -> JUG_ONE; case 2 -> JUG_TWO; case 3 -> JUG_THREE; default -> JUG_FOUR;
            };
        }
        // Fallback to original shapes
        return switch (count) {
            case 1 -> Block.box(5, 0, 5, 11, 12, 11);
            case 2 -> Shapes.or(Block.box(2, 0, 5, 8, 12, 11), Block.box(8, 0, 5, 14, 12, 11));
            case 3 -> Shapes.or(Block.box(2, 0, 5, 8, 12, 11), Block.box(8, 0, 5, 14, 12, 11), Block.box(5, 0, 1, 11, 12, 7));
            default -> Shapes.or(Block.box(2, 0, 5, 8, 12, 11), Block.box(8, 0, 5, 14, 12, 11), Block.box(2, 0, 1, 8, 12, 7), Block.box(8, 0, 1, 14, 12, 7));
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            removeOne(level, pos, state, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == this && state.getValue(COUNT) < 4) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(COUNT, state.getValue(COUNT) + 1), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            removeOne(level, pos, state, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private void removeOne(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return;
        int count = state.getValue(COUNT);
        ItemStack out = new ItemStack(this.asItem());
        if (!player.addItem(out)) player.drop(out, false);
        if (count <= 1) level.removeBlock(pos, false);
        else level.setBlock(pos, state.setValue(COUNT, count - 1), Block.UPDATE_ALL);
    }
}
