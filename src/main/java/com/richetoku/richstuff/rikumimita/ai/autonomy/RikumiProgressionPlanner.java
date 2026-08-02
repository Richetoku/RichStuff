package com.richetoku.richstuff.rikumimita.ai.autonomy;

import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.RikumiAction;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiSchematicItem;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiSchematicMarkerBlock;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiSchematicMarkerBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Short, progression-based survival goals for Rikumi's Assist and Auto modes.
 * Every material action consumes inventory resources or uses the survival fake player's game mode.
 * The only free artifact is the schematic plan itself, as requested; blocks and marker still cost materials.
 */
public final class RikumiProgressionPlanner {
    private long nextActionTick;
    /** Keeps each companion committed to one surveyed site until its marker is placed. */
    private final Map<UUID, MarkerSite> plannedMarkerSites = new HashMap<>();


    /**
     * Runs before every autonomous mining/building action. Dark work areas are lit first; when no
     * torches are available Rikumi makes sticks, crafts torches from coal/charcoal, mines coal, or
     * starts a real furnace charcoal cycle from collected logs.
     */
    public boolean prioritizeLighting(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (!avatar.hasHome() || !avatar.isWithinHomeWorkArea()) return false;
        BlockPos work = avatar.getLastMinedBlock() == null ? avatar.getMiningShaft() : avatar.getLastMinedBlock();
        // Lighting is part of active shaft/stair/tunnel work only. It is never a general wandering action.
        if (work == null) return false;
        BlockPos lastTorch = avatar.getLastTorchPosition();
        boolean spaced = lastTorch == null || lastTorch.distSqr(work) >= 36.0D;
        if (!spaced || level.getMaxLocalRawBrightness(work) > 7) return false;

        int torchSlot = find(player, stack -> stack.is(Items.TORCH));
        if (torchSlot >= 0 && placeTorchNear(level, player, avatar, torchSlot, work)) return true;

        if (count(player, stack -> stack.is(Items.STICK)) < 1
                && count(player, stack -> stack.is(ItemTags.PLANKS)) >= 2
                && craft(player, stack -> stack.is(ItemTags.PLANKS), 2, new ItemStack(Items.STICK, 4))) {
            avatar.setGoalStatus("Light the Mine", "Prepare torches before continuing underground work");
            act(avatar, "Crafting", "Made sticks for mine torches", RikumiAction.CRAFT);
            return true;
        }
        if (count(player, stack -> stack.is(ItemTags.COALS)) >= 1
                && count(player, stack -> stack.is(Items.STICK)) >= 1
                && craftTorch(player, avatar)) return true;

        if (collectOrStartCharcoal(level, player, avatar)) return true;

        return harvest(level, player, avatar,
                state -> state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE),
                "Mine Coal", "Finding coal so every stairway and tunnel stays safely lit",
                stack -> stack.is(ItemTags.PICKAXES));
    }

    private static boolean craftTorch(FakePlayer player, RikumiMitaEntity avatar) {
        if (!canAdd(player, new ItemStack(Items.TORCH, 4))) return false;
        consume(player, stack -> stack.is(ItemTags.COALS), 1);
        consume(player, stack -> stack.is(Items.STICK), 1);
        player.getInventory().add(new ItemStack(Items.TORCH, 4));
        player.getInventory().setChanged();
        avatar.setGoalStatus("Light the Mine", "Keep the active mine above unsafe light levels");
        act(avatar, "Crafting", "Crafted four torches for the mine", RikumiAction.CRAFT);
        return true;
    }

    private static boolean placeTorchNear(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar,
                                          int torchSlot, BlockPos preferred) {
        BlockPos center = avatar.blockPosition();
        BlockPos best = null;
        BlockPos support = null;
        Direction face = null;
        for (BlockPos candidate : new BlockPos[]{preferred, center, preferred.above(), center.above()}) {
            if (!avatar.canWorkAt(candidate) || avatar.distanceToSqr(candidate.getCenter()) > 9.0D
                    || level.getMaxLocalRawBrightness(candidate) > 7 || !level.getBlockState(candidate).canBeReplaced()) continue;
            // Prefer a tunnel/shaft wall. This keeps torches off the middle of work floors and stairs.
            for (Direction wall : Direction.Plane.HORIZONTAL) {
                BlockPos wallBlock = candidate.relative(wall);
                Direction placementFace = wall.getOpposite();
                if (level.getBlockState(wallBlock).isFaceSturdy(level, wallBlock, placementFace)) {
                    best = candidate.immutable(); support = wallBlock.immutable(); face = placementFace; break;
                }
            }
            if (best != null) break;
        }
        if (best == null || support == null || face == null) return false;
        avatar.getNavigation().stop();
        avatar.setDeltaMovement(Vec3.ZERO);
        swapIntoSelected(player, torchSlot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5D)),
                face, support, false);
        if (!player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction())
            return false;
        avatar.rememberTorch(best);
        avatar.setGoalStatus("Light the Mine", "Keep the active mine above unsafe light levels");
        act(avatar, "Lighting", "Placed a torch at " + best.toShortString(), RikumiAction.BUILD);
        return true;
    }

    private boolean collectOrStartCharcoal(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        BlockPos furnacePos = findBlockEntity(level, avatar.blockPosition(), AbstractFurnaceBlockEntity.class);
        if (furnacePos != null && level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack result = furnace.getItem(2);
            if (result.is(Items.CHARCOAL) && canAdd(player, result)) {
                player.getInventory().add(result.copy());
                furnace.setItem(2, ItemStack.EMPTY);
                furnace.setChanged();
                player.getInventory().setChanged();
                act(avatar, "Collecting", "Collected charcoal for mine torches", RikumiAction.USE_ITEM);
                return true;
            }
            int logSlot = find(player, stack -> stack.is(ItemTags.LOGS));
            int fuelSlot = find(player, stack -> stack.is(ItemTags.PLANKS) || stack.is(ItemTags.LOGS));
            if (logSlot >= 0 && fuelSlot >= 0) {
                if (avatar.distanceToSqr(furnacePos.getCenter()) > 9.0D) {
                    avatar.getNavigation().moveTo(furnacePos.getX() + 0.5D, furnacePos.getY(), furnacePos.getZ() + 0.5D, 1.0D);
                    act(avatar, "Traveling", "Walking to the furnace to make charcoal", RikumiAction.WALK);
                    return true;
                }
                avatar.getNavigation().stop();
                avatar.setDeltaMovement(Vec3.ZERO);
                if (furnace.getItem(0).isEmpty()) {
                    ItemStack log = player.getInventory().getItem(logSlot);
                    furnace.setItem(0, log.copyWithCount(1));
                    log.shrink(1);
                    if (log.isEmpty()) player.getInventory().setItem(logSlot, ItemStack.EMPTY);
                }
                if (furnace.getItem(1).isEmpty()) {
                    if (fuelSlot == logSlot && player.getInventory().getItem(fuelSlot).isEmpty())
                        fuelSlot = find(player, stack -> stack.is(ItemTags.PLANKS) || stack.is(ItemTags.LOGS));
                    if (fuelSlot >= 0) {
                        ItemStack fuel = player.getInventory().getItem(fuelSlot);
                        furnace.setItem(1, fuel.copyWithCount(1));
                        fuel.shrink(1);
                        if (fuel.isEmpty()) player.getInventory().setItem(fuelSlot, ItemStack.EMPTY);
                    }
                }
                furnace.setChanged();
                player.getInventory().setChanged();
                avatar.setGoalStatus("Make Charcoal", "Produce torch fuel before continuing the mine");
                act(avatar, "Smelting", "Started a charcoal batch for torches", RikumiAction.CRAFT);
                return true;
            }
        }

        if (count(player, stack -> stack.is(ItemTags.LOGS)) < 2) {
            return harvest(level, player, avatar, state -> state.is(BlockTags.LOGS),
                    "Make Charcoal", "Collecting logs for charcoal and furnace fuel", null);
        }
        if (!has(player, Items.FURNACE)) {
            if (count(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS)) >= 8
                    && craft(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS), 8, new ItemStack(Items.FURNACE))) {
                act(avatar, "Crafting", "Crafted a furnace for charcoal", RikumiAction.CRAFT);
                return true;
            }
            return harvest(level, player, avatar,
                    state -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(BlockTags.BASE_STONE_OVERWORLD),
                    "Build Furnace", "Mining stone for a charcoal furnace", stack -> stack.is(ItemTags.PICKAXES));
        }
        BlockPos placement = findUtilityPlacement(level, avatar.blockPosition());
        if (placement == null) return false;
        if (avatar.distanceToSqr(placement.getCenter()) > 9.0D) {
            avatar.getNavigation().moveTo(placement.getX() + 0.5D, placement.getY(), placement.getZ() + 0.5D, 1.0D);
            act(avatar, "Traveling", "Walking to a safe charcoal furnace location", RikumiAction.WALK);
            return true;
        }
        int furnaceSlot = find(player, stack -> stack.is(Items.FURNACE));
        swapIntoSelected(player, furnaceSlot);
        BlockPos ground = placement.below();
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ground).add(0.0D, 0.5D, 0.0D), Direction.UP, ground, false));
        act(avatar, "Building", "Placed a furnace for charcoal production", RikumiAction.BUILD);
        return true;
    }

    /** Foundational progression always runs before optional lighting/exploration behavior. */
    public boolean needsFoundationalProgression(FakePlayer player) {
        return count(player, stack -> stack.is(ItemTags.LOGS)) < 1 && count(player, stack -> stack.is(ItemTags.PLANKS)) < 4
                || count(player, stack -> stack.is(ItemTags.PLANKS)) < 16
                || !has(player, Items.CRAFTING_TABLE)
                || !hasPickaxeAtLeast(player, 2)
                || !has(player, Items.STONE_AXE)
                || !has(player, Items.STONE_SHOVEL);
    }

    /** Returns true when a progression action or navigation decision claimed this helper tick. */
    public boolean tick(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (level.getGameTime() < nextActionTick) return false;
        nextActionTick = level.getGameTime() + 12L;

        if (avatar.getBuildProject().isPresent()) {
            return gatherForActiveBuild(level, player, avatar);
        }
        if (count(player, stack -> stack.is(ItemTags.LOGS)) < 1 && count(player, stack -> stack.is(ItemTags.PLANKS)) < 4) {
            return harvest(level, player, avatar, state -> state.is(BlockTags.LOGS), "Get Wood", "Collecting nearby logs", null);
        }
        if (count(player, stack -> stack.is(ItemTags.PLANKS)) < 16) {
            avatar.setGoalStatus("Make Planks", "Turn collected logs into enough planks for tools and a starter base");
            if (craft(player, stack -> stack.is(ItemTags.LOGS), 1, new ItemStack(Items.OAK_PLANKS, 4))) {
                act(avatar, "Crafting", "Made oak planks from a collected log", RikumiAction.CRAFT);
                return true;
            }
            return harvest(level, player, avatar, state -> state.is(BlockTags.LOGS), "Get Wood", "Collecting more logs for planks", null);
        }
        if (!has(player, Items.CRAFTING_TABLE)) {
            avatar.setGoalStatus("Craft Table", "Make a crafting table for the first tool progression step");
            return craftAndReport(player, avatar, stack -> stack.is(ItemTags.PLANKS), 4,
                    new ItemStack(Items.CRAFTING_TABLE), "Crafted a crafting table");
        }
        if (!has(player, Items.WOODEN_PICKAXE) && !hasPickaxeAtLeast(player, 1)) {
            if (count(player, stack -> stack.is(Items.STICK)) < 2
                    && craft(player, stack -> stack.is(ItemTags.PLANKS), 2, new ItemStack(Items.STICK, 4))) {
                avatar.setGoalStatus("Wooden Pickaxe", "Prepare sticks and planks for the first pickaxe");
                act(avatar, "Crafting", "Made sticks for a wooden pickaxe", RikumiAction.CRAFT);
                return true;
            }
            avatar.setGoalStatus("Wooden Pickaxe", "Craft the first pickaxe to begin mining stone");
            return craftTool(player, avatar, stack -> stack.is(ItemTags.PLANKS), Items.WOODEN_PICKAXE, "Crafted a wooden pickaxe");
        }
        if (!has(player, Items.WOODEN_AXE) && !has(player, Items.STONE_AXE)
                && !has(player, Items.IRON_AXE) && !has(player, Items.DIAMOND_AXE)) {
            ensureSticks(player, avatar, "Wooden Axe");
            avatar.setGoalStatus("Wooden Axe", "Craft an axe so collecting logs is faster and reliable");
            return craftTool(player, avatar, stack -> stack.is(ItemTags.PLANKS), Items.WOODEN_AXE,
                    "Crafted a wooden axe");
        }
        if (count(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS)) < 16) {
            return harvest(level, player, avatar,
                    state -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(BlockTags.BASE_STONE_OVERWORLD),
                    "Mine Stone", "Mining stone for better tools and a furnace", stack -> stack.is(ItemTags.PICKAXES));
        }
        if (!has(player, Items.STONE_PICKAXE) && !hasPickaxeAtLeast(player, 2)) {
            ensureSticks(player, avatar, "Stone Pickaxe");
            avatar.setGoalStatus("Stone Pickaxe", "Upgrade to a stone pickaxe before mining iron");
            return craftTool(player, avatar, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS), Items.STONE_PICKAXE,
                    "Crafted a stone pickaxe");
        }
        if (!has(player, Items.STONE_AXE) && !has(player, Items.IRON_AXE) && !has(player, Items.DIAMOND_AXE)) {
            ensureSticks(player, avatar, "Stone Axe");
            avatar.setGoalStatus("Stone Axe", "Upgrade the wood-cutting tool before gathering larger supplies");
            return craftTool(player, avatar, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS), Items.STONE_AXE,
                    "Crafted a stone axe");
        }
        if (!has(player, Items.STONE_SHOVEL) && !has(player, Items.IRON_SHOVEL) && !has(player, Items.DIAMOND_SHOVEL)) {
            if (count(player, stack -> stack.is(Items.STICK)) < 2) ensureSticks(player, avatar, "Stone Shovel");
            avatar.setGoalStatus("Stone Shovel", "Craft a shovel for dirt and gravel in the mine entrance");
            return craftSimpleTool(player, avatar, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS), 1,
                    Items.STONE_SHOVEL, "Crafted a stone shovel");
        }
        if (cookAvailableFood(level, player, avatar)) return true;

        int rawIron = count(player, stack -> stack.is(Items.RAW_IRON));
        int ironIngots = count(player, stack -> stack.is(Items.IRON_INGOT));
        int ironNeeded = (!hasPickaxeAtLeast(player, 3) ? 3 : 0)
                + (!has(player, Items.IRON_AXE) && !has(player, Items.DIAMOND_AXE) ? 3 : 0)
                + (!has(player, Items.IRON_SHOVEL) && !has(player, Items.DIAMOND_SHOVEL) ? 1 : 0);
        if (rawIron + ironIngots < ironNeeded) {
            return harvest(level, player, avatar, state -> state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE),
                    "Mine Iron", "Finding and mining enough iron for an iron pickaxe", stack -> stack.is(Items.STONE_PICKAXE)
                            || stack.is(Items.IRON_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE));
        }
        if (ironIngots < ironNeeded) {
            return smeltIron(level, player, avatar);
        }
        if (!hasPickaxeAtLeast(player, 3)) {
            ensureSticks(player, avatar, "Iron Pickaxe");
            avatar.setGoalStatus("Iron Pickaxe", "Craft an iron pickaxe so diamond ore can be harvested safely");
            return craftTool(player, avatar, stack -> stack.is(Items.IRON_INGOT), Items.IRON_PICKAXE,
                    "Crafted an iron pickaxe");
        }
        if (!has(player, Items.IRON_AXE) && !has(player, Items.DIAMOND_AXE)) {
            ensureSticks(player, avatar, "Iron Axe");
            avatar.setGoalStatus("Iron Axe", "Upgrade the axe for dependable resource gathering");
            return craftTool(player, avatar, stack -> stack.is(Items.IRON_INGOT), Items.IRON_AXE,
                    "Crafted an iron axe");
        }
        if (!has(player, Items.IRON_SHOVEL) && !has(player, Items.DIAMOND_SHOVEL)) {
            if (count(player, stack -> stack.is(Items.STICK)) < 2) ensureSticks(player, avatar, "Iron Shovel");
            avatar.setGoalStatus("Iron Shovel", "Upgrade the shovel before extending the mine");
            return craftSimpleTool(player, avatar, stack -> stack.is(Items.IRON_INGOT), 1,
                    Items.IRON_SHOVEL, "Crafted an iron shovel");
        }
        if (!avatar.hasCompletedStarterHouse()) {
            return startStarterHouse(level, player, avatar);
        }
        if (!has(player, Items.DIAMOND)) {
            return harvest(level, player, avatar,
                    state -> state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE),
                    "Reach Diamond", "Searching underground for diamond ore with an iron-tier pickaxe",
                    stack -> stack.is(Items.IRON_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE));
        }
        avatar.setGoalStatus("Diamond Reached", "Reached the first full progression milestone and can choose a new project");
        avatar.setTaskStatus("Planning", "Planning the next independent resource or building project");
        return false;
    }

    private boolean gatherForActiveBuild(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        var project = avatar.getBuildProject().orElse(null);
        if (project == null) return false;
        var missing = project.remainingMaterials();
        if (missing.isEmpty()) return false;
        if (missing.keySet().stream().anyMatch(id -> id.getPath().contains("oak_log") || id.getPath().contains("oak_planks")
                || id.getPath().contains("chest") || id.getPath().contains("crafting_table") || id.getPath().contains("door"))) {
            if (count(player, stack -> stack.is(ItemTags.LOGS)) < 8) {
                return harvest(level, player, avatar, state -> state.is(BlockTags.LOGS), "Gather Materials",
                        "Collecting wood required by " + project.displayName(), null);
            }
            if (count(player, stack -> stack.is(ItemTags.PLANKS)) < 32
                    && craft(player, stack -> stack.is(ItemTags.LOGS), 1, new ItemStack(Items.OAK_PLANKS, 4))) {
                act(avatar, "Crafting", "Made planks required by " + project.displayName(), RikumiAction.CRAFT);
                return true;
            }
        }
        if (missing.keySet().stream().anyMatch(id -> id.getPath().contains("glass_pane"))) {
            if (count(player, stack -> stack.is(Items.GLASS)) >= 6
                    && craft(player, stack -> stack.is(Items.GLASS), 6, new ItemStack(Items.GLASS_PANE, 16))) {
                act(avatar, "Crafting", "Made glass panes for the starter house windows", RikumiAction.CRAFT);
                return true;
            }
            if (count(player, stack -> stack.is(Items.SAND)) < 6) {
                return harvest(level, player, avatar, state -> state.is(Blocks.SAND) || state.is(Blocks.RED_SAND),
                        "Gather Materials", "Collecting sand for starter house windows", null);
            }
            return loadFurnace(level, player, avatar, Items.SAND, "Smelting sand into glass for the starter house");
        }
        if (missing.keySet().stream().anyMatch(id -> id.getPath().contains("red_bed"))
                && count(player, stack -> stack.is(ItemTags.WOOL)) < 3) {
            avatar.setGoalStatus("Gather Materials", "Collect three wool and three planks for the starter bed");
            avatar.setTaskStatus("Needs Materials", "Waiting for or collecting wool dropped by nearby sheep");
            return false;
        }
        if (missing.keySet().stream().anyMatch(id -> id.getPath().contains("furnace"))
                && count(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS)) < 8) {
            return harvest(level, player, avatar,
                    state -> state.is(Blocks.STONE) || state.is(BlockTags.BASE_STONE_OVERWORLD),
                    "Gather Materials", "Mining stone for the starter house furnace", stack -> stack.is(ItemTags.PICKAXES));
        }
        if (missing.keySet().stream().anyMatch(id -> id.getPath().contains("torch"))
                && count(player, stack -> stack.is(ItemTags.COALS)) < 1) {
            return harvest(level, player, avatar, state -> state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE),
                    "Gather Materials", "Mining coal for starter house torches", stack -> stack.is(ItemTags.PICKAXES));
        }
        avatar.setGoalStatus("Gather Materials", "Gather the remaining real materials for " + project.displayName());
        avatar.setTaskStatus("Needs Materials", "Missing: " + missing.entrySet().stream().limit(3)
                .map(entry -> entry.getKey() + " x" + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse("unknown materials"));
        return false;
    }

    private boolean startStarterHouse(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        avatar.setGoalStatus("Starter House", "Place a reusable marker and build the built-in oak starter house one block at a time");
        if (!has(player, RichStuff.RIKUMI_SCHEMATIC_MARKER_ITEM.get())) {
            ItemStack markerItem = new ItemStack(RichStuff.RIKUMI_SCHEMATIC_MARKER_ITEM.get());
            if (count(player, stack -> stack.is(ItemTags.PLANKS)) < 4 || count(player, stack -> stack.is(Items.IRON_INGOT)) < 1) {
                avatar.setTaskStatus("Needs Materials", "Needs four planks and one iron ingot to craft a reusable schematic marker");
                return false;
            }
            if (!canAdd(player, markerItem)) {
                avatar.setTaskStatus("Inventory Full", "Needs one free inventory slot before crafting the schematic marker");
                return false;
            }
            consume(player, stack -> stack.is(ItemTags.PLANKS), 4);
            consume(player, stack -> stack.is(Items.IRON_INGOT), 1);
            player.getInventory().add(markerItem);
            player.getInventory().setChanged();
            act(avatar, "Crafting", "Crafted the reusable schematic placement marker", RikumiAction.CRAFT);
            return true;
        }
        if (!hasSchematic(player, RikumiSchematicItem.STARTER_HOUSE)) {
            // Explicitly free: schematic plans are authored data, not physical construction materials.
            ItemStack plan = RikumiSchematicItem.create(RichStuff.RIKUMI_SCHEMATIC_ITEM.get(), RikumiSchematicItem.STARTER_HOUSE);
            if (!player.getInventory().add(plan)) Block.popResource(level, avatar.blockPosition(), plan);
            player.getInventory().setChanged();
            act(avatar, "Planning", "Created the starter-house schematic plan without consuming building materials", RikumiAction.CRAFT);
            return true;
        }

        MarkerSite site = plannedMarkerSites.get(avatar.getUUID());
        if (site != null && !isClearBuildVolume(level, site.pos().relative(site.facing()))) {
            plannedMarkerSites.remove(avatar.getUUID());
            site = null;
        }
        if (site == null) {
            site = findFlatMarkerSite(level, avatar.getHomePosition() == null ? avatar.blockPosition() : avatar.getHomePosition());
            if (site != null) plannedMarkerSites.put(avatar.getUUID(), site);
        }
        if (site == null) {
            avatar.setTaskStatus("Finding Site", "Searching for a clear, level area for the seven-by-seven starter house");
            return false;
        }
        BlockPos markerPos = site.pos();
        if (avatar.distanceToSqr(markerPos.getCenter()) > 9.0D) {
            avatar.getNavigation().moveTo(markerPos.getX() + 0.5D, markerPos.getY(), markerPos.getZ() + 0.5D, 1.05D);
            act(avatar, "Traveling", "Walking to the locked starter-house marker location " + markerPos.toShortString()
                    + " facing " + site.facing().getSerializedName(), RikumiAction.WALK);
            return true;
        }
        if (!avatar.canWorkAt(markerPos)) {
            avatar.getNavigation().stop();
            avatar.setTaskStatus("Outside Home", "The starter-house marker must be inside the home work area");
            return false;
        }
        avatar.getNavigation().stop();
        avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
        int markerSlot = find(player, stack -> stack.is(RichStuff.RIKUMI_SCHEMATIC_MARKER_ITEM.get()));
        if (markerSlot < 0) return false;
        swapIntoSelected(player, markerSlot);
        BlockPos ground = markerPos.below();
        BlockHitResult groundHit = new BlockHitResult(Vec3.atCenterOf(ground).add(0.0D, 0.5D, 0.0D), Direction.UP, ground, false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, groundHit);
        if (!(level.getBlockEntity(markerPos) instanceof RikumiSchematicMarkerBlockEntity marker)) {
            plannedMarkerSites.remove(avatar.getUUID());
            avatar.setTaskStatus("Place Marker", "Could not place the marker at " + markerPos.toShortString() + "; selecting another build site");
            return false;
        }
        // FakePlayer placement would otherwise own the marker. Assign the real companion owner and
        // force the surveyed orientation so the marker, clear-volume test, and rotated schematic agree.
        marker.setOwner(avatar.getOwnerUUID());
        level.setBlock(markerPos, level.getBlockState(markerPos).setValue(RikumiSchematicMarkerBlock.FACING, site.facing()), 3);
        int schematicSlot = find(player, stack -> stack.is(RichStuff.RIKUMI_SCHEMATIC_ITEM.get())
                && RikumiSchematicItem.getSchematicId(stack).equals(RikumiSchematicItem.STARTER_HOUSE));
        if (schematicSlot >= 0) {
            swapIntoSelected(player, schematicSlot);
            BlockHitResult markerHit = new BlockHitResult(Vec3.atCenterOf(markerPos), Direction.UP, markerPos, false);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, markerHit);
        }
        if (!marker.hasSchematic()) {
            avatar.setTaskStatus("Load Marker", "Place the starter-house schematic into the marker");
            return true;
        }
        Direction facing = site.facing();
        if (avatar.startBuildProject(marker.schematicId(), markerPos, facing)) {
            plannedMarkerSites.remove(avatar.getUUID());
            marker.setActiveRikumi(avatar.getUUID());
            act(avatar, "Planning", "Locked the starter-house schematic to marker " + markerPos.toShortString()
                    + " facing " + facing.getSerializedName(), RikumiAction.BUILD);
            return true;
        }
        return false;
    }

    private boolean smeltIron(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        if (count(player, stack -> stack.is(ItemTags.COALS)) < 1) {
            return harvest(level, player, avatar, state -> state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE),
                    "Smelt Iron", "Mining coal to fuel the first iron smelting cycle", stack -> stack.is(ItemTags.PICKAXES));
        }
        return loadFurnace(level, player, avatar, Items.RAW_IRON, "Smelting raw iron for an iron pickaxe");
    }

    private boolean loadFurnace(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar,
                                net.minecraft.world.item.Item inputItem, String detail) {
        BlockPos furnacePos = findBlockEntity(level, avatar.blockPosition(), AbstractFurnaceBlockEntity.class);
        if (furnacePos == null) {
            if (!has(player, Items.FURNACE)) {
                if (!craft(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS), 8, new ItemStack(Items.FURNACE))) {
                    return harvest(level, player, avatar,
                            state -> state.is(Blocks.STONE) || state.is(BlockTags.BASE_STONE_OVERWORLD),
                            "Build Furnace", "Mining stone for a furnace", stack -> stack.is(ItemTags.PICKAXES));
                }
                act(avatar, "Crafting", "Crafted a furnace from mined stone", RikumiAction.CRAFT);
                return true;
            }
            furnacePos = findUtilityPlacement(level, avatar.blockPosition());
            if (furnacePos == null) return false;
            if (avatar.distanceToSqr(furnacePos.getCenter()) > 9.0D) {
                avatar.getNavigation().moveTo(furnacePos.getX() + 0.5D, furnacePos.getY(), furnacePos.getZ() + 0.5D, 1.0D);
                act(avatar, "Traveling", "Walking to a safe furnace placement location", RikumiAction.WALK);
                return true;
            }
            if (!avatar.canWorkAt(furnacePos)) {
                avatar.setTaskStatus("Outside Home", "The furnace must be placed inside the home work area");
                return false;
            }
            avatar.getNavigation().stop();
            avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
            int slot = find(player, stack -> stack.is(Items.FURNACE));
            swapIntoSelected(player, slot);
            BlockPos ground = furnacePos.below();
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0.0D, 0.5D, 0.0D), Direction.UP, ground, false));
            if (!(level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity)) return false;
        }
        if (avatar.distanceToSqr(furnacePos.getCenter()) > 9.0D) {
            avatar.getNavigation().moveTo(furnacePos.getX() + 0.5D, furnacePos.getY(), furnacePos.getZ() + 0.5D, 1.0D);
            act(avatar, "Traveling", "Walking to the furnace", RikumiAction.WALK);
            return true;
        }
        if (!avatar.canWorkAt(furnacePos)) {
            avatar.getNavigation().stop();
            avatar.setTaskStatus("Outside Home", "The furnace is outside the home work area");
            return false;
        }
        avatar.getNavigation().stop();
        avatar.setDeltaMovement(0.0D, avatar.getDeltaMovement().y, 0.0D);
        if (!(level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace)) return false;
        int inputSlot = find(player, stack -> stack.is(inputItem));
        int fuelSlot = find(player, stack -> stack.is(ItemTags.COALS));
        if (inputSlot >= 0 && furnace.getItem(0).isEmpty()) {
            ItemStack source = player.getInventory().getItem(inputSlot);
            int amount = Math.min(source.getCount(), inputItem == Items.RAW_IRON ? 7 : 8);
            furnace.setItem(0, source.copyWithCount(amount));
            source.shrink(amount);
            if (source.isEmpty()) player.getInventory().setItem(inputSlot, ItemStack.EMPTY);
        }
        if (fuelSlot >= 0 && furnace.getItem(1).isEmpty()) {
            ItemStack fuel = player.getInventory().getItem(fuelSlot);
            furnace.setItem(1, fuel.copyWithCount(1));
            fuel.shrink(1);
            if (fuel.isEmpty()) player.getInventory().setItem(fuelSlot, ItemStack.EMPTY);
        }
        furnace.setChanged();
        player.getInventory().setChanged();
        avatar.setGoalStatus(inputItem == Items.RAW_IRON ? "Smelt Iron" : "Make Glass", detail);
        act(avatar, "Smelting", detail, RikumiAction.CRAFT);
        return true;
    }

    private boolean harvest(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar,
                            Predicate<BlockState> blockFilter, String goal, String detail,
                            @Nullable Predicate<ItemStack> toolFilter) {
        String normalizedGoal = goal == null ? "" : goal.toLowerCase(java.util.Locale.ROOT);
        boolean seekExposedOre = !normalizedGoal.contains("stone") && !normalizedGoal.contains("build furnace");
        BlockPos target = isStructuredMiningGoal(goal)
                ? findStructuredMineTarget(level, avatar, blockFilter, seekExposedOre, goal)
                : findNearestBlock(level, avatar.blockPosition(), blockFilter);
        if (target == null) {
            avatar.getNavigation().stop();
            avatar.setDeltaMovement(Vec3.ZERO);
            avatar.setGoalStatus(goal, detail);
            avatar.setTaskStatus("Surveying", "Scanning the home work area and planned mine for " + goal.toLowerCase());
            return true;
        }
        if (toolFilter != null) {
            int tool = find(player, toolFilter);
            if (tool < 0) {
                avatar.setTaskStatus("Needs Tool", "A suitable tool is required for " + goal.toLowerCase());
                return false;
            }
            swapIntoSelected(player, tool);
        }
        RikumiMiningController.Result result = RikumiMiningController.mine(level, player, avatar, target, goal, detail);
        return result == RikumiMiningController.Result.IN_PROGRESS
                || result == RikumiMiningController.Result.COMPLETE;
    }

    private boolean cookAvailableFood(ServerLevel level, FakePlayer player, RikumiMitaEntity avatar) {
        int rawSlot = find(player, RikumiProgressionPlanner::isRawCookableFood);
        BlockPos furnacePos = findBlockEntity(level, avatar.blockPosition(), AbstractFurnaceBlockEntity.class);
        if (furnacePos != null && level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack cooked = furnace.getItem(2);
            if (!cooked.isEmpty() && isCookedFood(cooked) && canAdd(player, cooked)) {
                player.getInventory().add(cooked.copy());
                furnace.setItem(2, ItemStack.EMPTY);
                furnace.setChanged();
                player.getInventory().setChanged();
                act(avatar, "Cooking", "Collected cooked food from the furnace", RikumiAction.CRAFT);
                return true;
            }
        }
        if (rawSlot < 0) return false;
        if (furnacePos == null) {
            if (!has(player, Items.FURNACE)) {
                if (count(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS)) < 8) return false;
                if (craft(player, stack -> stack.is(ItemTags.STONE_CRAFTING_MATERIALS), 8, new ItemStack(Items.FURNACE))) {
                    act(avatar, "Crafting", "Crafted a furnace for cooking and smelting", RikumiAction.CRAFT);
                    return true;
                }
            }
            BlockPos placement = findUtilityPlacement(level, avatar.getHomePosition() == null ? avatar.blockPosition() : avatar.getHomePosition());
            if (placement == null || !avatar.canWorkAt(placement)) return false;
            if (avatar.distanceToSqr(placement.getCenter()) > 9.0D) {
                avatar.getNavigation().moveTo(placement.getX() + 0.5D, placement.getY(), placement.getZ() + 0.5D, 1.0D);
                act(avatar, "Traveling", "Walking to the home furnace location", RikumiAction.WALK);
                return true;
            }
            int furnaceSlot = find(player, stack -> stack.is(Items.FURNACE));
            if (furnaceSlot < 0) return false;
            swapIntoSelected(player, furnaceSlot);
            BlockPos ground = placement.below();
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0.0D, 0.5D, 0.0D), Direction.UP, ground, false));
            act(avatar, "Building", "Placed the home furnace", RikumiAction.BUILD);
            return true;
        }
        if (avatar.distanceToSqr(furnacePos.getCenter()) > 9.0D) {
            avatar.getNavigation().moveTo(furnacePos.getX() + 0.5D, furnacePos.getY(), furnacePos.getZ() + 0.5D, 1.0D);
            act(avatar, "Traveling", "Walking to the furnace to cook food", RikumiAction.WALK);
            return true;
        }
        if (!(level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace)) return false;
        int fuelSlot = find(player, stack -> stack.is(ItemTags.COALS) || stack.is(ItemTags.PLANKS));
        if (fuelSlot < 0 || !furnace.getItem(0).isEmpty()) return false;
        ItemStack raw = player.getInventory().getItem(rawSlot);
        furnace.setItem(0, raw.copyWithCount(1));
        raw.shrink(1);
        if (raw.isEmpty()) player.getInventory().setItem(rawSlot, ItemStack.EMPTY);
        if (furnace.getItem(1).isEmpty()) {
            ItemStack fuel = player.getInventory().getItem(fuelSlot);
            furnace.setItem(1, fuel.copyWithCount(1));
            fuel.shrink(1);
            if (fuel.isEmpty()) player.getInventory().setItem(fuelSlot, ItemStack.EMPTY);
        }
        furnace.setChanged();
        player.getInventory().setChanged();
        act(avatar, "Cooking", "Started cooking food for continued autonomous work", RikumiAction.CRAFT);
        return true;
    }

    private static boolean isRawCookableFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN)
                || stack.is(Items.MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COD)
                || stack.is(Items.SALMON) || stack.is(Items.POTATO) || stack.is(Items.KELP);
    }

    private static boolean isCookedFood(ItemStack stack) {
        return stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.COOKED_MUTTON) || stack.is(Items.COOKED_RABBIT) || stack.is(Items.COOKED_COD)
                || stack.is(Items.COOKED_SALMON) || stack.is(Items.BAKED_POTATO) || stack.is(Items.DRIED_KELP);
    }

    private static boolean craftSimpleTool(FakePlayer player, RikumiMitaEntity avatar, Predicate<ItemStack> head,
                                           int headCount, net.minecraft.world.item.Item result, String detail) {
        ItemStack crafted = new ItemStack(result);
        if (count(player, head) < headCount || count(player, stack -> stack.is(Items.STICK)) < 2 || !canAdd(player, crafted)) return false;
        consume(player, head, headCount);
        consume(player, stack -> stack.is(Items.STICK), 2);
        player.getInventory().add(crafted);
        player.getInventory().setChanged();
        act(avatar, "Crafting", detail, RikumiAction.CRAFT);
        return true;
    }

    private static boolean craftTool(FakePlayer player, RikumiMitaEntity avatar, Predicate<ItemStack> head,
                                     net.minecraft.world.item.Item result, String detail) {
        ItemStack crafted = new ItemStack(result);
        if (count(player, head) < 3 || count(player, stack -> stack.is(Items.STICK)) < 2 || !canAdd(player, crafted)) return false;
        consume(player, head, 3);
        consume(player, stack -> stack.is(Items.STICK), 2);
        if (!player.getInventory().add(crafted)) return false;
        player.getInventory().setChanged();
        act(avatar, "Crafting", detail, RikumiAction.CRAFT);
        return true;
    }

    private static boolean ensureSticks(FakePlayer player, RikumiMitaEntity avatar, String goal) {
        if (count(player, stack -> stack.is(Items.STICK)) >= 2) return true;
        avatar.setGoalStatus(goal, "Make sticks required for the next tool upgrade");
        if (craft(player, stack -> stack.is(ItemTags.PLANKS), 2, new ItemStack(Items.STICK, 4))) {
            act(avatar, "Crafting", "Made sticks for the next pickaxe", RikumiAction.CRAFT);
            return true;
        }
        return false;
    }

    private static boolean craftAndReport(FakePlayer player, RikumiMitaEntity avatar, Predicate<ItemStack> ingredient,
                                          int amount, ItemStack result, String detail) {
        if (!craft(player, ingredient, amount, result)) return false;
        act(avatar, "Crafting", detail, RikumiAction.CRAFT);
        return true;
    }

    private static boolean craft(FakePlayer player, Predicate<ItemStack> ingredient, int amount, ItemStack result) {
        if (count(player, ingredient) < amount || !canAdd(player, result)) return false;
        consume(player, ingredient, amount);
        player.getInventory().add(result);
        player.getInventory().setChanged();
        return true;
    }

    private static void act(RikumiMitaEntity avatar, String descriptor, String detail, RikumiAction action) {
        avatar.setTaskStatus(descriptor, detail);
        avatar.setActionState(action, 14);
    }

    private static boolean isStructuredMiningGoal(String goal) {
        String value = goal == null ? "" : goal.toLowerCase(java.util.Locale.ROOT);
        return value.contains("mine stone") || value.contains("mine iron") || value.contains("reach diamond")
                || value.contains("build furnace") || value.contains("smelt iron") || value.contains("mine coal");
    }

    /**
     * Chooses blocks along one persistent, two-block-high switchback staircase and then a planned
     * branch-mine grid at the resource level. The entrance, direction, depth target, stair progress,
     * tunnel progress, last mined block, and last torch are all saved by the avatar.
     */
    @Nullable
    private static BlockPos findStructuredMineTarget(ServerLevel level, RikumiMitaEntity avatar,
                                                      Predicate<BlockState> requestedResource,
                                                      boolean seekExposedResource, String goal) {
        BlockPos home = avatar.getHomePosition();
        if (home == null) return null;
        BlockPos shaft = avatar.getMiningShaft();
        Direction direction = avatar.getMiningDirection();
        if (shaft == null) {
            Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            direction = directions[Math.floorMod(avatar.getUUID().hashCode(), directions.length)];
            int x = home.getX() + direction.getStepX() * 6;
            int z = home.getZ() + direction.getStepZ() * 6;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            shaft = new BlockPos(x, y, z);
            avatar.rememberMiningShaft(shaft, direction);
            avatar.setMiningStep(0);
            avatar.setMiningTunnelStep(0);
        }

        int requestedY = targetYForGoal(level, home, goal);
        if (avatar.getMiningTargetY() == Integer.MAX_VALUE || requestedY < avatar.getMiningTargetY()) {
            avatar.setMiningTargetY(requestedY);
            avatar.setMiningTunnelStep(0);
        }
        int targetY = avatar.getMiningTargetY();

        int descentCells = Math.max(0, shaft.getY() - targetY);
        int stairStep = avatar.getMiningStep();
        int stairCell = Math.max(0, stairStep / 2);
        if (stairCell < descentCells) {
            BlockPos stairFoot = switchbackCell(shaft, direction, stairCell).below(stairCell);
            BlockPos scanCenter = avatar.getLastMinedBlock() == null ? stairFoot : avatar.getLastMinedBlock();
            if (seekExposedResource) {
                BlockPos ore = nearestMatching(level, scanCenter, requestedResource, avatar, 3, 3);
                if (ore != null) return ore;
            }
            for (int guard = 0; guard < 128; guard++) {
                stairStep = avatar.getMiningStep();
                stairCell = Math.max(0, stairStep / 2);
                if (stairCell >= descentCells) break;
                stairFoot = switchbackCell(shaft, direction, stairCell).below(stairCell);
                BlockPos target = (stairStep & 1) == 0 ? stairFoot.above() : stairFoot;
                if (!avatar.canWorkAt(target)) return null;
                BlockState state = level.getBlockState(target);
                if (!state.isAir() && state.getDestroySpeed(level, target) >= 0.0F
                        && RikumiPlacementLedger.mayRikumiBreak(level, target)) return target.immutable();
                avatar.setMiningStep(stairStep + 1);
            }
        }

        BlockPos bottom = switchbackCell(shaft, direction, Math.max(0, descentCells - 1))
                .below(Math.max(0, descentCells));
        int tunnelStep = avatar.getMiningTunnelStep();
        int tunnelCell = Math.max(0, tunnelStep / 2);
        BlockPos planned = branchMineCell(bottom, direction, tunnelCell);
        BlockPos scanCenter = avatar.getLastMinedBlock() == null ? planned : avatar.getLastMinedBlock();
        if (seekExposedResource) {
            BlockPos ore = nearestMatching(level, scanCenter, requestedResource, avatar, 4, 3);
            if (ore != null) return ore;
        }

        for (int guard = 0; guard < 256; guard++) {
            tunnelStep = avatar.getMiningTunnelStep();
            tunnelCell = Math.max(0, tunnelStep / 2);
            if (tunnelCell > 512) return null;
            planned = branchMineCell(bottom, direction, tunnelCell);
            BlockPos target = (tunnelStep & 1) == 0 ? planned.above() : planned;
            if (!avatar.canWorkAt(target)) {
                avatar.setMiningTunnelStep(tunnelStep + 2);
                continue;
            }
            BlockState state = level.getBlockState(target);
            if (!state.isAir() && state.getDestroySpeed(level, target) >= 0.0F
                    && RikumiPlacementLedger.mayRikumiBreak(level, target)) return target.immutable();
            avatar.setMiningTunnelStep(tunnelStep + 1);
        }
        return null;
    }

    private static int targetYForGoal(ServerLevel level, BlockPos home, String goal) {
        String value = goal == null ? "" : goal.toLowerCase(java.util.Locale.ROOT);
        int target;
        if (value.contains("diamond")) target = -54;
        else if (value.contains("iron")) target = 16;
        else if (value.contains("coal")) target = Math.min(48, home.getY() - 16);
        else target = home.getY() - 12;
        return Math.max(level.getMinBuildHeight() + 6, Math.min(home.getY() - 4, target));
    }

    /** A compact serpentine stair descends to deep levels while staying near Home. */
    private static BlockPos switchbackCell(BlockPos entrance, Direction direction, int cell) {
        int rowLength = 12;
        int row = Math.max(0, cell) / rowLength;
        int column = Math.max(0, cell) % rowLength;
        int along = (row & 1) == 0 ? column : rowLength - 1 - column;
        Direction side = direction.getClockWise();
        return entrance.relative(direction, along).relative(side, row);
    }

    /** Main galleries alternate with left/right exploratory branches and always remain connected. */
    private static BlockPos branchMineCell(BlockPos bottom, Direction direction, int cell) {
        int cycle = Math.max(0, cell) / 24;
        int within = Math.max(0, cell) % 24;
        int mainDistance = cycle * 8 + Math.min(7, within) + 1;
        BlockPos anchor = bottom.relative(direction, cycle * 8 + 8);
        if (within < 8) return bottom.relative(direction, mainDistance);
        int branchDistance = within < 16 ? within - 7 : within - 15;
        Direction side = (within < 16 ? direction.getClockWise() : direction.getCounterClockWise());
        return anchor.relative(side, branchDistance);
    }

    @Nullable
    private static BlockPos nearestMatching(ServerLevel level, BlockPos center, Predicate<BlockState> predicate,
                                            RikumiMitaEntity avatar, int horizontal, int vertical) {
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontal, -vertical, -horizontal),
                center.offset(horizontal, vertical, horizontal))) {
            if (!predicate.test(level.getBlockState(pos)) || !avatar.canWorkAt(pos)
                    || !RikumiPlacementLedger.mayRikumiBreak(level, pos)
                    || !isExposedAndReachable(level, avatar, pos)) continue;
            double distance = pos.distSqr(center);
            if (distance < best) { best = distance; nearest = pos.immutable(); }
        }
        return nearest;
    }

    private static boolean isExposedAndReachable(ServerLevel level, RikumiMitaEntity avatar, BlockPos ore) {
        for (Direction direction : Direction.values()) {
            BlockPos exposed = ore.relative(direction);
            if (!level.getBlockState(exposed).getCollisionShape(level, exposed).isEmpty()) continue;
            for (BlockPos stand : new BlockPos[]{exposed, exposed.below(), exposed.above()}) {
                if (!avatar.canWorkAt(stand)) continue;
                if (!level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()) continue;
                if (!level.getBlockState(stand.above()).getCollisionShape(level, stand.above()).isEmpty()) continue;
                if (!level.getBlockState(stand.below()).isFaceSturdy(level, stand.below(), Direction.UP)) continue;
                if (avatar.distanceToSqr(stand.getCenter()) <= 9.0D) return true;
                var path = avatar.getNavigation().createPath(stand, 0);
                if (path != null && path.canReach()) return true;
            }
        }
        return false;
    }

    @Nullable
    private static BlockPos findNearestBlock(ServerLevel level, BlockPos origin, Predicate<BlockState> predicate) {
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-24, -10, -24), origin.offset(24, 10, 24))) {
            if (!predicate.test(level.getBlockState(pos))) continue;
            if (!RikumiPlacementLedger.mayRikumiBreak(level, pos)) continue;
            double distance = pos.distSqr(origin);
            if (distance < best) { best = distance; nearest = pos.immutable(); }
        }
        return nearest;
    }

    @Nullable
    private static MarkerSite findFlatMarkerSite(ServerLevel level, BlockPos origin) {
        for (int radius = 7; radius <= 18; radius += 2) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = origin.getX() + direction.getStepX() * radius;
                int z = origin.getZ() + direction.getStepZ() * radius;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos marker = new BlockPos(x, y, z);
                Direction facing = direction.getOpposite();
                BlockPos buildOrigin = marker.relative(facing);
                if (isClearBuildVolume(level, buildOrigin)) return new MarkerSite(marker.immutable(), facing);
            }
        }
        return null;
    }

    private static boolean isClearBuildVolume(ServerLevel level, BlockPos origin) {
        int floorY = origin.getY();
        for (int x = -1; x <= 7; x++) for (int z = -1; z <= 7; z++) {
            BlockPos floor = origin.offset(x, 0, z);
            if (!level.getBlockState(floor.below()).isFaceSturdy(level, floor.below(), Direction.UP)) return false;
            for (int y = 0; y <= 4; y++) if (!level.getBlockState(new BlockPos(floor.getX(), floorY + y, floor.getZ())).canBeReplaced()) return false;
        }
        return true;
    }

    @Nullable
    private static BlockPos findUtilityPlacement(ServerLevel level, BlockPos origin) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = origin.relative(direction, 2);
            if (level.getBlockState(pos).canBeReplaced()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return pos;
        }
        return null;
    }

    @Nullable
    private static <T> BlockPos findBlockEntity(ServerLevel level, BlockPos origin, Class<T> type) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 3, 8))) {
            if (type.isInstance(level.getBlockEntity(pos))) return pos.immutable();
        }
        return null;
    }

    private static boolean hasPickaxeAtLeast(FakePlayer player, int tier) {
        return find(player, stack -> switch (tier) {
            case 1 -> stack.is(ItemTags.PICKAXES);
            case 2 -> stack.is(Items.STONE_PICKAXE) || stack.is(Items.IRON_PICKAXE)
                    || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
            default -> stack.is(Items.IRON_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
        }) >= 0;
    }

    private static boolean has(FakePlayer player, net.minecraft.world.item.Item item) { return find(player, stack -> stack.is(item)) >= 0; }
    private static boolean hasSchematic(FakePlayer player, ResourceLocation id) {
        return find(player, stack -> stack.is(RichStuff.RIKUMI_SCHEMATIC_ITEM.get())
                && RikumiSchematicItem.getSchematicId(stack).equals(id)) >= 0;
    }
    private static int find(FakePlayer player, Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (predicate.test(player.getInventory().getItem(slot))) return slot;
        return -1;
    }
    private static int count(FakePlayer player, Predicate<ItemStack> predicate) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }
    private static void consume(FakePlayer player, Predicate<ItemStack> predicate, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!predicate.test(stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            if (stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
    }
    private static boolean canAdd(FakePlayer player, ItemStack result) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, result)
                    && existing.getCount() + result.getCount() <= existing.getMaxStackSize()) return true;
        }
        return false;
    }
    private record MarkerSite(BlockPos pos, Direction facing) {}

    private static void swapIntoSelected(FakePlayer player, int slot) {
        if (slot < 0) return;
        int selected = player.getInventory().selected;
        if (slot == selected) return;
        ItemStack target = player.getInventory().getItem(slot).copy();
        ItemStack current = player.getInventory().getItem(selected).copy();
        player.getInventory().setItem(selected, target);
        player.getInventory().setItem(slot, current);
        player.getInventory().setChanged();
    }
}
