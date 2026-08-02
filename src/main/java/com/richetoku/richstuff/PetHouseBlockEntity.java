package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/** One owner-bound house assigned to exactly one nearby small tamed pet. */
public final class PetHouseBlockEntity extends BlockEntity implements Nameable {
    public static final float MAX_PET_WIDTH = 1.05F;
    public static final float MAX_PET_HEIGHT = 1.50F;
    private static final int SEARCH_INTERVAL = 20;
    private static final double SEARCH_RADIUS = 4.5D;

    private @Nullable UUID ownerUuid;
    private String ownerName = "";
    private @Nullable UUID petUuid;
    private String petName = "";
    private int searchCooldown;

    public PetHouseBlockEntity(BlockPos pos, BlockState state) {
        super(RichStuff.PET_HOUSE_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PetHouseBlockEntity house) {
        if (!(level instanceof ServerLevel serverLevel) || house.ownerUuid == null) return;
        if (house.searchCooldown-- > 0) return;
        house.searchCooldown = SEARCH_INTERVAL;

        PetHouseSavedData homes = PetHouseSavedData.get(serverLevel.getServer());
        if (house.petUuid != null) {
            if (!homes.isAvailableForHouse(house.petUuid, serverLevel.dimension(), pos)) {
                // Another house owns this pet now; release the stale local binding and find another pet.
                house.petUuid = null;
                house.petName = "";
                house.sync();
            } else {
                var existing = serverLevel.getEntity(house.petUuid);
                if (existing instanceof LivingEntity pet && isEligibleSmallPet(pet, house.ownerUuid)) {
                    String currentName = pet.getName().getString();
                    homes.setHome(house.petUuid, house.ownerUuid, currentName, serverLevel.dimension(), pos,
                            state.getValue(PetHouseBlock.FACING));
                    if (!currentName.equals(house.petName)) house.updatePetName(currentName);
                }
                return;
            }
        }

        AABB area = new AABB(pos).inflate(SEARCH_RADIUS, 2.5D, SEARCH_RADIUS);
        LivingEntity pet = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                        candidate -> isEligibleSmallPet(candidate, house.ownerUuid)
                                && homes.isAvailableForHouse(candidate.getUUID(), serverLevel.dimension(), pos))
                .stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(pos.getCenter())))
                .orElse(null);
        if (pet != null) house.bindPet(serverLevel, pet);
    }

    public static boolean isEligibleSmallPet(LivingEntity entity, UUID expectedOwner) {
        if (!entity.isAlive() || entity instanceof Player || !(entity instanceof OwnableEntity ownable)) return false;
        UUID actualOwner = ownable.getOwnerUUID();
        return actualOwner != null && actualOwner.equals(expectedOwner)
                && entity.getBbWidth() <= MAX_PET_WIDTH && entity.getBbHeight() <= MAX_PET_HEIGHT;
    }

    public void setOwner(Player player) {
        ownerUuid = player.getUUID();
        ownerName = player.getGameProfile().getName();
        sync();
    }

    private void bindPet(ServerLevel level, LivingEntity pet) {
        petUuid = pet.getUUID();
        petName = pet.getName().getString();
        PetHouseSavedData.get(level.getServer()).setHome(
                petUuid, ownerUuid, petName, level.dimension(), worldPosition,
                getBlockState().getValue(PetHouseBlock.FACING));
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) owner.displayClientMessage(Component.translatable(
                "message.richstuff.pet_house.assigned", pet.getDisplayName(), getName()), true);
        sync();
    }

    private void updatePetName(String name) {
        petName = name;
        if (level instanceof ServerLevel serverLevel && petUuid != null && ownerUuid != null) {
            PetHouseSavedData.get(serverLevel.getServer()).setHome(
                    petUuid, ownerUuid, petName, serverLevel.dimension(), worldPosition,
                    getBlockState().getValue(PetHouseBlock.FACING));
        }
        sync();
    }

    public void clearPetBinding() {
        if (level instanceof ServerLevel serverLevel && petUuid != null) {
            PetHouseSavedData.get(serverLevel.getServer()).removeHome(petUuid, serverLevel.dimension(), worldPosition);
        }
        petUuid = null;
        petName = "";
        searchCooldown = SEARCH_INTERVAL;
        sync();
    }

    public boolean hasOwner() { return ownerUuid != null; }
    public boolean hasPet() { return petUuid != null; }
    public boolean isOwnedBy(Player player) { return ownerUuid != null && ownerUuid.equals(player.getUUID()); }
    public @Nullable UUID ownerUuid() { return ownerUuid; }
    public @Nullable UUID petUuid() { return petUuid; }
    public String ownerName() { return ownerName.isBlank() ? "Unknown Player" : ownerName; }
    public String petName() { return petName.isBlank() ? "Pet" : petName; }

    @Override public Component getName() {
        return hasPet() ? Component.literal(petName() + " Home") : Component.translatable("block.richstuff.pet_house");
    }
    @Nullable @Override public Component getCustomName() { return hasPet() ? getName() : null; }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        if (!ownerName.isBlank()) tag.putString("OwnerName", ownerName);
        if (petUuid != null) tag.putUUID("Pet", petUuid);
        if (!petName.isBlank()) tag.putString("PetName", petName);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        petUuid = tag.hasUUID("Pet") ? tag.getUUID("Pet") : null;
        petName = tag.getString("PetName");
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
