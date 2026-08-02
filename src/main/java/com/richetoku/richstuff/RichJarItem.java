package com.richetoku.richstuff;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One physical one-bucket Jar item. Filled fluids use their own registry item, just like buckets. */
public final class RichJarItem extends Item {
    public static final int CAPACITY = 1000;
    @Nullable private final ResourceLocation fluidId;

    public RichJarItem(Properties properties, @Nullable ResourceLocation fluidId) {
        super(properties.stacksTo(16));
        this.fluidId = fluidId;
    }

    public boolean isEmptyJar() { return fluidId == null; }
    @Nullable public ResourceLocation fluidId() { return fluidId; }
    public Fluid fluid() {
        if (fluidId == null) return Fluids.EMPTY;
        Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fluidId);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @Override public Component getName(ItemStack stack) {
        Fluid fluid = fluid();
        return fluid == Fluids.EMPTY ? Component.translatable("item.richstuff.empty_jar")
                : Component.translatable("item.richstuff.jar_of", new FluidStack(fluid, CAPACITY).getHoverName());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Fluid fluid = fluid();
        if (fluid == Fluids.EMPTY) tooltip.add(Component.translatable("tooltip.richstuff.fluid.empty").withStyle(ChatFormatting.GRAY));
        else tooltip.add(Component.translatable("tooltip.richstuff.fluid.amount", new FluidStack(fluid, CAPACITY).getHoverName(), CAPACITY, CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.richstuff.jar.place_four").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level=context.getLevel(); Player player=context.getPlayer(); ItemStack held=context.getItemInHand();
        BlockPos clicked=context.getClickedPos(); FluidState source=level.getFluidState(clicked);
        if (isEmptyJar() && !source.isEmpty() && source.isSource() && level.getBlockState(clicked).getBlock() instanceof LiquidBlock)
            return fillFromWorld(level,player,context.getHand(),held,clicked,source.getType());
        // Placement of any Jar is owned by Rich Farming's four-vessel cluster. Filled Jars pour only
        // through normal bucket-like use, while shift-use is reserved for the four-Jar cluster.
        return InteractionResult.PASS;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand); Fluid fluid=fluid();
        BlockHitResult hit=getPlayerPOVHitResult(level,player,isEmptyJar()? ClipContext.Fluid.SOURCE_ONLY:ClipContext.Fluid.NONE);
        if (hit.getType()!=HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
        BlockPos pos=hit.getBlockPos();
        if (isEmptyJar()) {
            FluidState state=level.getFluidState(pos);
            if (!state.isEmpty() && state.isSource() && level.getBlockState(pos).getBlock() instanceof LiquidBlock) {
                InteractionResult result=fillFromWorld(level,player,hand,held,pos,state.getType());
                return new InteractionResultHolder<>(result,player.getItemInHand(hand));
            }
            return InteractionResultHolder.pass(held);
        }
        BlockPos target=level.getBlockState(pos).canBeReplaced()?pos:pos.relative(hit.getDirection());
        InteractionResult result=emptyIntoWorld(level,player,hand,held,target,fluid);
        return new InteractionResultHolder<>(result,player.getItemInHand(hand));
    }

    private static InteractionResult fillFromWorld(Level level, Player player, InteractionHand hand, ItemStack held, BlockPos sourcePos, Fluid fluid) {
        ItemStack filled=RichStuff.jarForFluid(fluid);
        if (filled.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(sourcePos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL_IMMEDIATE);
        swapOne(player,hand,held,filled);
        level.playSound(null,sourcePos,SoundEvents.BUCKET_FILL,SoundSource.PLAYERS,1.0F,1.0F);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult emptyIntoWorld(Level level, Player player, InteractionHand hand, ItemStack held, BlockPos target, Fluid fluid) {
        if (fluid==Fluids.EMPTY || !fluid.defaultFluidState().isSource()) return InteractionResult.PASS;
        BlockState legacy=fluid.defaultFluidState().createLegacyBlock();
        if (legacy.isAir() || !level.getBlockState(target).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(target,legacy,Block.UPDATE_ALL_IMMEDIATE);
        swapOne(player,hand,held,RichStuff.emptyJar());
        level.playSound(null,target,SoundEvents.BUCKET_EMPTY,SoundSource.PLAYERS,1.0F,1.0F);
        return InteractionResult.SUCCESS;
    }

    private static void swapOne(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player==null || player.getAbilities().instabuild) return;
        if (held.getCount()<=1) player.setItemInHand(hand,result);
        else { held.shrink(1); if (!player.getInventory().add(result)) player.drop(result,false); }
    }
}
