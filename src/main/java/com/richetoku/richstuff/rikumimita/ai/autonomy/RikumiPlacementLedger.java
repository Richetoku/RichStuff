package com.richetoku.richstuff.rikumimita.ai.autonomy;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Per-dimension record of blocks explicitly placed by real players.
 *
 * <p>Rikumi's survival fake player is deliberately excluded, so naturally generated blocks and
 * blocks placed by Rikumi remain valid work targets. The ledger is persisted with the world and is
 * consulted by every autonomous or externally requested Rikumi mining action.</p>
 */
public final class RikumiPlacementLedger extends SavedData {
    private static final String DATA_NAME = "richstuff_rikumi_player_placements";
    private static final Factory<RikumiPlacementLedger> FACTORY =
            new Factory<>(RikumiPlacementLedger::new, RikumiPlacementLedger::load);

    private final Set<Long> playerPlaced = new HashSet<>();

    public static RikumiPlacementLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static boolean mayRikumiBreak(ServerLevel level, BlockPos pos) {
        return !get(level).playerPlaced.contains(pos.asLong());
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor accessor = event.getLevel();
        Entity entity = event.getEntity();
        if (!(accessor instanceof ServerLevel level) || !(entity instanceof Player) || entity instanceof FakePlayer) return;
        RikumiPlacementLedger ledger = get(level);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) ledger.mark(snapshot.getPos());
        } else {
            ledger.mark(event.getPos());
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.isCanceled()) return;
        // A position can safely be forgotten after either its owner or another permitted actor breaks it.
        get(level).unmark(event.getPos());
    }

    private void mark(BlockPos pos) {
        if (playerPlaced.add(pos.asLong())) setDirty();
    }

    private void unmark(BlockPos pos) {
        if (playerPlaced.remove(pos.asLong())) setDirty();
    }

    private static RikumiPlacementLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        RikumiPlacementLedger ledger = new RikumiPlacementLedger();
        for (long packed : tag.getLongArray("PlayerPlaced")) ledger.playerPlaced.add(packed);
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] packed = new long[playerPlaced.size()];
        int index = 0;
        for (long value : playerPlaced) packed[index++] = value;
        tag.putLongArray("PlayerPlaced", packed);
        return tag;
    }
}
