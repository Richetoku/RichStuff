package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Owner-bound home that automatically assigns itself to one nearby small tamed pet. */
public final class PetHouseBlock extends BaseEntityBlock {
    public static final MapCodec<PetHouseBlock> CODEC = simpleCodec(PetHouseBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    public PetHouseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PetHouseBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, RichStuff.PET_HOUSE_ENTITY.get(), PetHouseBlockEntity::serverTick);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING); }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel) || !(placer instanceof Player player)) return;
        if (level.getBlockEntity(pos) instanceof PetHouseBlockEntity house) {
            house.setOwner(player);
            player.displayClientMessage(Component.translatable("message.richstuff.pet_house.awaiting_small_pet"), true);
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PetHouseBlockEntity house)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!house.hasOwner()) player.displayClientMessage(Component.translatable("message.richstuff.pet_house.unowned"), true);
            else if (!house.isOwnedBy(player)) player.displayClientMessage(Component.translatable(
                    "message.richstuff.pet_house.belongs_to", house.ownerName()), true);
            else if (player.isShiftKeyDown() && house.hasPet()) {
                house.clearPetBinding();
                player.displayClientMessage(Component.translatable("message.richstuff.pet_house.cleared"), true);
            } else if (house.hasPet()) player.displayClientMessage(Component.translatable(
                    "message.richstuff.pet_house.status_pet", house.getName()), true);
            else player.displayClientMessage(Component.translatable("message.richstuff.pet_house.awaiting_small_pet"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof PetHouseBlockEntity house && house.petUuid() != null) {
            PetHouseSavedData.get(serverLevel.getServer()).removeHome(house.petUuid(), serverLevel.dimension(), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
