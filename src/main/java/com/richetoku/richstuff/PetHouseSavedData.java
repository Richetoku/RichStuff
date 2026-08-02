package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** World-persistent per-pet homes and delayed resurrection snapshots. */
public final class PetHouseSavedData extends SavedData {
    private static final String DATA_NAME = "richstuff_pet_houses";
    private static final Factory<PetHouseSavedData> FACTORY =
            new Factory<>(PetHouseSavedData::new, PetHouseSavedData::load);

    private final Map<UUID, PetHome> homesByPet = new HashMap<>();
    private final List<PendingRespawn> pendingRespawns = new ArrayList<>();

    public static PetHouseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<PetHome> homeForPet(UUID petUuid) {
        return Optional.ofNullable(homesByPet.get(petUuid));
    }

    /** A pet can be claimed only when it has no home or is already assigned to this exact house. */
    public boolean isAvailableForHouse(UUID petUuid, ResourceKey<Level> dimension, BlockPos pos) {
        PetHome current = homesByPet.get(petUuid);
        return current == null || current.dimension().equals(dimension) && current.pos().equals(pos);
    }

    public void setHome(UUID petUuid, UUID ownerUuid, String petName, ResourceKey<Level> dimension,
                        BlockPos pos, Direction facing) {
        homesByPet.put(petUuid, new PetHome(petUuid, ownerUuid, petName, dimension, pos.immutable(), facing));
        setDirty();
    }

    public void removeHome(UUID petUuid, ResourceKey<Level> dimension, BlockPos pos) {
        PetHome current = homesByPet.get(petUuid);
        if (current != null && current.dimension().equals(dimension) && current.pos().equals(pos)) {
            homesByPet.remove(petUuid);
            setDirty();
        }
    }

    public boolean hasPending(UUID petUuid) {
        return pendingRespawns.stream().anyMatch(pending -> pending.petUuid().equals(petUuid));
    }

    public void queueRespawn(UUID petUuid, UUID ownerUuid, String petName, CompoundTag petData,
                             PetHome home, long dueGameTime) {
        if (hasPending(petUuid)) return;
        pendingRespawns.add(new PendingRespawn(
                petUuid, ownerUuid, petName, petData.copy(), home, dueGameTime, 0));
        setDirty();
    }

    public List<PendingRespawn> dueRespawns(long gameTime) {
        return pendingRespawns.stream().filter(pending -> pending.dueGameTime() <= gameTime).toList();
    }

    public void complete(PendingRespawn pending) {
        if (pendingRespawns.remove(pending)) setDirty();
    }

    public void retry(PendingRespawn pending, long nextGameTime) {
        int index = pendingRespawns.indexOf(pending);
        if (index < 0) return;
        pendingRespawns.set(index, new PendingRespawn(
                pending.petUuid(), pending.ownerUuid(), pending.petName(), pending.petData(),
                pending.home(), nextGameTime, pending.attempts() + 1));
        setDirty();
    }

    public static PetHouseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PetHouseSavedData data = new PetHouseSavedData();
        ListTag homesTag = tag.getList("Homes", Tag.TAG_COMPOUND);
        for (Tag raw : homesTag) {
            if (!(raw instanceof CompoundTag entry)) continue;
            PetHome home = readHome(entry);
            if (home != null) data.homesByPet.put(home.petUuid(), home);
        }

        ListTag pendingTag = tag.getList("PendingRespawns", Tag.TAG_COMPOUND);
        for (Tag raw : pendingTag) {
            if (!(raw instanceof CompoundTag entry)
                    || !entry.hasUUID("Pet") || !entry.hasUUID("Owner")
                    || !entry.contains("Entity", Tag.TAG_COMPOUND)) continue;
            PetHome home = readHome(entry.getCompound("Home"));
            if (home == null) continue;
            data.pendingRespawns.add(new PendingRespawn(
                    entry.getUUID("Pet"), entry.getUUID("Owner"), entry.getString("PetName"),
                    entry.getCompound("Entity").copy(), home, entry.getLong("Due"), entry.getInt("Attempts")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag homesTag = new ListTag();
        homesByPet.values().forEach(home -> homesTag.add(writeHome(home)));
        tag.put("Homes", homesTag);

        ListTag pendingTag = new ListTag();
        for (PendingRespawn pending : pendingRespawns) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Pet", pending.petUuid());
            entry.putUUID("Owner", pending.ownerUuid());
            entry.putString("PetName", pending.petName());
            entry.put("Entity", pending.petData().copy());
            entry.put("Home", writeHome(pending.home()));
            entry.putLong("Due", pending.dueGameTime());
            entry.putInt("Attempts", pending.attempts());
            pendingTag.add(entry);
        }
        tag.put("PendingRespawns", pendingTag);
        return tag;
    }

    private static CompoundTag writeHome(PetHome home) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Pet", home.petUuid());
        tag.putUUID("Owner", home.ownerUuid());
        tag.putString("PetName", home.petName());
        tag.putString("Dimension", home.dimension().location().toString());
        tag.putInt("X", home.pos().getX());
        tag.putInt("Y", home.pos().getY());
        tag.putInt("Z", home.pos().getZ());
        tag.putString("Facing", home.facing().getName());
        return tag;
    }

    private static PetHome readHome(CompoundTag tag) {
        if (!tag.hasUUID("Pet") || !tag.hasUUID("Owner")) return null;
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimensionId == null) return null;
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null || facing.getAxis().isVertical()) facing = Direction.NORTH;
        return new PetHome(tag.getUUID("Pet"), tag.getUUID("Owner"), tag.getString("PetName"), dimension,
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")), facing);
    }

    public record PetHome(UUID petUuid, UUID ownerUuid, String petName, ResourceKey<Level> dimension,
                          BlockPos pos, Direction facing) {}

    public record PendingRespawn(UUID petUuid, UUID ownerUuid, String petName, CompoundTag petData,
                                 PetHome home, long dueGameTime, int attempts) {}
}
