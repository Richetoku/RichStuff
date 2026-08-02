package com.richetoku.richstuff;

import com.richetoku.richcore.MaterialDef;
import com.richetoku.richcore.RichStuffCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A largest-size slime whose body, combat ability, spawn profile, core and drops are driven by one
 * Rich Stuff metal/alloy definition. Tier one matches the largest ordinary vanilla slime and each
 * tier grows smoothly until tier seven reaches exactly twice that size. Tier-seven instances are
 * boss forms with no vanilla death fragmentation and five smaller fragments at every 10% threshold.
 */
public class RichStuffMetalSlime extends Slime {
    private static final int NORMAL_SIZE = 4;
    /** Tier seven is visually/collision-equivalent to vanilla slime size eight (2x size four). */
    private static final int BOSS_SIZE = 8;
    private static final int BOSS_FRAGMENT_SIZE = 2;
    private static final int BOSS_THRESHOLD_COUNT = 10;
    private static final int BOSS_FRAGMENTS_PER_THRESHOLD = 5;
    private static final int READY_TICKS = 20 * 20;
    private static final int BREEDING_COOLDOWN_TICKS = 20 * 30;

    private int specialCooldown;
    private int materialAbilityCooldown;
    private int breedReadyTicks;
    private int breedingCooldown;
    private int bossThresholdsTriggered;
    private boolean bossFragment;
    private final ServerBossEvent bossEvent;

