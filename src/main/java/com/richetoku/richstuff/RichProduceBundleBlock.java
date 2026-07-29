package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Eight independently addressable half-scale produce blocks bound into a 2x2x2 rope bundle. */
public final class RichProduceBundleBlock extends Block {
    public static final IntegerProperty MASK = IntegerProperty.create("mask", 1, 255);
    private static final VoxelShape[] SHAPES = makeShapes();
    private static final TagKey<Item> MELON_BLOCKS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/melon"));
    private static final TagKey<Item> MELONS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "melons"));
    private static final TagKey<Item> PUMPKIN_BLOCKS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/pumpkin"));
    private static final TagKey<Item> PUMPKINS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "pumpkins"));
    private final Supplier<Item> produce;

    public RichProduceBundleBlock(Properties properties, Supplier<Item> produce) {
        super(properties.noOcclusion());
        this.produce = produce;
        registerDefaultState(stateDefinition.any().setValue(MASK, 255));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(MASK); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(MASK)]; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(MASK)]; }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isValidProduce(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int bit = 1 << slotFromHit(hit, pos);
        int mask = state.getValue(MASK);
        if ((mask & bit) != 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(MASK, mask | bit), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Left-click removes only the miniature produce cube under the crosshair. */
    @Override public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            HitResult picked = player.pick(6.0D, 0.0F, false);
            int slot = picked instanceof BlockHitResult hit && hit.getBlockPos().equals(pos) ? slotFromHit(hit, pos) : firstOccupied(state.getValue(MASK));
            int bit = 1 << slot;
            int mask = state.getValue(MASK);
            if ((mask & bit) == 0) slot = firstOccupied(mask);
            bit = 1 << slot;
            popResource(level, pos, new ItemStack(produce.get()));
            int remaining = mask & ~bit;
            if (remaining == 0) {
                popResource(level, pos, ropeStack());
                level.removeBlock(pos, false);
            } else level.setBlock(pos, state.setValue(MASK, remaining), Block.UPDATE_ALL);
        }
        super.attack(state, level, pos, player);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        int count = Integer.bitCount(state.getValue(MASK));
        List<ItemStack> drops = new ArrayList<>(count + 1);
        for (int i = 0; i < count; i++) drops.add(new ItemStack(produce.get()));
        drops.add(ropeStack());
        return drops;
    }

    private boolean isValidProduce(ItemStack stack) {
        if (stack.is(produce.get())) return true;
        boolean melon = produce.get() == Items.MELON;
        return melon ? stack.is(MELON_BLOCKS) || stack.is(MELONS) : stack.is(PUMPKIN_BLOCKS) || stack.is(PUMPKINS);
    }

    private static ItemStack ropeStack() {
        Item rope = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("richfarming", "rope"));
        if (rope == Items.AIR) rope = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("richstuff", "rope"));
        return rope == Items.AIR ? ItemStack.EMPTY : new ItemStack(rope);
    }

    private static int slotFromHit(BlockHitResult hit, BlockPos pos) {
        double x = Math.max(0.0D, Math.min(0.999999D, hit.getLocation().x - pos.getX()));
        double y = Math.max(0.0D, Math.min(0.999999D, hit.getLocation().y - pos.getY()));
        double z = Math.max(0.0D, Math.min(0.999999D, hit.getLocation().z - pos.getZ()));
        return (x >= 0.5D ? 1 : 0) | (z >= 0.5D ? 2 : 0) | (y >= 0.5D ? 4 : 0);
    }
    private static int firstOccupied(int mask) { return Math.max(0, Integer.numberOfTrailingZeros(mask)); }

    private static VoxelShape[] makeShapes() {
        VoxelShape[] result = new VoxelShape[256];
        result[0] = Shapes.empty();
        for (int mask = 1; mask < 256; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int slot = 0; slot < 8; slot++) if ((mask & (1 << slot)) != 0) {
                int x = (slot & 1) == 0 ? 0 : 8;
                int z = (slot & 2) == 0 ? 0 : 8;
                int y = (slot & 4) == 0 ? 0 : 8;
                shape = Shapes.or(shape, Block.box(x + 0.5D, y + 0.5D, z + 0.5D, x + 7.5D, y + 7.5D, z + 7.5D));
            }
            result[mask] = shape.optimize();
        }
        return result;
    }
}
