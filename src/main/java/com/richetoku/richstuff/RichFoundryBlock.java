package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Foundry controller. The alloy controller differs only by enabling explicit molten-fluid blending recipes. */
public final class RichFoundryBlock extends BaseEntityBlock {
    public static final MapCodec<RichFoundryBlock> FOUNDRY_CODEC = simpleCodec(properties -> new RichFoundryBlock(properties, false));
    public static final MapCodec<RichFoundryBlock> ALLOY_CODEC = simpleCodec(properties -> new RichFoundryBlock(properties, true));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private final boolean alloying;

    public RichFoundryBlock(BlockBehaviour.Properties properties, boolean alloying) {
        super(properties);
        this.alloying = alloying;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }
    public boolean alloying() { return alloying; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return alloying ? ALLOY_CODEC : FOUNDRY_CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING, FORMED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RichFoundryBlockEntity(pos, state); }
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, RichStuff.FOUNDRY_CONTROLLER_ENTITY.get(), RichFoundryBlockEntity::serverTick);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof RichFoundryBlockEntity foundry) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                if (player.isShiftKeyDown()) foundry.cycleSelectedFluid();
                else serverPlayer.openMenu(foundry, buffer -> buffer.writeBlockPos(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RichFoundryBlockEntity foundry)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && foundry.tryInsert(stack)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
            serverPlayer.openMenu(foundry, buffer -> buffer.writeBlockPos(pos));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
