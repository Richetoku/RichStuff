package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import com.richetoku.richcore.api.RichFluidItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Seven-tier framed fluid tank usable standalone, as a square multiblock, or in a Foundry shell. */
public final class RichTankBlock extends BaseEntityBlock {
    private static final MapCodec<RichTankBlock> TIER_I_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 1));
    private static final MapCodec<RichTankBlock> TIER_II_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 2));
    private static final MapCodec<RichTankBlock> TIER_III_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 3));
    private static final MapCodec<RichTankBlock> TIER_IV_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 4));
    private static final MapCodec<RichTankBlock> TIER_V_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 5));
    private static final MapCodec<RichTankBlock> TIER_VI_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 6));
    private static final MapCodec<RichTankBlock> TIER_VII_CODEC = simpleCodec(properties -> new RichTankBlock(properties, 7));

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private final int tier;

    public RichTankBlock(Properties properties, int tier) {
        super(properties);
        this.tier = Math.max(1, Math.min(7, tier));
        registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(DOWN, false)
                .setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false).setValue(WEST, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }

    public int tier() { return tier; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return switch (tier) {
            case 1 -> TIER_I_CODEC;
            case 2 -> TIER_II_CODEC;
            case 3 -> TIER_III_CODEC;
            case 4 -> TIER_IV_CODEC;
            case 5 -> TIER_V_CODEC;
            case 6 -> TIER_VI_CODEC;
            default -> TIER_VII_CODEC;
        };
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RichTankBlockEntity(pos, state); }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof RichTankBlockEntity tank) tank.loadFromItem(stack);
        RichTankNetwork.refreshConnections(level, pos, tier);
    }

    /** Applies all six face joins from one already-resolved network view. */
    static void applyConnectionState(Level level, RichTankBlockEntity tank, RichTankNetwork.View view) {
        BlockPos pos = tank.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RichTankBlock)) return;
        BlockState next = state.setValue(UP, view.contains(pos.above()))
                .setValue(DOWN, view.contains(pos.below()))
                .setValue(NORTH, view.contains(pos.north()))
                .setValue(SOUTH, view.contains(pos.south()))
                .setValue(EAST, view.contains(pos.east()))
                .setValue(WEST, view.contains(pos.west()));
        if (!next.equals(state)) level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
                                      BlockState nextState, boolean movedByPiston) {
        boolean removed = state.getBlock() != nextState.getBlock();
        super.onRemove(state, level, pos, nextState, movedByPiston);
        if (removed) RichTankNetwork.refreshConnections(level, pos, tier);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                           Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RichTankBlockEntity tank)) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer server)
                server.openMenu(tank, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!tank.canSafelyRemove()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.richstuff.rich_tank.removal_blocked"), true);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide()) {
            tank.prepareForRemoval();
            ItemStack picked = new ItemStack(asItem());
            tank.saveToItem(picked);
            level.removeBlock(pos, false);
            if (!player.getInventory().add(picked)) popResource(level, pos, picked);
            RichTankNetwork.refreshConnections(level, pos, tier);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RichTankBlockEntity tank))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Shift-right-clicking with an empty matching tier tank atomically adds a complete top layer.
        if (player.isShiftKeyDown() && held.getItem() instanceof RichTankBlockItem tankItem
                && tankItem.tier() == tank.tier()) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            LayerResult result = addCompleteLayer(level, tank, player);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(result.translationKey), true);
            return ItemInteractionResult.SUCCESS;
        }

        // Fluid item capabilities mutate their container. Work on one copy so stacked jars/jugs never
        // share component data across the stack.
        ItemStack single = held.copyWithCount(1);
        IFluidHandlerItem itemHandler = single.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemHandler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        boolean moved = false;
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            int request = Math.min(1000, inItem.getAmount());
            FluidStack offered = inItem.copyWithAmount(request);
            int accepted = tank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                FluidStack drained = itemHandler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                moved = !drained.isEmpty() && tank.fill(drained, IFluidHandler.FluidAction.EXECUTE) > 0;
            }
        } else {
            int request = Math.min(1000, tank.getFluidInTank(0).getAmount());
            FluidStack offered = tank.drain(request, IFluidHandler.FluidAction.SIMULATE);
            int accepted = offered.isEmpty() ? 0 : itemHandler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                FluidStack drained = tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                moved = !drained.isEmpty() && itemHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE) > 0;
            }
        }
        if (!moved) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        ItemStack result = itemHandler.getContainer().copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, result);
            else if (!player.getInventory().add(result)) player.drop(result, false);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private LayerResult addCompleteLayer(Level level, RichTankBlockEntity tank, Player player) {
        if (tank.foundryController() != null) return LayerResult.FOUNDRY_LINKED;
        RichTankNetwork.View network = RichTankNetwork.resolve(tank);
        if (network.height() >= RichTankNetwork.MAX_HEIGHT) return LayerResult.TOO_TALL;

        int targetY = network.max().getY() + 1;
        if (targetY >= level.getMaxBuildHeight()) return LayerResult.TOO_TALL;
        List<BlockPos> targets = new ArrayList<>(network.width() * network.depth());
        for (int x = network.min().getX(); x <= network.max().getX(); x++) {
            for (int z = network.min().getZ(); z <= network.max().getZ(); z++) {
                BlockPos target = new BlockPos(x, targetY, z);
                if (!level.getBlockState(target).canBeReplaced()) return LayerResult.BLOCKED;
                targets.add(target);
            }
        }

        Item matching = asItem();
        int required = targets.size();
        if (!player.getAbilities().instabuild && countEmptyTanks(player.getInventory(), matching) < required)
            return LayerResult.NOT_ENOUGH;

        if (!player.getAbilities().instabuild) consumeEmptyTanks(player.getInventory(), matching, required);
        for (BlockPos target : targets) level.setBlock(target, defaultBlockState(), Block.UPDATE_ALL);
        RichTankNetwork.refreshConnections(level, tank.getBlockPos(), tier);
        return LayerResult.SUCCESS;
    }

    private static int countEmptyTanks(Inventory inventory, Item matching) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(matching) && RichFluidItemHandler.getFluid(stack).isEmpty()) count += stack.getCount();
        }
        return count;
    }

    private static void consumeEmptyTanks(Inventory inventory, Item matching, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(matching) || !RichFluidItemHandler.getFluid(stack).isEmpty()) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        inventory.setChanged();
    }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(asItem());
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof RichTankBlockEntity tank) tank.saveToItem(stack);
        return List.of(stack);
    }

    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                                  boolean willHarvest, FluidState fluid) {
        if (!player.getAbilities().instabuild && level.getBlockEntity(pos) instanceof RichTankBlockEntity tank) {
            if (!tank.canSafelyRemove()) {
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
                    serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "message.richstuff.rich_tank.removal_blocked"), true);
                return false;
            }
            if (!level.isClientSide()) tank.prepareForRemoval();
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    private enum LayerResult {
        SUCCESS("message.richstuff.rich_tank.layer_added"),
        NOT_ENOUGH("message.richstuff.rich_tank.layer_not_enough"),
        BLOCKED("message.richstuff.rich_tank.layer_blocked"),
        TOO_TALL("message.richstuff.rich_tank.layer_too_tall"),
        FOUNDRY_LINKED("message.richstuff.rich_tank.layer_foundry_linked");

        private final String translationKey;
        LayerResult(String translationKey) { this.translationKey = translationKey; }
    }
}
