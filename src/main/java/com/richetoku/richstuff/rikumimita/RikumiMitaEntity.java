package com.richetoku.richstuff.rikumimita;

import com.richetoku.richcore.RichCoreConfig;
import com.richetoku.richstuff.rikumimita.ai.RikumiAiLifecycle;
import com.richetoku.richstuff.rikumimita.ai.autonomy.RikumiMiningController;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiBuildProject;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiSchematic;
import com.richetoku.richstuff.rikumimita.ai.schematic.RikumiSchematicRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Native visible companion with local mob AI and an optional server-authoritative fake-player actor. */
public final class RikumiMitaEntity extends TamableAnimal implements MenuProvider {
    private static final EntityDataAccessor<Integer> OUTFIT =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VOICE_ENABLED =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NAMEPLATE_ENABLED =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> CURRENT_TASK =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_GOAL =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_PROJECT =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_TASK_DETAIL =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_GOAL_DETAIL =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_PROJECT_DETAIL =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> HOME_POSITION =
            SynchedEntityData.defineId(RikumiMitaEntity.class, EntityDataSerializers.STRING);
    public static final int HOME_WORK_RADIUS = 64;

    private final RikumiInventoryBridge inventory = new RikumiInventoryBridge(this::setPersistenceRequired);
    private int actionTicks;
    private long nextModeDecisionTick;
    private long nextPathDebugTick;
    private BlockPos autoAnchor;
    @Nullable private BlockPos miningShaft;
    @Nullable private BlockPos lastMinedBlock;
    private Direction miningDirection = Direction.NORTH;
    private int miningStep;
    private int miningTunnelStep;
    private int miningTargetY = Integer.MAX_VALUE;
    @Nullable private BlockPos lastTorchPosition;
    @Nullable private RikumiBuildProject buildProject;
    private boolean starterHouseCompleted;

