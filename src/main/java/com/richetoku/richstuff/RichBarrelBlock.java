package com.richetoku.richstuff;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Single-block double-chest-sized barrel with seven capacity tiers. */
public final class RichBarrelBlock extends BaseEntityBlock {
    private static final MapCodec<RichBarrelBlock>[] CODECS = codecs();
    private final int tier;
    @SuppressWarnings("unchecked") private static MapCodec<RichBarrelBlock>[] codecs() {
        MapCodec<RichBarrelBlock>[] result = new MapCodec[7];
        for (int i=0;i<7;i++){final int tier=i+1;result[i]=simpleCodec(p->new RichBarrelBlock(p,tier));}
        return result;
    }
    public RichBarrelBlock(Properties properties,int tier){super(properties);this.tier=Math.max(1,Math.min(7,tier));}
    public int tier(){return tier;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODECS[tier-1];}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RichBarrelBlockEntity(pos,state);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,@Nullable LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(level.getBlockEntity(pos) instanceof RichBarrelBlockEntity barrel)barrel.loadFromItem(stack);}

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if (!(level.getBlockEntity(pos) instanceof RichBarrelBlockEntity barrel)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                ItemStack picked = new ItemStack(asItem());
                barrel.saveToItem(picked);
                level.removeBlock(pos, false);
                if (!player.getInventory().add(picked)) popResource(level, pos, picked);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if(!level.isClientSide()&&player instanceof ServerPlayer server)server.openMenu(barrel,b->{b.writeBlockPos(pos);b.writeVarInt(barrel.tier());});
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){java.util.ArrayList<ItemStack> drops=new java.util.ArrayList<>();drops.add(new ItemStack(asItem()));BlockEntity entity=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);if(entity instanceof RichBarrelBlockEntity barrel)drops.addAll(barrel.contentsForWorldDrops());return drops;}
}
