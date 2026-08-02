package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/** Captures owned animal deaths and recreates them at their owner's active Pet House. */
public final class PetHouseEvents {
    private static final int RESPAWN_DELAY_TICKS = 100;
    private static final int MAX_RESPAWN_ATTEMPTS = 60;
    private static boolean registered;

    private PetHouseEvents() {}

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(PetHouseEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(PetHouseEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(PetHouseEvents::onServerTick);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity pet = event.getEntity();
        UUID ownerUuid = ownerOfPet(pet);
        if (ownerUuid == null || !(pet.level() instanceof ServerLevel serverLevel)) return;

        MinecraftServer server = serverLevel.getServer();
        PetHouseSavedData data = PetHouseSavedData.get(server);
        PetHouseSavedData.PetHome home = data.homeForPet(pet.getUUID()).orElse(null);
        if (!PetHouseBlockEntity.isEligibleSmallPet(pet, ownerUuid) || home == null || !isActiveHouse(server, pet.getUUID(), ownerUuid, home)) return;

        net.minecraft.nbt.CompoundTag snapshot = snapshot(pet);
        if (snapshot == null) return;

        long due = server.overworld().getGameTime() + RESPAWN_DELAY_TICKS;
        data.queueRespawn(pet.getUUID(), ownerUuid, pet.getName().getString(), snapshot, home, due);
        notifyOwner(server, ownerUuid, Component.translatable(
                "message.richstuff.pet_house.respawn_pending", pet.getDisplayName()));
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity pet = event.getEntity();
        if (!(pet.level() instanceof ServerLevel serverLevel)) return;
        if (PetHouseSavedData.get(serverLevel.getServer()).hasPending(pet.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        PetHouseSavedData data = PetHouseSavedData.get(server);
        long gameTime = server.overworld().getGameTime();
        for (PetHouseSavedData.PendingRespawn pending : data.dueRespawns(gameTime)) {
            RespawnResult result = respawn(server, pending);
            if (result == RespawnResult.SUCCESS || result == RespawnResult.PERMANENT_FAILURE) {
                data.complete(pending);
            } else if (pending.attempts() >= MAX_RESPAWN_ATTEMPTS) {
                data.complete(pending);
                notifyOwner(server, pending.ownerUuid(), Component.translatable(
                        "message.richstuff.pet_house.respawn_failed", pending.petName()));
            } else {
                data.retry(pending, gameTime + 20L);
            }
        }
    }

    private static RespawnResult respawn(MinecraftServer server, PetHouseSavedData.PendingRespawn pending) {
        ServerLevel level = server.getLevel(pending.home().dimension());
        if (level == null) return RespawnResult.RETRY;

        net.minecraft.nbt.CompoundTag tag = pending.petData().copy();
        tag.remove("Passengers");
        tag.remove("leash");
        tag.remove("DeathTime");
        tag.remove("HurtTime");
        tag.remove("HurtByTimestamp");

        Entity entity = EntityType.loadEntityRecursive(tag, level, loaded -> loaded);
        if (!(entity instanceof LivingEntity living)) return RespawnResult.PERMANENT_FAILURE;

        BlockPos spawnPos = findSafeSpawn(level, pending.home(), living);
        living.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                pending.home().facing().toYRot(), 0.0F);
        living.setDeltaMovement(Vec3.ZERO);
        living.resetFallDistance();
        living.setHealth(living.getMaxHealth());
        living.setAirSupply(living.getMaxAirSupply());
        living.setOnGround(true);
        if (living instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        if (!level.tryAddFreshEntityWithPassengers(entity)) return RespawnResult.RETRY;

        level.sendParticles(ParticleTypes.HEART,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.6D, entity.getZ(),
                12, 0.35D, 0.35D, 0.35D, 0.05D);
        level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, 1.15F);
        notifyOwner(server, pending.ownerUuid(), Component.translatable(
                "message.richstuff.pet_house.respawned", entity.getDisplayName()));
        return RespawnResult.SUCCESS;
    }

    private static UUID ownerOfPet(LivingEntity entity) {
        if (!(entity instanceof OwnableEntity ownable)) return null;
        return ownable.getOwnerUUID();
    }

    private static boolean isActiveHouse(MinecraftServer server, UUID petUuid, UUID ownerUuid, PetHouseSavedData.PetHome home) {
        ServerLevel level = server.getLevel(home.dimension());
        if (level == null) return false;
        BlockState state = level.getBlockState(home.pos());
        if (!state.is(RichStuff.PET_HOUSE.get())) return false;
        return level.getBlockEntity(home.pos()) instanceof PetHouseBlockEntity house
                && ownerUuid.equals(house.ownerUuid()) && petUuid.equals(house.petUuid());
    }

    private static BlockPos findSafeSpawn(ServerLevel level, PetHouseSavedData.PetHome home, LivingEntity pet) {
        BlockPos front = home.pos().relative(home.facing());
        int[][] offsets = {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}
        };
        for (int rise = 0; rise <= 2; rise++) {
            for (int[] offset : offsets) {
                BlockPos candidate = front.offset(offset[0], rise, offset[1]);
                if (isSafe(level, candidate, pet, home.facing())) return candidate;
            }
        }
        return home.pos().above();
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos, LivingEntity pet, Direction facing) {
        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!floor.isFaceSturdy(level, floorPos, Direction.UP)) return false;

        pet.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.toYRot(), 0.0F);
        return level.noCollision(pet, pet.getBoundingBox());
    }

    private static net.minecraft.nbt.CompoundTag snapshot(LivingEntity pet) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        if (!pet.saveAsPassenger(tag)) return null;
        tag.remove("Passengers");
        tag.remove("leash");
        tag.putFloat("Health", pet.getMaxHealth());
        tag.putShort("DeathTime", (short) 0);
        tag.putShort("HurtTime", (short) 0);
        tag.putInt("HurtByTimestamp", 0);
        return tag;
    }

    private static void notifyOwner(MinecraftServer server, UUID ownerUuid, Component message) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner != null) owner.sendSystemMessage(message);
    }

    private enum RespawnResult { SUCCESS, RETRY, PERMANENT_FAILURE }

}
