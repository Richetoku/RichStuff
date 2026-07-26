package com.richetoku.richstuff.rikumimita;

import com.richetoku.richstuff.RichStuff;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * RichStuff's native Rikumi Mita companion.
 *
 * <p>The entity, owner state, inventory, outfit state, renderer, model, and menu are all
 * implemented by RichStuff. No external maid entity or model loader is used.</p>
 */
public final class RikumiMitaEntity extends TamableAnimal implements MenuProvider {
    private static final EntityDataAccessor<Integer> OUTFIT =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VOICE_ENABLED =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NAMEPLATE_ENABLED =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.BOOLEAN);

    private final ItemStackHandler inventory = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            RikumiMitaEntity.this.setPersistenceRequired();
        }
    };

    public RikumiMitaEntity(EntityType<? extends RikumiMitaEntity> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("Rikumi Mita"));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.05D, 4.0F, 2.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OUTFIT, 0);
        builder.define(VOICE_ENABLED, true);
        builder.define(NAMEPLATE_ENABLED, true);
    }

    public ItemStackHandler getInventoryHandler() {
        return inventory;
    }

    public int getOutfitIndex() {
        return entityData.get(OUTFIT);
    }

    public void setOutfitIndex(int index) {
        entityData.set(OUTFIT, Math.floorMod(index, OutfitRegistry.OUTFITS.size()));
    }

    public boolean isVoiceEnabled() {
        return entityData.get(VOICE_ENABLED);
    }

    public void setVoiceEnabled(boolean value) {
        entityData.set(VOICE_ENABLED, value);
    }

    public boolean isNameplateEnabled() {
        return entityData.get(NAMEPLATE_ENABLED);
    }

    public void setNameplateEnabled(boolean value) {
        entityData.set(NAMEPLATE_ENABLED, value);
        setCustomNameVisible(value);
    }

    public boolean mayConfigure(Player player) {
        UUID ownerId = getOwnerUUID();
        return player.isCreative() || (ownerId != null && ownerId.equals(player.getUUID()));
    }

    /**
     * Assigns ownership by the player's persistent Minecraft UUID. Display names are never
     * taken from the operating system or stored as the source of truth.
     */
    public void assignOwner(UUID ownerId) {
        setOwnerUUID(ownerId);
        setTame(true, true);
    }

    /**
     * Resolves the owner's current in-game Minecraft username from their UUID.
     * "Player" is used only when that UUID is not currently online/resolvable.
     */
    public String getOwnerDisplayName() {
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String profileName = owner.getGameProfile().getName();
            if (profileName != null && !profileName.isBlank()) {
                return profileName;
            }
        }
        return "Player";
    }

    @Nullable
    private ServerPlayer onlineOwner() {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(ownerId);
    }

    private Component playerStyleChat(Component message) {
        return Component.translatable("chat.type.text", getDisplayName(), message);
    }

    /** Sends Rikumi's dialogue to chat using the same visible format as player chat. */
    public void sendDialogueToOwner(String message) {
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            owner.sendSystemMessage(playerStyleChat(Component.literal(message)));
        }
    }

    public void greetOwnerFromPresent() {
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String ownerName = getOwnerDisplayName();
            owner.sendSystemMessage(playerStyleChat(Component.literal(
                    "Hi, " + ownerName + "! I'm here and ready to help.")));
        }
    }

    public void cycleOutfit(int delta) {
        setOutfitIndex(getOutfitIndex() + delta);
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String ownerName = getOwnerDisplayName();
            String outfitName = OutfitRegistry.byIndex(getOutfitIndex()).label();
            owner.sendSystemMessage(playerStyleChat(Component.literal(
                    "What do you think of my " + outfitName + " outfit, " + ownerName + "?")));
        }
    }

    public void toggleSitFollow() {
        boolean sitting = !isOrderedToSit();
        setOrderedToSit(sitting);
        setInSittingPose(sitting);
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String ownerName = getOwnerDisplayName();
            String message = sitting
                    ? "I'll wait right here, " + ownerName + "."
                    : "I'm with you, " + ownerName + ". Let's go!";
            owner.sendSystemMessage(playerStyleChat(Component.literal(message)));
        }
    }

    public void toggleVoiceWithDialogue() {
        setVoiceEnabled(!isVoiceEnabled());
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String ownerName = getOwnerDisplayName();
            String message = isVoiceEnabled()
                    ? "Voice is on again, " + ownerName + "."
                    : "I'll use chat only for now, " + ownerName + ".";
            owner.sendSystemMessage(playerStyleChat(Component.literal(message)));
        }
    }

    public void toggleNameplateWithDialogue() {
        setNameplateEnabled(!isNameplateEnabled());
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String ownerName = getOwnerDisplayName();
            String message = isNameplateEnabled()
                    ? "My nameplate is visible, " + ownerName + "."
                    : "I've hidden my nameplate, " + ownerName + ".";
            owner.sendSystemMessage(playerStyleChat(Component.literal(message)));
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!mayConfigure(player)) {
            if (!level().isClientSide()) {
                playSound(SoundEvents.VILLAGER_NO, 0.65F, 1.05F);
            }
            return InteractionResult.FAIL;
        }

        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide()) {
                toggleSitFollow();
                playSound(SoundEvents.WOOL_PLACE, 0.45F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }

        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, buffer -> buffer.writeVarInt(getId()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Rikumi Mita");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return mayConfigure(player) ? new RikumiMitaMenu(containerId, playerInventory, this) : null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        HolderLookup.Provider registries = level().registryAccess();
        tag.put("RikumiInventory", inventory.serializeNBT(registries));
        tag.putInt("Outfit", getOutfitIndex());
        tag.putBoolean("VoiceEnabled", isVoiceEnabled());
        tag.putBoolean("NameplateEnabled", isNameplateEnabled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        HolderLookup.Provider registries = level().registryAccess();
        if (tag.contains("RikumiInventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("RikumiInventory"));
        }
        setOutfitIndex(tag.getInt("Outfit"));
        setVoiceEnabled(!tag.contains("VoiceEnabled") || tag.getBoolean("VoiceEnabled"));
        setNameplateEnabled(!tag.contains("NameplateEnabled") || tag.getBoolean("NameplateEnabled"));
        // Legacy OwnerDisplayName values are intentionally ignored. The UUID is authoritative.
    }
}