    public RikumiMitaEntity(EntityType<? extends RikumiMitaEntity> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("Rikumi Mita"));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        RikumiAiLifecycle.bindAvatar(this);
        if (actionTicks > 0 && --actionTicks == 0) setActionState(RikumiAction.IDLE, 0);
        if (level() instanceof ServerLevel server) {
            boolean externallyControlled = RikumiAiLifecycle.externalAgentActive(server);
            if (!externallyControlled) tickLocalMobAi(server);
            if (RichCoreConfig.debugMode() && server.getGameTime() >= nextPathDebugTick) {
                nextPathDebugTick = server.getGameTime() + 4L;
                renderDebugPath(server);
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    private void tickLocalMobAi(ServerLevel level) {
        // A live mining session owns navigation and movement. The autonomous roaming planner must not
        // select another destination between mining-controller ticks or Rikumi oscillates beside the block.
        if (RikumiMiningController.hasActive(this)) {
            setOrderedToSit(false);
            setInSittingPose(false);
            if (!getNavigation().isDone() && getActionState() != RikumiAction.MINE)
                setActionState(RikumiAction.WALK, 8);
            return;
        }
        RikumiMode mode = getMode();
        setOrderedToSit(mode == RikumiMode.STAY);
        setInSittingPose(mode == RikumiMode.STAY);
        if (mode == RikumiMode.STAY) {
            getNavigation().stop();
            setCurrentGoal("Stay safely in place");
            setCurrentTask("Waiting for the owner");
            setActionState(RikumiAction.SIT, 10);
            return;
        }

        Player owner = getOwner() instanceof Player player ? player : null;
        switch (mode) {
            case FOLLOW -> {
                setCurrentGoal("Stay near " + getOwnerDisplayName());
                followOwnerSafely(owner, false);
            }
            case ASSIST -> {
                if (buildProject == null) setCurrentGoal("Assist " + getOwnerDisplayName() + " with useful work");
                followOwnerSafely(owner, true);
            }
            case AUTO -> tickAutonomousRoaming(level);
            case PATROL -> tickPatrol(level, owner);
            default -> { }
        }
        if (!getNavigation().isDone() && getActionState() == RikumiAction.IDLE) setActionState(RikumiAction.WALK, 8);
    }

    private void followOwnerSafely(@Nullable Player owner, boolean assisting) {
        if (owner == null || owner.level() != level()) {
            getNavigation().stop();
            setCurrentTask("Waiting for the owner to return");
            return;
        }
        double distance = distanceToSqr(owner);
        if (distance > 64.0D) {
            setCurrentTask(assisting ? "Catching up to assist" : "Following at a safe distance");
            getNavigation().moveTo(owner, 1.18D);
        } else if (distance > 25.0D) {
            setCurrentTask(assisting ? "Following while looking for tasks" : "Following safely");
            getNavigation().moveTo(owner, 1.02D);
        } else if (distance < 9.0D) {
            getNavigation().stop();
            Vec3 away = position().subtract(owner.position());
            if (away.lengthSqr() > 0.01D && distance < 4.0D) {
                Vec3 target = position().add(away.normalize().scale(2.0D));
                getNavigation().moveTo(target.x, target.y, target.z, 0.9D);
            }
            setCurrentTask(assisting ? "Ready to assist" : "Keeping a safe distance");
        } else {
            getNavigation().stop();
            getLookControl().setLookAt(owner, 20.0F, 20.0F);
            setCurrentTask(assisting ? "Watching for a useful task" : "Following nearby");
        }
    }

    private void tickAutonomousRoaming(ServerLevel level) {
        if (buildProject != null && !buildProject.complete()) {
            setCurrentGoal("Complete project: " + buildProject.displayName());
            return;
        }
        setCurrentGoal("Choose and complete useful independent tasks");
        if (autoAnchor == null) autoAnchor = blockPosition();
        if (level.getGameTime() < nextModeDecisionTick && !getNavigation().isDone()) return;
        nextModeDecisionTick = level.getGameTime() + 100L + random.nextInt(120);
        Vec3 destination = DefaultRandomPos.getPosTowards(this, 14, 6, Vec3.atCenterOf(autoAnchor), Math.PI / 2.0D);
        if (destination == null) destination = DefaultRandomPos.getPos(this, 12, 5);
        if (destination != null) {
            getNavigation().moveTo(destination.x, destination.y, destination.z, 0.92D);
            setCurrentTask("Exploring for useful work");
        } else setCurrentTask("Planning the next task");
    }

    private void tickPatrol(ServerLevel level, @Nullable Player owner) {
        Vec3 center = owner != null && owner.level() == level ? owner.position() : position();
        setCurrentGoal("Patrol and protect the area near " + getOwnerDisplayName());
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(18.0D, 8.0D, 18.0D), Monster::isAlive);
        hostiles.sort(Comparator.comparingDouble(this::distanceToSqr));
        if (!hostiles.isEmpty()) {
            Monster target = hostiles.get(0);
            setTarget(target);
            setCurrentTask("Protecting the area from " + target.getName().getString());
            if (distanceToSqr(target) <= 6.25D && hasLineOfSight(target)) {
                doHurtTarget(target);
                swing(InteractionHand.MAIN_HAND, true);
                setActionState(RikumiAction.ATTACK, 10);
            } else getNavigation().moveTo(target, 1.18D);
            return;
        }
        setTarget(null);
        if (level.getGameTime() >= nextModeDecisionTick || getNavigation().isDone()) {
            nextModeDecisionTick = level.getGameTime() + 80L + random.nextInt(80);
            BlockPos patrolCenter = BlockPos.containing(center);
            Vec3 destination = DefaultRandomPos.getPosTowards(this, 12, 5, Vec3.atCenterOf(patrolCenter), Math.PI / 2.0D);
            if (destination != null) getNavigation().moveTo(destination.x, destination.y, destination.z, 0.9D);
        }
        setCurrentTask("Patrolling for hostile mobs");
    }

    private void renderDebugPath(ServerLevel level) {
        Path path = getNavigation().getPath();
        if (path == null || path.isDone()) return;
        DustParticleOptions particle = new DustParticleOptions(new Vector3f(0.15F, 0.95F, 0.55F), 0.65F);
        Vec3 previous = position().add(0.0D, 0.35D, 0.0D);
        int start = Math.max(0, path.getNextNodeIndex());
        int end = Math.min(path.getNodeCount(), start + 32);
        for (int index = start; index < end; index++) {
            Node node = path.getNode(index);
            Vec3 next = new Vec3(node.x + 0.5D, node.y + 0.25D, node.z + 0.5D);
            double length = previous.distanceTo(next);
            int steps = Math.max(1, (int) Math.ceil(length * 4.0D));
            for (int step = 0; step <= steps; step++) {
                Vec3 point = previous.lerp(next, step / (double) steps);
                level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            previous = next;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OUTFIT, 0);
        builder.define(VOICE_ENABLED, true);
        builder.define(NAMEPLATE_ENABLED, true);
        builder.define(MODE, RikumiMode.FOLLOW.ordinal());
        builder.define(ACTION, RikumiAction.IDLE.ordinal());
        builder.define(CURRENT_TASK, "Following");
        builder.define(CURRENT_GOAL, "Follow Owner");
        builder.define(CURRENT_PROJECT, "None");
        builder.define(CURRENT_TASK_DETAIL, "Following nearby at a safe distance");
        builder.define(CURRENT_GOAL_DETAIL, "Stay near the owner");
        builder.define(CURRENT_PROJECT_DETAIL, "No active project");
        builder.define(HOME_POSITION, "");
    }

    public RikumiInventoryBridge getInventoryHandler() { return inventory; }

    /** Mirrors the survival actor's real equipment onto the visible companion for client rendering. */
    public void syncDisplayedHands(FakePlayer player) {
        ItemStack main = player == null ? ItemStack.EMPTY : player.getMainHandItem().copy();
        ItemStack off = player == null ? ItemStack.EMPTY : player.getOffhandItem().copy();
        if (!sameDisplayedStack(getMainHandItem(), main)) setItemSlot(EquipmentSlot.MAINHAND, main);
        if (!sameDisplayedStack(getOffhandItem(), off)) setItemSlot(EquipmentSlot.OFFHAND, off);
    }

    private static boolean sameDisplayedStack(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return left.isEmpty() && right.isEmpty();
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }
    public int getOutfitIndex() { return entityData.get(OUTFIT); }
    public void setOutfitIndex(int index) { entityData.set(OUTFIT, Math.floorMod(index, OutfitRegistry.OUTFITS.size())); }
    public boolean isVoiceEnabled() { return entityData.get(VOICE_ENABLED); }
    public void setVoiceEnabled(boolean value) { entityData.set(VOICE_ENABLED, value); }
    public boolean isNameplateEnabled() { return entityData.get(NAMEPLATE_ENABLED); }
    public void setNameplateEnabled(boolean value) { entityData.set(NAMEPLATE_ENABLED, value); setCustomNameVisible(value); }
    public RikumiMode getMode() { return RikumiMode.byOrdinal(entityData.get(MODE)); }
    public void setMode(RikumiMode mode) {
        RikumiMode next = mode == null ? RikumiMode.FOLLOW : mode;
        entityData.set(MODE, next.ordinal());
        setOrderedToSit(next == RikumiMode.STAY);
        setInSittingPose(next == RikumiMode.STAY);
        // A mode button is an immediate command: abandon the old path, target, use action, and mining crack state now.
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(Vec3.ZERO);
        actionTicks = 0;
        entityData.set(ACTION, (next == RikumiMode.STAY ? RikumiAction.SIT : RikumiAction.IDLE).ordinal());
        nextModeDecisionTick = 0L;
        if (level() instanceof ServerLevel server) RikumiMiningController.clear(server, this);
        if (next != RikumiMode.AUTO) autoAnchor = null;
        switch (next) {
            case STAY -> setTaskStatus("Waiting", "Staying at the current position");
            case FOLLOW -> setTaskStatus("Following", "Following the owner");
            case ASSIST -> setTaskStatus("Ready", "Ready to assist inside the home work area");
            case AUTO -> setTaskStatus("Ready", "Choosing the next autonomous work task");
            case PATROL -> setTaskStatus("Patrolling", "Watching the nearby area for threats");
        }
    }
    public RikumiAction getActionState() { return RikumiAction.byOrdinal(entityData.get(ACTION)); }
    public void setActionState(RikumiAction action, int ticks) {
        entityData.set(ACTION, (action == null ? RikumiAction.IDLE : action).ordinal());
        actionTicks = Math.max(actionTicks, Math.max(0, ticks));
    }
    public String getCurrentTask() { return entityData.get(CURRENT_TASK); }
    public String getCurrentTaskDetail() { return entityData.get(CURRENT_TASK_DETAIL); }
    public void setCurrentTask(String detail) { setTaskStatus(shortTask(detail), detail); }
    public void setTaskStatus(String descriptor, String detail) {
        entityData.set(CURRENT_TASK, safeDescriptor(descriptor, "Idle"));
        entityData.set(CURRENT_TASK_DETAIL, safeStatus(detail, descriptor == null ? "Idle" : descriptor));
    }
    public String getCurrentGoal() { return entityData.get(CURRENT_GOAL); }
    public String getCurrentGoalDetail() { return entityData.get(CURRENT_GOAL_DETAIL); }
    public void setCurrentGoal(String detail) { setGoalStatus(shortGoal(detail), detail); }
    public void setGoalStatus(String descriptor, String detail) {
        entityData.set(CURRENT_GOAL, safeDescriptor(descriptor, "Help Owner"));
        entityData.set(CURRENT_GOAL_DETAIL, safeStatus(detail, descriptor == null ? "Help the owner" : descriptor));
    }
    public String getCurrentProjectName() { return entityData.get(CURRENT_PROJECT); }
    public String getCurrentProjectDetail() { return entityData.get(CURRENT_PROJECT_DETAIL); }
    public Optional<RikumiBuildProject> getBuildProject() { return Optional.ofNullable(buildProject); }
    public boolean hasCompletedStarterHouse() { return starterHouseCompleted; }
    public void markStarterHouseCompleted() { starterHouseCompleted = true; }

    private void setProjectStatus(String name, String detail) {
        entityData.set(CURRENT_PROJECT, safeDescriptor(name, "None"));
        entityData.set(CURRENT_PROJECT_DETAIL, safeStatus(detail, "No active project"));
    }

    public boolean hasHome() { return !entityData.get(HOME_POSITION).isBlank(); }

    @Nullable
    public BlockPos getHomePosition() {
        String encoded = entityData.get(HOME_POSITION);
        if (encoded == null || encoded.isBlank()) return null;
        String[] parts = encoded.split(",", 3);
        if (parts.length != 3) return null;
        try { return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])); }
        catch (NumberFormatException ignored) { return null; }
    }

    public void setHomePosition(BlockPos position) {
        if (position == null) return;
        if (hasHome() && buildProject != null) cancelBuildProject();
        BlockPos safe = position.immutable();
        entityData.set(HOME_POSITION, safe.getX() + "," + safe.getY() + "," + safe.getZ());
        autoAnchor = safe;
        miningShaft = null;
        lastMinedBlock = null;
        miningStep = 0;
        miningTunnelStep = 0;
        miningTargetY = Integer.MAX_VALUE;
        lastTorchPosition = null;
        setGoalStatus("Home Set", "Work and building are anchored near " + safe.toShortString());
        setTaskStatus("Ready", "Home set at " + safe.toShortString());
        sendDialogueToOwner("I set my home at " + safe.toShortString() + ". I'll keep my projects and mine nearby.");
    }

    public boolean canWorkAt(BlockPos target) {
        BlockPos home = getHomePosition();
        if (home == null || target == null) return false;
        long dx = target.getX() - home.getX();
        long dz = target.getZ() - home.getZ();
        // Home constrains the horizontal project/mining district. A vertical mine directly below
        // home remains valid all the way to deepslate and diamond levels instead of being rejected
        // by a spherical distance check.
        return dx * dx + dz * dz <= (long) HOME_WORK_RADIUS * HOME_WORK_RADIUS
                && target.getY() >= level().getMinBuildHeight()
                && target.getY() < level().getMaxBuildHeight();
    }

    public boolean isWithinHomeWorkArea() { return canWorkAt(blockPosition()); }
    @Nullable public BlockPos getMiningShaft() { return miningShaft; }
    @Nullable public BlockPos getLastMinedBlock() { return lastMinedBlock; }
    public Direction getMiningDirection() { return miningDirection; }
    public int getMiningStep() { return miningStep; }
    public int getMiningTunnelStep() { return miningTunnelStep; }
    public int getMiningTargetY() { return miningTargetY; }
    @Nullable public BlockPos getLastTorchPosition() { return lastTorchPosition; }
    public void rememberMiningShaft(BlockPos shaft, Direction direction) {
        miningShaft = shaft == null ? null : shaft.immutable();
        miningDirection = direction == null || direction.getAxis().isVertical() ? Direction.NORTH : direction;
    }
    public void rememberMinedBlock(BlockPos pos) { if (pos != null) lastMinedBlock = pos.immutable(); }
    public void rememberTorch(BlockPos pos) { if (pos != null) lastTorchPosition = pos.immutable(); }
    public void setMiningStep(int step) { miningStep = Math.max(0, step); }
    public void setMiningTunnelStep(int step) { miningTunnelStep = Math.max(0, step); }
    public void setMiningTargetY(int y) { miningTargetY = y; }

    public void cycleMode() {
        setMode(getMode().next());
    }

    public boolean startBuildProject(ResourceLocation schematicId, BlockPos origin) {
        return startBuildProjectInternal(schematicId, origin, null, Direction.SOUTH);
    }

    /** Starts an orientation-aware project anchored to a reusable in-world placement marker. */
    public boolean startBuildProject(ResourceLocation schematicId, BlockPos markerPos, Direction facing) {
        Direction safeFacing = facing == null || facing.getAxis().isVertical() ? Direction.SOUTH : facing;
        BlockPos origin = markerPos.relative(safeFacing);
        return startBuildProjectInternal(schematicId, origin, markerPos, safeFacing);
    }

    private boolean startBuildProjectInternal(ResourceLocation schematicId, BlockPos origin,
                                              @Nullable BlockPos markerPos, Direction facing) {
        Optional<RikumiSchematic> schematic = RikumiSchematicRegistry.get(schematicId);
        if (schematic.isEmpty()) return false;
        if (!hasHome()) {
            setGoalStatus("Set Home", "Set a home before starting a building project");
            setTaskStatus("Waiting", "Use Set Home in Rikumi's GUI first");
            return false;
        }
        if (!canWorkAt(origin)) {
            setTaskStatus("Outside Home", "The selected build origin is outside the home work area");
            return false;
        }
        buildProject = new RikumiBuildProject(schematic.get(), origin, markerPos, facing, 0);
        setProjectStatus(schematic.get().displayName(), "Planning placement and materials for " + schematic.get().displayName());
        setMode(RikumiMode.AUTO);
        setGoalStatus("Build Project", "Build " + schematic.get().displayName());
        setTaskStatus("Planning", "Planning materials, placement orientation, and build order");
        return true;
    }

    public void cancelBuildProject() {
        if (buildProject != null) buildProject.cancel();
        buildProject = null;
        setProjectStatus("None", "No active project");
        setCurrentTask("No active build project");
    }

    public boolean tickBuildProject(ServerLevel level, net.neoforged.neoforge.common.util.FakePlayer player) {
        if (buildProject == null) return false;
        RikumiBuildProject active = buildProject;
        if (active.complete()) {
            buildProject = null;
            return false;
        }
        boolean worked = active.tick(level, player, this);
        if (active.complete()) {
            setProjectStatus("None", "Completed " + active.displayName());
            buildProject = null;
        } else {
            setProjectStatus(active.displayName(), active.progressDescription());
        }
        return worked;
    }


    private void restoreBuildProject(ResourceLocation schematicId, BlockPos origin, int completedPlacements,
                                     @Nullable BlockPos markerPos, Direction facing) {
        Optional<RikumiSchematic> schematic = RikumiSchematicRegistry.get(schematicId);
        if (schematic.isEmpty()) return;
        buildProject = new RikumiBuildProject(schematic.get(), origin, markerPos, facing, completedPlacements);
        setProjectStatus(schematic.get().displayName(), buildProject.progressDescription());
        setGoalStatus("Build Project", "Build " + schematic.get().displayName());
    }

    private static String safeStatus(String value, String fallback) {
        String safe = value == null || value.isBlank() ? fallback : value.strip();
        return safe.length() > 240 ? safe.substring(0, 240) : safe;
    }

    private static String safeDescriptor(String value, String fallback) {
        String safe = value == null || value.isBlank() ? fallback : value.strip();
        return safe.length() > 28 ? safe.substring(0, 27).stripTrailing() + "…" : safe;
    }

    private static String shortTask(String detail) {
        String value = safeStatus(detail, "Idle").toLowerCase(java.util.Locale.ROOT);
        if (value.contains("complete")) return "Complete";
        if (value.contains("need") || value.contains("missing")) return "Needs Materials";
        if (value.contains("walk") || value.contains("follow") || value.contains("catching up")) return "Traveling";
        if (value.contains("mine") || value.contains("mining") || value.contains("ore")) return "Mining";
        if (value.contains("craft")) return "Crafting";
        if (value.contains("cook")) return "Cooking";
        if (value.contains("fish")) return "Fishing";
        if (value.contains("place") || value.contains("build")) return "Building";
        if (value.contains("attack") || value.contains("defend") || value.contains("protect")) return "Combat";
        if (value.contains("collect") || value.contains("gather")) return "Collecting";
        if (value.contains("patrol")) return "Patrolling";
        if (value.contains("explor") || value.contains("planning")) return "Exploring";
        if (value.contains("wait") || value.contains("idle")) return "Waiting";
        if (value.contains("assist") || value.contains("useful task")) return "Assisting";
        return safeDescriptor(detail, "Idle");
    }

    private static String shortGoal(String detail) {
        String value = safeStatus(detail, "Help the owner").toLowerCase(java.util.Locale.ROOT);
        if (value.contains("diamond")) return "Reach Diamond";
        if (value.contains("starter house") || value.contains("build ") || value.contains("project")) return "Build Project";
        if (value.contains("mine") || value.contains("resource")) return "Gather Resources";
        if (value.contains("craft") || value.contains("prepare")) return "Craft Supplies";
        if (value.contains("patrol") || value.contains("protect")) return "Protect Area";
        if (value.contains("follow") || value.contains("stay near")) return "Follow Owner";
        if (value.contains("assist") || value.contains("help")) return "Assist Owner";
        if (value.contains("stay") || value.contains("safe")) return "Stay Safe";
        if (value.contains("choose") || value.contains("independent")) return "Progress";
        return safeDescriptor(detail, "Help Owner");
    }

    public boolean mayConfigure(Player player) {
        UUID ownerId = getOwnerUUID();
        return player.isCreative() || (ownerId != null && ownerId.equals(player.getUUID()));
    }

    public void assignOwner(UUID ownerId) { setOwnerUUID(ownerId); setTame(true, true); }

    public String getOwnerDisplayName() {
        ServerPlayer owner = onlineOwner();
        if (owner != null) {
            String profileName = owner.getGameProfile().getName();
            if (profileName != null && !profileName.isBlank()) return profileName;
        }
        return "Player";
    }

    @Nullable
    private ServerPlayer onlineOwner() {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(ownerId);
    }

    private Component playerStyleChat(Component message) { return Component.translatable("chat.type.text", getDisplayName(), message); }
    public void sendDialogueToOwner(String message) {
        ServerPlayer owner = onlineOwner();
        if (owner != null) owner.sendSystemMessage(playerStyleChat(Component.literal(message)));
    }
    public void greetOwnerFromPresent() { sendDialogueToOwner("Hi, " + getOwnerDisplayName() + "! I'm here and ready to help."); }
    public void cycleOutfit(int delta) {
        setOutfitIndex(getOutfitIndex() + delta);
        sendDialogueToOwner("What do you think of my " + OutfitRegistry.byIndex(getOutfitIndex()).label()
                + " outfit, " + getOwnerDisplayName() + "?");
    }
    /** Legacy button/action alias now cycles all five modes. */
    public void toggleSitFollow() { cycleMode(); }
    public void toggleVoiceWithDialogue() {
        setVoiceEnabled(!isVoiceEnabled());
        sendDialogueToOwner(isVoiceEnabled() ? "Voice is on again." : "I'll use chat only for now.");
    }
    public void toggleNameplateWithDialogue() {
        setNameplateEnabled(!isNameplateEnabled());
        sendDialogueToOwner(isNameplateEnabled() ? "My nameplate is visible." : "I've hidden my nameplate.");
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!mayConfigure(player)) {
            if (!level().isClientSide()) playSound(SoundEvents.VILLAGER_NO, 0.65F, 1.05F);
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide()) {
                cycleMode();
                playSound(SoundEvents.WOOL_PLACE, 0.45F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, buffer -> buffer.writeVarInt(getId()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override public Component getDisplayName() { return Component.literal("Rikumi Mita"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return mayConfigure(player) ? new RikumiMitaMenu(id, inv, this) : null;
    }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob parent) { return null; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        HolderLookup.Provider registries = level().registryAccess();
        tag.put("RikumiInventory", inventory.serializeNBT(registries));
        tag.putInt("Outfit", getOutfitIndex());
        tag.putBoolean("VoiceEnabled", isVoiceEnabled());
        tag.putBoolean("NameplateEnabled", isNameplateEnabled());
        tag.putString("Mode", getMode().name());
        tag.putString("CurrentTask", getCurrentTask());
        tag.putString("CurrentTaskDetail", getCurrentTaskDetail());
        tag.putString("CurrentGoal", getCurrentGoal());
        tag.putString("CurrentGoalDetail", getCurrentGoalDetail());
        tag.putString("CurrentProjectDetail", getCurrentProjectDetail());
        if (autoAnchor != null) tag.putLong("AutoAnchor", autoAnchor.asLong());
        BlockPos home = getHomePosition();
        if (home != null) tag.putLong("HomePosition", home.asLong());
        if (miningShaft != null) tag.putLong("MiningShaft", miningShaft.asLong());
        if (lastMinedBlock != null) tag.putLong("LastMinedBlock", lastMinedBlock.asLong());
        tag.putString("MiningDirection", miningDirection.getSerializedName());
        tag.putInt("MiningStep", miningStep);
        tag.putInt("MiningTunnelStep", miningTunnelStep);
        tag.putInt("MiningTargetY", miningTargetY);
        if (lastTorchPosition != null) tag.putLong("LastTorchPosition", lastTorchPosition.asLong());
        tag.putBoolean("StarterHouseCompleted", starterHouseCompleted);
        if (buildProject != null && !buildProject.complete()) {
            tag.putString("BuildSchematic", buildProject.schematicId().toString());
            tag.putLong("BuildOrigin", buildProject.origin().asLong());
            if (buildProject.markerPos() != null) tag.putLong("BuildMarker", buildProject.markerPos().asLong());
            tag.putString("BuildFacing", buildProject.facing().getSerializedName());
            tag.putInt("BuildProgress", buildProject.placed());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        HolderLookup.Provider registries = level().registryAccess();
        if (tag.contains("RikumiInventory")) inventory.deserializeNBT(registries, tag.getCompound("RikumiInventory"));
        setOutfitIndex(tag.getInt("Outfit"));
        setVoiceEnabled(!tag.contains("VoiceEnabled") || tag.getBoolean("VoiceEnabled"));
        setNameplateEnabled(!tag.contains("NameplateEnabled") || tag.getBoolean("NameplateEnabled"));
        setMode(tag.contains("Mode") ? RikumiMode.parse(tag.getString("Mode")) : RikumiMode.FOLLOW);
        if (tag.contains("CurrentTaskDetail")) setCurrentTask(tag.getString("CurrentTaskDetail"));
        else if (tag.contains("CurrentTask")) setCurrentTask(tag.getString("CurrentTask"));
        if (tag.contains("CurrentGoalDetail")) setCurrentGoal(tag.getString("CurrentGoalDetail"));
        else if (tag.contains("CurrentGoal")) setCurrentGoal(tag.getString("CurrentGoal"));
        if (tag.contains("CurrentProjectDetail")) entityData.set(CURRENT_PROJECT_DETAIL, safeStatus(tag.getString("CurrentProjectDetail"), "No active project"));
        if (tag.contains("AutoAnchor")) autoAnchor = BlockPos.of(tag.getLong("AutoAnchor"));
        if (tag.contains("HomePosition")) {
            BlockPos home = BlockPos.of(tag.getLong("HomePosition"));
            entityData.set(HOME_POSITION, home.getX() + "," + home.getY() + "," + home.getZ());
        }
        if (tag.contains("MiningShaft")) miningShaft = BlockPos.of(tag.getLong("MiningShaft"));
        if (tag.contains("LastMinedBlock")) lastMinedBlock = BlockPos.of(tag.getLong("LastMinedBlock"));
        Direction storedMiningDirection = tag.contains("MiningDirection") ? Direction.byName(tag.getString("MiningDirection")) : null;
        miningDirection = storedMiningDirection == null || storedMiningDirection.getAxis().isVertical() ? Direction.NORTH : storedMiningDirection;
        miningStep = Math.max(0, tag.getInt("MiningStep"));
        miningTunnelStep = Math.max(0, tag.getInt("MiningTunnelStep"));
        miningTargetY = tag.contains("MiningTargetY") ? tag.getInt("MiningTargetY") : Integer.MAX_VALUE;
        lastTorchPosition = tag.contains("LastTorchPosition") ? BlockPos.of(tag.getLong("LastTorchPosition")) : null;
        starterHouseCompleted = tag.getBoolean("StarterHouseCompleted");
        if (tag.contains("BuildSchematic") && tag.contains("BuildOrigin")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("BuildSchematic"));
            if (id != null) {
                BlockPos marker = tag.contains("BuildMarker") ? BlockPos.of(tag.getLong("BuildMarker")) : null;
                Direction facing = tag.contains("BuildFacing") ? Direction.byName(tag.getString("BuildFacing")) : Direction.SOUTH;
                restoreBuildProject(id, BlockPos.of(tag.getLong("BuildOrigin")), tag.getInt("BuildProgress"), marker,
                        facing == null ? Direction.SOUTH : facing);
            }
        }
    }
}