    public static AttributeSupplier.Builder createAttributes(int tier) {
        int safeTier = Math.max(1, Math.min(7, tier));
        double health = safeTier == 7 ? 650.0D : 32.0D + safeTier * safeTier * 10.0D;
        double attack = safeTier == 7 ? 34.0D : 5.0D + safeTier * 2.75D;
        double armor = safeTier == 7 ? 24.0D : safeTier * 2.0D;
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, 0.19D + safeTier * 0.012D)
                .add(Attributes.ATTACK_DAMAGE, attack)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, safeTier == 7 ? 1.0D : safeTier * 0.08D)
                .add(Attributes.FOLLOW_RANGE, 24.0D + safeTier * 4.0D);
    }

    public RichStuffMetalSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS);
        bossEvent.setVisible(false);
    }

    public static float visualScaleForTier(int tier) {
        int safeTier = Math.max(1, Math.min(7, tier));
        float bossMultiplier = BOSS_SIZE / (float) NORMAL_SIZE;
        return 1.0F + (safeTier - 1) * (bossMultiplier - 1.0F) / 6.0F;
    }

    public float tierVisualScale() {
        return visualScaleForTier(tier());
    }

    /**
     * Base collision dimensions for this slime. LivingEntity#getDimensions(Pose) is final in
     * Minecraft 1.21.1, so custom living entities must override getDefaultDimensions(Pose).
     * Returning the final tier-sized base here avoids Slime's normal size multiplier being applied
     * a second time. LivingEntity can still apply the generic SCALE attribute afterward.
     */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float slimeSize = bossFragment ? BOSS_FRAGMENT_SIZE : NORMAL_SIZE * tierVisualScale();
        float side = 2.04F * 0.255F * slimeSize;
        return EntityDimensions.scalable(side, side);
    }

    /** Scale for the vanilla slime mesh; fragments intentionally ignore the parent tier multiplier. */
    public float shellRenderScale() {
        return bossFragment ? BOSS_FRAGMENT_SIZE : NORMAL_SIZE * tierVisualScale();
    }

    public RichStuffSlimeCatalog.MetalSlimeDef profile() {
        return RichStuff.profileForSlime((EntityType<?>) getType());
    }

    public String materialId() {
        RichStuffSlimeCatalog.MetalSlimeDef definition = profile();
        return definition == null ? "unknown" : definition.material();
    }

    public int tier() {
        RichStuffSlimeCatalog.MetalSlimeDef definition = profile();
        return definition == null ? 1 : definition.tier();
    }

    public int materialColor() {
        RichStuffSlimeCatalog.MetalSlimeDef definition = profile();
        return definition == null ? 0x66CC66 : (definition.red() << 16) | (definition.green() << 8) | definition.blue();
    }

    public ItemStack nuggetStack() {
        RichStuffSlimeCatalog.MetalSlimeDef definition = profile();
        if (definition == null || RichStuff.item(definition.nuggetId()) == null) return ItemStack.EMPTY;
        return new ItemStack(RichStuff.item(definition.nuggetId()).get());
    }

    public boolean isBossForm() {
        return tier() == 7 && !bossFragment;
    }

    private void applyCombatStats(boolean resetHealth) {
        int safeTier = Math.max(1, Math.min(7, tier()));
        double maxHealth = bossFragment ? 48.0D : isBossForm() ? 650.0D : 32.0D + safeTier * safeTier * 10.0D;
        double attack = bossFragment ? 10.0D : isBossForm() ? 34.0D : 5.0D + safeTier * 2.75D;
        double armor = bossFragment ? 8.0D : isBossForm() ? 24.0D : safeTier * 2.0D;
        double speed = bossFragment ? 0.25D : 0.19D + safeTier * 0.012D;
        var maxHealthAttribute = getAttribute(Attributes.MAX_HEALTH);
        var attackAttribute = getAttribute(Attributes.ATTACK_DAMAGE);
        var armorAttribute = getAttribute(Attributes.ARMOR);
        var speedAttribute = getAttribute(Attributes.MOVEMENT_SPEED);
        var knockbackAttribute = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (maxHealthAttribute != null) maxHealthAttribute.setBaseValue(maxHealth);
        if (attackAttribute != null) attackAttribute.setBaseValue(attack);
        if (armorAttribute != null) armorAttribute.setBaseValue(armor);
        if (speedAttribute != null) speedAttribute.setBaseValue(speed);
        if (knockbackAttribute != null) knockbackAttribute.setBaseValue(isBossForm() ? 1.0D : bossFragment ? 0.35D : safeTier * 0.08D);
        refreshDimensions();
        if (resetHealth) setHealth(getMaxHealth());
        else if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    public boolean canAcceptSlimeTreat() {
        return isAlive() && breedingCooldown <= 0;
    }

    public void markReadyToBreed() {
        breedReadyTicks = READY_TICKS;
    }

    public boolean readyToBreed() {
        return breedReadyTicks > 0 && breedingCooldown <= 0 && isAlive();
    }

    private void clearBreedState() {
        breedReadyTicks = 0;
        breedingCooldown = BREEDING_COOLDOWN_TICKS;
    }

    public void playSlimeSquishSound() {
        level().playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.7F, 1.0F);
    }

    @Nullable
    public RichStuffMetalSlime findNearbyReadyMate(double radius) {
        for (RichStuffMetalSlime candidate : level().getEntitiesOfClass(RichStuffMetalSlime.class, getBoundingBox().inflate(radius))) {
            if (candidate == this || !candidate.readyToBreed()) continue;
            return candidate;
        }
        return null;
    }

    public boolean tryBreedWith(ServerLevel server, RichStuffMetalSlime mate) {
        EntityType<? extends RichStuffMetalSlime> childType = childTypeWith(mate);
        if (childType == null) return false;
        RichStuffMetalSlime child = childType.create(server);
        if (child == null) return false;
        child.moveTo((getX() + mate.getX()) * 0.5D, Math.max(getY(), mate.getY()) + 0.15D,
                (getZ() + mate.getZ()) * 0.5D, server.random.nextFloat() * 360.0F, 0.0F);
        child.setSize(NORMAL_SIZE, false);
        child.applyCombatStats(true);
        child.breedingCooldown = BREEDING_COOLDOWN_TICKS / 2;
        server.addFreshEntity(child);
        clearBreedState();
        mate.clearBreedState();
        server.sendParticles(ParticleTypes.HEART, child.getX(), child.getY(0.6D), child.getZ(),
                12, 0.45D, 0.25D, 0.45D, 0.02D);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private EntityType<? extends RichStuffMetalSlime> childTypeWith(RichStuffMetalSlime mate) {
        String first = materialId();
        String second = mate.materialId();
        if (first.equals(second)) {
            DeferredHolder<EntityType<?>, EntityType<RichStuffMetalSlime>> holder = RichStuff.METAL_SLIMES.get(first);
            return holder == null ? null : holder.get();
        }
        for (MaterialDef material : RichStuffCatalog.MATERIALS) {
            if (!material.kind().equals("alloy")) continue;
            boolean matches = (first.equals(material.parent1()) && second.equals(material.parent2()))
                    || (first.equals(material.parent2()) && second.equals(material.parent1()));
            if (!matches) continue;
            DeferredHolder<EntityType<?>, EntityType<RichStuffMetalSlime>> holder = RichStuff.METAL_SLIMES.get(material.name());
            if (holder != null) return holder.get();
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("RichStuffMetal", materialId());
        tag.putInt("SpecialCooldown", specialCooldown);
        tag.putInt("MaterialAbilityCooldown", materialAbilityCooldown);
        tag.putInt("BreedReadyTicks", breedReadyTicks);
        tag.putInt("BreedingCooldown", breedingCooldown);
        tag.putInt("BossThresholdsTriggered", bossThresholdsTriggered);
        tag.putBoolean("BossFragment", bossFragment);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        specialCooldown = tag.getInt("SpecialCooldown");
        materialAbilityCooldown = tag.getInt("MaterialAbilityCooldown");
        breedReadyTicks = tag.getInt("BreedReadyTicks");
        breedingCooldown = tag.getInt("BreedingCooldown");
        bossThresholdsTriggered = tag.getInt("BossThresholdsTriggered");
        bossFragment = tag.getBoolean("BossFragment");
        applyCombatStats(false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        bossFragment = false;
        bossThresholdsTriggered = 0;
        setSize(NORMAL_SIZE, false);
        applyCombatStats(true);
        xpReward = tier() == 7 ? 100 : 5 + tier() * 3;
        return data;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (isBossForm()) bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    private void updateBossBar() {
        boolean visible = isBossForm() && isAlive();
        bossEvent.setVisible(visible);
        if (visible) {
            bossEvent.setName(getDisplayName());
            bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / Math.max(1.0F, getMaxHealth()))));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt || !isBossForm() || level().isClientSide()) return hurt;
        if (level() instanceof ServerLevel server) spawnCrossedBossThresholds(server);
        return hurt;
    }

    private void spawnCrossedBossThresholds(ServerLevel server) {
        float ratio = Math.max(0.0F, getHealth() / Math.max(1.0F, getMaxHealth()));
        while (bossThresholdsTriggered < BOSS_THRESHOLD_COUNT) {
            float nextThreshold = 0.9F - bossThresholdsTriggered * 0.1F;
            if (ratio > nextThreshold + 0.0001F) break;
            bossThresholdsTriggered++;
            spawnBossFragments(server);
        }
    }

    private void spawnBossFragments(ServerLevel server) {
        for (int index = 0; index < BOSS_FRAGMENTS_PER_THRESHOLD; index++) {
            EntityType<? extends Slime> type = getType();
            Slime created = type.create(server);
            if (!(created instanceof RichStuffMetalSlime fragment)) continue;
            double angle = (Math.PI * 2.0D * index / BOSS_FRAGMENTS_PER_THRESHOLD) + random.nextDouble() * 0.25D;
            double distance = 0.7D + random.nextDouble() * 0.5D;
            fragment.bossFragment = true;
            fragment.bossThresholdsTriggered = BOSS_THRESHOLD_COUNT;
            fragment.setSize(BOSS_FRAGMENT_SIZE, false);
            fragment.applyCombatStats(true);
            fragment.moveTo(getX() + Math.cos(angle) * distance, getY() + 0.35D,
                    getZ() + Math.sin(angle) * distance, random.nextFloat() * 360.0F, 0.0F);
            fragment.setDeltaMovement(Math.cos(angle) * 0.38D, 0.48D, Math.sin(angle) * 0.38D);
            server.addFreshEntity(fragment);
        }
        sendColoredParticles(server, 55, 1.2D, 0.12D);
        server.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.4F, 0.55F);
    }

    /** Prevents every tier-seven body, including threshold fragments, from recursively fragmenting on death. */
    @Override
    public void remove(RemovalReason reason) {
        if (tier() == 7 && reason == RemovalReason.KILLED && getSize() > 1) {
            setSize(1, false);
        }
        super.remove(reason);
    }

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        if (level().isClientSide() || materialAbilityCooldown > 0 || !isDealsDamage()) return;
        materialAbilityCooldown = Math.max(24, 70 - tier() * 6);
        applyMaterialAbility(player, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) updateBossBar();
        if (specialCooldown > 0) specialCooldown--;
        if (materialAbilityCooldown > 0) materialAbilityCooldown--;
        if (breedReadyTicks > 0) breedReadyTicks--;
        if (breedingCooldown > 0) breedingCooldown--;
        if (level().isClientSide() || specialCooldown > 0 || getTarget() == null) return;

        int requiredTier = isBossForm() ? 1 : 4;
        if (tier() < requiredTier || distanceToSqr(getTarget()) > (isBossForm() ? 196.0D : 100.0D)) return;
        specialCooldown = isBossForm() ? 55 : 110;
        if (level() instanceof ServerLevel server) {
            double radius = isBossForm() ? 8.0D : 4.5D;
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius))) {
                applyMaterialAbility(player, true);
            }
            sendColoredParticles(server, 45 + tier() * 8, radius * 0.55D, 0.08D);
            server.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE,
                    isBossForm() ? 1.6F : 1.0F, isBossForm() ? 0.45F : 0.75F);
        }
    }

    /** Material-keyed contact/burst effects. Every registered metal/alloy resolves to a profile. */
    private void applyMaterialAbility(Player player, boolean burst) {
        int duration = burst ? 100 : 60;
        int strength = Math.max(0, Math.min(3, tier() / 2));
        switch (materialId()) {
            case "aluminum" -> effect(player, MobEffects.LEVITATION, duration / 2, 0);
            case "andesite_alloy" -> { effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 1); pushAway(player, 0.75D); }
            case "azure_electrum" -> { effect(player, MobEffects.DARKNESS, duration, 0); damage(player, 3.0F + tier()); }
            case "azure_silver" -> { effect(player, MobEffects.GLOWING, duration * 2, 0); effect(player, MobEffects.WEAKNESS, duration, 1); }
            case "blaze_gold" -> { player.igniteForSeconds(4 + tier()); damage(player, 2.0F + tier() * 0.5F); }
            case "brass" -> { effect(player, MobEffects.CONFUSION, duration, 0); pushAway(player, 1.0D); }
            case "bronze" -> { effect(player, MobEffects.DIG_SLOWDOWN, duration, 1); damage(player, 2.5F); }
            case "cast_iron" -> { effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 2); pushAway(player, 1.25D); }
            case "cobalt" -> { player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() * 2, player.getTicksFrozen() + 120)); effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 0); }
            case "constantan" -> { effect(player, MobEffects.WEAKNESS, duration, 1); effect(player, MobEffects.DIG_SLOWDOWN, duration, 0); }
            case "copper" -> { effect(player, MobEffects.GLOWING, duration, 0); damage(player, 3.5F); }
            case "copronickel" -> { effect(player, MobEffects.CONFUSION, duration, 1); effect(player, MobEffects.WEAKNESS, duration, 0); }
            case "crimson_iron" -> { player.igniteForSeconds(3 + tier()); effect(player, MobEffects.WEAKNESS, duration, 0); }
            case "crimson_steel" -> { player.igniteForSeconds(5 + tier()); effect(player, MobEffects.WITHER, duration / 2, 0); }
            case "dark_iron" -> { effect(player, MobEffects.DARKNESS, duration * 2, 0); effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 1); }
            case "eclipse_alloy" -> { effect(player, MobEffects.DARKNESS, duration * 2, 1); effect(player, MobEffects.WITHER, duration, 1); pushAway(player, 1.5D); }
            case "electrum" -> { effect(player, MobEffects.GLOWING, duration, 0); damage(player, 4.0F); pushAway(player, 0.55D); }
            case "enderium" -> { teleportAround(player); effect(player, MobEffects.CONFUSION, duration, 0); }
            case "ferricore" -> { pullToward(player, 0.9D); effect(player, MobEffects.LEVITATION, duration / 3, 0); }
            case "gold" -> { effect(player, MobEffects.GLOWING, duration * 2, 0); effect(player, MobEffects.WEAKNESS, duration / 2, 0); }
            case "invar" -> { effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 1); effect(player, MobEffects.DIG_SLOWDOWN, duration, 1); }
            case "iridium" -> { damage(player, 4.0F + strength); heal(Math.max(1.0F, tier() * 0.75F)); }
            case "iron" -> { pushAway(player, 1.0D); effect(player, MobEffects.WEAKNESS, duration, 0); }
            case "lead" -> { effect(player, MobEffects.POISON, duration, 0); effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 2); }
            case "lithium" -> { effect(player, MobEffects.CONFUSION, duration, 1); effect(player, MobEffects.MOVEMENT_SPEED, duration / 2, 1); }
            case "lumium" -> { effect(player, MobEffects.BLINDNESS, duration / 2, 0); effect(player, MobEffects.GLOWING, duration * 2, 0); }
            case "netherite" -> { player.igniteForSeconds(7); damage(player, 5.0F + tier()); pushAway(player, 1.2D); }
            case "nickel" -> { effect(player, MobEffects.HUNGER, duration * 2, 1); effect(player, MobEffects.WEAKNESS, duration, 0); }
            case "osmium" -> { effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 2); effect(player, MobEffects.DIG_SLOWDOWN, duration, 2); }
            case "pig_iron" -> { effect(player, MobEffects.HUNGER, duration * 2, 2); damage(player, 3.0F); }
            case "platinum" -> { effect(player, MobEffects.LEVITATION, duration / 2, 0); effect(player, MobEffects.GLOWING, duration * 2, 0); }
            case "reinforced_copper" -> { damage(player, 4.5F); pushAway(player, 1.15D); effect(player, MobEffects.GLOWING, duration, 0); }
            case "signalum" -> { damage(player, 3.5F + strength); effect(player, MobEffects.GLOWING, duration * 2, 0); effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration / 2, 0); }
            case "silver" -> { effect(player, MobEffects.WEAKNESS, duration, 1); effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration / 2, 0); }
            case "steel" -> { damage(player, 4.0F); pushAway(player, 1.35D); effect(player, MobEffects.DIG_SLOWDOWN, duration, 0); }
            case "tin" -> { effect(player, MobEffects.WEAKNESS, duration, 1); pushAway(player, 0.55D); }
            case "tyrian_steel" -> { effect(player, MobEffects.WITHER, duration, 1); effect(player, MobEffects.DARKNESS, duration * 2, 1); teleportAround(player); }
            case "uranium" -> { effect(player, MobEffects.POISON, duration * 2, 1); effect(player, MobEffects.GLOWING, duration * 3, 0); }
            case "vanadium" -> { effect(player, MobEffects.DIG_SLOWDOWN, duration, 2); effect(player, MobEffects.WEAKNESS, duration, 1); }
            case "wrought_iron" -> { pushAway(player, 1.4D); effect(player, MobEffects.MOVEMENT_SLOWDOWN, duration / 2, 1); }
            case "zinc" -> { effect(player, MobEffects.CONFUSION, duration, 0); effect(player, MobEffects.HUNGER, duration, 0); }
            default -> { damage(player, 2.0F + tier()); pushAway(player, 0.6D); }
        }
    }

    private static void effect(Player player, Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, Math.max(1, ticks), Math.max(0, amplifier)));
    }

    private void damage(Player player, float amount) {
        player.hurt(damageSources().mobAttack(this), amount);
    }

    private void pushAway(Player player, double force) {
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        double length = Math.max(0.2D, Math.sqrt(dx * dx + dz * dz));
        player.push(dx / length * force, 0.25D + force * 0.2D, dz / length * force);
    }

    private void pullToward(Player player, double force) {
        double dx = getX() - player.getX();
        double dz = getZ() - player.getZ();
        double length = Math.max(0.2D, Math.sqrt(dx * dx + dz * dz));
        player.push(dx / length * force, 0.12D, dz / length * force);
    }

    private void teleportAround(Player player) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 2.5D + random.nextDouble() * 2.0D;
        teleportTo(player.getX() + Math.cos(angle) * distance, player.getY() + 0.2D,
                player.getZ() + Math.sin(angle) * distance);
    }

    /** Replaces vanilla white slime splashes with the material/alloy color. */
    @Override
    protected boolean spawnCustomParticles() {
        int color = materialColor();
        Vector3f rgb = new Vector3f(((color >> 16) & 255) / 255.0F,
                ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F);
        DustParticleOptions dust = new DustParticleOptions(rgb, isBossForm() ? 1.65F : 1.05F);
        int count = Math.max(16, getSize() * 8);
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = (0.2D + random.nextDouble() * 0.8D) * Math.max(0.6D, getBbWidth() * 0.48D);
            level().addParticle(dust, getX() + Math.cos(angle) * radius, getY() + 0.12D,
                    getZ() + Math.sin(angle) * radius, Math.cos(angle) * 0.025D,
                    0.02D + random.nextDouble() * 0.04D, Math.sin(angle) * 0.025D);
        }
        return true;
    }

    private void sendColoredParticles(ServerLevel server, int count, double spread, double speed) {
        int color = materialColor();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255.0F,
                ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F), isBossForm() ? 1.7F : 1.1F);
        server.sendParticles(dust, getX(), getY() + getBbHeight() * 0.45D, getZ(), count,
                spread, Math.max(0.3D, getBbHeight() * 0.3D), spread, speed);
    }

    public static boolean checkMetalSlimeSpawnRules(EntityType<? extends RichStuffMetalSlime> type,
                                                     ServerLevelAccessor level, MobSpawnType reason,
                                                     BlockPos pos, RandomSource random) {
        if (!RichStuffConfig.ENABLE_METAL_SLIMES.get() || level.getDifficulty() == Difficulty.PEACEFUL) return false;
        RichStuffSlimeCatalog.MetalSlimeDef definition = RichStuff.profileForSlime((EntityType<?>) type);
        if (definition == null || !RichStuffMaterialDefinitions.isMaterialEnabled(definition.material())
                || pos.getY() < definition.minY() || pos.getY() > definition.maxY()) return false;
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
