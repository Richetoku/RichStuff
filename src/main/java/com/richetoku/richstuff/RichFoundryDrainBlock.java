package com.richetoku.richstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Foundry shell block that exposes the nearest controller to an attached faucet. */
public final class RichFoundryDrainBlock extends Block {
    public RichFoundryDrainBlock(BlockBehaviour.Properties properties) { super(properties); }
    public static RichFoundryBlockEntity findController(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x=-16;x<=16;x++) for(int y=-16;y<=16;y++) for(int z=-16;z<=16;z++) {
            cursor.set(origin.getX()+x,origin.getY()+y,origin.getZ()+z);
            if (level.getBlockEntity(cursor) instanceof RichFoundryBlockEntity foundry && foundry.formed()) return foundry;
        }
        return null;
    }
}
