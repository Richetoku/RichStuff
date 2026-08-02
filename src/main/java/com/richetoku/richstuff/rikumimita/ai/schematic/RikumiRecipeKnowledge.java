package com.richetoku.richstuff.rikumimita.ai.schematic;

import java.util.function.Predicate;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Small survival-safe recipe planner for starter construction materials. */
final class RikumiRecipeKnowledge {
    private RikumiRecipeKnowledge() {}

    static boolean ensureBlockItem(FakePlayer player, Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) return true;
        if (find(player, stack -> stack.is(item)) >= 0) return true;

        if (block == Blocks.CHEST) return craft(player, new ItemStack(Items.CHEST), 8, RikumiRecipeKnowledge::isPlank);
        if (block == Blocks.CRAFTING_TABLE) return craft(player, new ItemStack(Items.CRAFTING_TABLE), 4, RikumiRecipeKnowledge::isPlank);
        if (block == Blocks.FURNACE) return craft(player, new ItemStack(Items.FURNACE), 8, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS));
        if (block == Blocks.OAK_DOOR) return craftMultiple(player, new ItemStack(Items.OAK_DOOR, 3), 6, RikumiRecipeKnowledge::isPlank);
        if (block == Blocks.GLASS_PANE) return craftMultiple(player, new ItemStack(Items.GLASS_PANE, 16), 6,
                stack -> stack.is(Items.GLASS));
        if (block == Blocks.RED_BED) {
            ItemStack result = new ItemStack(Items.RED_BED);
            if (count(player, RikumiRecipeKnowledge::isPlank) >= 3
                    && count(player, stack -> stack.is(ItemTags.WOOL)) >= 3 && canAdd(player, result)) {
                consume(player, RikumiRecipeKnowledge::isPlank, 3);
                consume(player, stack -> stack.is(ItemTags.WOOL), 3);
                return player.getInventory().add(result);
            }
            return false;
        }
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) {
            ItemStack result = new ItemStack(Items.TORCH, 4);
            if (count(player, stack -> stack.is(ItemTags.COALS)) >= 1
                    && count(player, stack -> stack.is(Items.STICK)) >= 1 && canAdd(player, result)) {
                consume(player, stack -> stack.is(ItemTags.COALS), 1);
                consume(player, stack -> stack.is(Items.STICK), 1);
                return player.getInventory().add(result);
            }
            return false;
        }
        if (block == Blocks.OAK_PLANKS && count(player, stack -> stack.is(ItemTags.LOGS)) >= 1) {
            ItemStack result = new ItemStack(Items.OAK_PLANKS, 4);
            if (!canAdd(player, result)) return false;
            consume(player, stack -> stack.is(ItemTags.LOGS), 1);
            return player.getInventory().add(result);
        }
        return false;
    }

    static boolean consumeBlockItem(FakePlayer player, Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) return true;
        int slot = find(player, stack -> stack.is(item)
                || (block == Blocks.WALL_TORCH && stack.is(Items.TORCH)));
        if (slot < 0) return false;
        ItemStack stack = player.getInventory().getItem(slot);
        stack.shrink(1);
        if (stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.getInventory().setChanged();
        return true;
    }

    static int countBlock(FakePlayer player, Block block) {
        Item item = block.asItem();
        return item == Items.AIR ? Integer.MAX_VALUE : count(player, stack -> stack.is(item));
    }

    private static boolean craft(FakePlayer player, ItemStack result, int amount, Predicate<ItemStack> ingredient) {
        if (count(player, ingredient) < amount || !canAdd(player, result)) return false;
        consume(player, ingredient, amount);
        return player.getInventory().add(result);
    }

    private static boolean craftMultiple(FakePlayer player, ItemStack result, int amount, Predicate<ItemStack> ingredient) {
        return craft(player, result, amount, ingredient);
    }


    private static boolean canAdd(FakePlayer player, ItemStack result) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, result)
                    && existing.getCount() + result.getCount() <= existing.getMaxStackSize()) return true;
        }
        return false;
    }

    private static boolean isPlank(ItemStack stack) {
        return stack.is(ItemTags.PLANKS);
    }

    private static int find(FakePlayer player, Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (predicate.test(player.getInventory().getItem(slot))) return slot;
        }
        return -1;
    }

    private static int count(FakePlayer player, Predicate<ItemStack> predicate) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (predicate.test(stack)) count += stack.getCount();
        }
        return count;
    }

    private static void consume(FakePlayer player, Predicate<ItemStack> predicate, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!predicate.test(stack)) continue;
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
            if (stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
    }
}
