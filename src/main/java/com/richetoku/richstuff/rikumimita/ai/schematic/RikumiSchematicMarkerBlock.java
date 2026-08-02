package com.richetoku.richstuff.rikumimita.ai.schematic;

import com.mojang.serialization.MapCodec;
import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Reusable MineColonies-style anchor for player and Rikumi schematic placement. */
public final class RikumiSchematicMarkerBlock extends BaseEntityBlock {
    public static final MapCodec<RikumiSchematicMarkerBlock> CODEC = simpleCodec(RikumiSchematicMarkerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public RikumiSchematicMarkerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RikumiSchematicMarkerBlockEntity(pos, state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof RikumiSchematicMarkerBlockEntity marker) {
            marker.setOwner(player.getUUID());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RikumiSchematicMarkerBlockEntity marker)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!held.is(RichStuff.RIKUMI_SCHEMATIC_ITEM.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!mayConfigure(marker, player)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide()) {
            if (marker.hasSchematic()) giveOrDrop(level, pos, player, marker.removeSchematic());
            marker.setSchematic(held);
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 0.65F, 1.15F);
            player.displayClientMessage(Component.translatable("message.richstuff.rikumi_marker.loaded", marker.schematicId()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RikumiSchematicMarkerBlockEntity marker)) return InteractionResult.PASS;
        if (!mayConfigure(marker, player)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (marker.hasSchematic()) giveOrDrop(level, pos, player, marker.removeSchematic());
            marker.setActiveRikumi(null);
            player.displayClientMessage(Component.translatable("message.richstuff.rikumi_marker.unloaded"), true);
            return InteractionResult.SUCCESS;
        }
        if (!marker.hasSchematic()) {
            player.displayClientMessage(Component.translatable("message.richstuff.rikumi_marker.needs_schematic"), true);
            return InteractionResult.CONSUME;
        }
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.CONSUME;
        RikumiMitaEntity rikumi = nearestOwnedRikumi(serverLevel, pos, player.getUUID());
        if (rikumi == null) {
            player.displayClientMessage(Component.translatable("message.richstuff.rikumi_marker.no_rikumi"), true);
            return InteractionResult.CONSUME;
        }
        Direction facing = state.getValue(FACING);
        if (rikumi.startBuildProject(marker.schematicId(), pos, facing)) {
            marker.setActiveRikumi(rikumi.getUUID());
            player.displayClientMessage(Component.translatable("message.richstuff.rikumi_marker.started", marker.schematicId()), true);
            level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 0.7F, 1.1F);
        } else player.displayClientMessage(Component.translatable("message.richstuff.rikumi_marker.unknown", marker.schematicId()), true);
        return InteractionResult.CONSUME;
    }

    private static boolean mayConfigure(RikumiSchematicMarkerBlockEntity marker, Player player) {
        UUID owner = marker.owner();
        return player.isCreative() || owner == null || owner.equals(player.getUUID());
    }

    @Nullable
    private static RikumiMitaEntity nearestOwnedRikumi(ServerLevel level, BlockPos pos, UUID owner) {
        List<RikumiMitaEntity> candidates = level.getEntitiesOfClass(RikumiMitaEntity.class,
                new AABB(pos).inflate(48.0D), entity -> entity.isAlive() && owner.equals(entity.getOwnerUUID()));
        return candidates.stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getCenter()))).orElse(null);
    }

    private static void giveOrDrop(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) Block.popResource(level, pos.above(), stack);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RikumiSchematicMarkerBlockEntity marker) {
            ItemStack stored = marker.removeSchematic();
            if (!stored.isEmpty()) Block.popResource(level, pos, stored);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
