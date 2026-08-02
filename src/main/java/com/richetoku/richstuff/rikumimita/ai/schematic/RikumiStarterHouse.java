package com.richetoku.richstuff.rikumimita.ai.schematic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** A small seven-by-seven oak starter home that can be built one real material at a time. */
final class RikumiStarterHouse {
    private RikumiStarterHouse() {}

    static RikumiSchematic create() {
        List<RikumiSchematic.Placement> blocks = new ArrayList<>();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState logs = Blocks.OAK_LOG.defaultBlockState();

        // Floor and flat overhanging roof.
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) add(blocks, x, 0, z, planks, true);
        for (int x = -1; x <= 7; x++) for (int z = -1; z <= 7; z++) add(blocks, x, 4, z, planks, true);

        // Log corners and plank walls, with front door and four windows left open.
        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) {
                boolean edge = x == 0 || x == 6 || z == 0 || z == 6;
                if (!edge) continue;
                boolean corner = (x == 0 || x == 6) && (z == 0 || z == 6);
                boolean door = z == 0 && x == 3 && y <= 2;
                boolean window = y == 2 && ((z == 0 || z == 6) && (x == 1 || x == 5)
                        || (x == 0 || x == 6) && (z == 2 || z == 4));
                if (door || window) continue;
                add(blocks, x, y, z, corner ? logs : planks, true);
            }
        }

        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        for (BlockPos window : List.of(new BlockPos(1, 2, 0), new BlockPos(5, 2, 0),
                new BlockPos(1, 2, 6), new BlockPos(5, 2, 6),
                new BlockPos(0, 2, 2), new BlockPos(0, 2, 4),
                new BlockPos(6, 2, 2), new BlockPos(6, 2, 4))) {
            blocks.add(new RikumiSchematic.Placement(window, glass, true));
        }

        BlockState lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.FACING, Direction.NORTH);
        BlockState upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        add(blocks, 3, 1, 0, lowerDoor, true);
        add(blocks, 3, 2, 0, upperDoor, false);

        BlockState bedFoot = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.FACING, Direction.SOUTH);
        BlockState bedHead = bedFoot.setValue(BedBlock.PART, BedPart.HEAD);
        add(blocks, 1, 1, 4, bedFoot, true);
        add(blocks, 1, 1, 5, bedHead, false);
        add(blocks, 5, 1, 5, Blocks.CHEST.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), true);
        add(blocks, 4, 1, 5, Blocks.CRAFTING_TABLE.defaultBlockState(), true);
        add(blocks, 3, 1, 5, Blocks.FURNACE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), true);
        add(blocks, 1, 1, 1, Blocks.TORCH.defaultBlockState(), true);
        add(blocks, 5, 1, 1, Blocks.TORCH.defaultBlockState(), true);
        add(blocks, 1, 3, 5, Blocks.WALL_TORCH.defaultBlockState(), true);
        add(blocks, 5, 3, 5, Blocks.WALL_TORCH.defaultBlockState(), true);

        blocks.sort(Comparator.comparingInt((RikumiSchematic.Placement p) -> p.offset().getY())
                .thenComparingInt(p -> p.offset().getZ())
                .thenComparingInt(p -> p.offset().getX()));
        return new RikumiSchematic(ResourceLocation.fromNamespaceAndPath("richstuff", "starter_house"),
                "Cute Oak Starter House", "built_in", blocks);
    }

    private static void add(List<RikumiSchematic.Placement> blocks, int x, int y, int z,
                            BlockState state, boolean consumesItem) {
        blocks.add(new RikumiSchematic.Placement(new BlockPos(x, y, z), state, consumesItem));
    }
}
