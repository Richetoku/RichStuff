package com.richetoku.richstuff.rikumimita.ai.schematic;

import com.richetoku.richstuff.RichStuff;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Stores the selected schematic and server-authoritative placement ownership/orientation data. */
public final class RikumiSchematicMarkerBlockEntity extends BlockEntity {
    private ItemStack schematic = ItemStack.EMPTY;
    private UUID owner;
    private UUID activeRikumi;

    public RikumiSchematicMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(RichStuff.RIKUMI_SCHEMATIC_MARKER_ENTITY.get(), pos, state);
    }

    public ItemStack getSchematic() { return schematic; }
    public boolean hasSchematic() { return !schematic.isEmpty() && schematic.is(RichStuff.RIKUMI_SCHEMATIC_ITEM.get()); }
    public ResourceLocation schematicId() { return hasSchematic() ? RikumiSchematicItem.getSchematicId(schematic) : RikumiSchematicItem.STARTER_HOUSE; }
    @Nullable public UUID owner() { return owner; }
    @Nullable public UUID activeRikumi() { return activeRikumi; }

    public void setOwner(@Nullable UUID value) { owner = value; changedAndSync(); }
    public void setActiveRikumi(@Nullable UUID value) { activeRikumi = value; changedAndSync(); }

    public void setSchematic(ItemStack stack) {
        schematic = stack.is(RichStuff.RIKUMI_SCHEMATIC_ITEM.get()) ? stack.copyWithCount(1) : ItemStack.EMPTY;
        changedAndSync();
    }

    public ItemStack removeSchematic() {
        ItemStack removed = schematic;
        schematic = ItemStack.EMPTY;
        changedAndSync();
        return removed;
    }

    private void changedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!schematic.isEmpty()) tag.put("Schematic", schematic.save(registries));
        if (owner != null) tag.putUUID("Owner", owner);
        if (activeRikumi != null) tag.putUUID("ActiveRikumi", activeRikumi);
        tag.putString("SchematicId", schematicId().toString());
        tag.putString("Orientation", getBlockState().getValue(RikumiSchematicMarkerBlock.FACING).getSerializedName());
        tag.putString("Mirror", "none");
        tag.putLong("PlacementMarker", worldPosition.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        schematic = tag.contains("Schematic") ? ItemStack.parseOptional(registries, tag.getCompound("Schematic")) : ItemStack.EMPTY;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        activeRikumi = tag.hasUUID("ActiveRikumi") ? tag.getUUID("ActiveRikumi") : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
