package com.richetoku.richstuff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Breeding catalyst for Rich Stuff metal slimes. */
public final class SlimeTreatItem extends Item {
    public SlimeTreatItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof RichStuffMetalSlime slime)) return InteractionResult.PASS;
        if (slime.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(slime.level() instanceof ServerLevel server) || !slime.canAcceptSlimeTreat()) return InteractionResult.CONSUME;

        RichStuffMetalSlime mate = slime.findNearbyReadyMate(6.0D);
        boolean bred = mate != null && slime.tryBreedWith(server, mate);
        if (!bred) slime.markReadyToBreed();
        server.sendParticles(ParticleTypes.HEART, slime.getX(), slime.getY(0.7D), slime.getZ(), 5, 0.35D, 0.2D, 0.35D, 0.02D);
        slime.playSlimeSquishSound();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
