package com.richetoku.richstuff.rikumimita;

import com.mojang.serialization.MapCodec;
import com.richetoku.richstuff.RichStuff;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A placeable present which opens, releases Rikumi in a glittering twinkle, and disappears. */
public final class RikumiMitaPresentBlock extends BaseEntityBlock {
    public static final MapCodec<RikumiMitaPresentBlock> CODEC = simpleCodec(RikumiMitaPresentBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape CLOSED_SHAPE = Block.box(1, 0, 1, 15, 14, 15);
    private static final VoxelShape OPEN_SHAPE = Block.box(1, 0, 1, 15, 9, 15);

    public RikumiMitaPresentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RikumiMitaPresentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
            context.getLevel().scheduleTick(context.getClickedPos(), this, 1);
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof RikumiMitaPresentBlockEntity present) {
            present.setOwner(player.getUUID());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(OPEN)) return ItemInteractionResult.SUCCESS;
        if (!level.isClientSide()) {
            UUID owner = level.getBlockEntity(pos) instanceof RikumiMitaPresentBlockEntity present
                    ? present.getOwner() : null;
            if (owner != null && !owner.equals(player.getUUID()) && !player.isCreative()) {
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.0F);
                return ItemInteractionResult.FAIL;
            }
            level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.65F, 1.35F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.85F, 1.45F);
            level.scheduleTick(pos, this, 14);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) return;
        UUID owner = level.getBlockEntity(pos) instanceof RikumiMitaPresentBlockEntity present
                ? present.getOwner() : null;
        RikumiMitaEntity rikumi = RichStuff.RIKUMI_MITA_ENTITY.get().create(level);
        if (rikumi != null) {
            rikumi.moveTo(pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            if (owner != null) rikumi.assignOwner(owner);
            level.addFreshEntity(rikumi);

            double sparkleX = rikumi.getX();
            double sparkleY = rikumi.getY() + 1.0D;
            double sparkleZ = rikumi.getZ();
            level.sendParticles(ParticleTypes.END_ROD, sparkleX, sparkleY, sparkleZ,
                    46, 0.55D, 0.80D, 0.55D, 0.08D);
            level.sendParticles(ParticleTypes.ENCHANT, sparkleX, sparkleY, sparkleZ,
                    54, 0.75D, 0.95D, 0.75D, 0.25D);
            level.sendParticles(ParticleTypes.FIREWORK, sparkleX, sparkleY, sparkleZ,
                    28, 0.50D, 0.70D, 0.50D, 0.12D);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, sparkleX, sparkleY, sparkleZ,
                    22, 0.55D, 0.75D, 0.55D, 0.05D);

            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.25F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.85F, 1.70F);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.45F, 1.85F);
            rikumi.greetOwnerFromPresent();
        }

        level.removeBlock(pos, false);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPE : CLOSED_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }
}
