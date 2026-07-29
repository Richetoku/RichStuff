package com.richetoku.richstuff;

import com.richetoku.richcore.MaterialDef;
import com.richetoku.richcore.RichStuffCatalog;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
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

public class RichStuffMetalSlime extends Slime {
    private static final int READY_TICKS = 20 * 20;
    private static final int BREEDING_COOLDOWN_TICKS = 20 * 30;
    private int specialCooldown;
    private int breedReadyTicks;
    private int breedingCooldown;

    public static AttributeSupplier.Builder createAttributes(int tier) {
        int safeTier = Math.max(1, Math.min(7, tier));
        double health = safeTier == 7 ? 500.0D : 12.0D + safeTier * safeTier * 8.0D;
        double attack = safeTier == 7 ? 30.0D : 2.0D + safeTier * 2.5D;
        double armor = safeTier >= 6 ? 20.0D : safeTier * 2.0D;
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, 0.18D + safeTier * 0.014D)
                .add(Attributes.ATTACK_DAMAGE, attack)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, safeTier == 7 ? 1.0D : safeTier * 0.08D)
                .add(Attributes.FOLLOW_RANGE, 20.0D + safeTier * 4.0D);
    }

    public RichStuffMetalSlime(EntityType<? extends Slime> type, Level level) { super(type, level); }
    public RichStuffSlimeCatalog.MetalSlimeDef profile() { return RichStuff.profileForSlime((EntityType<?>) getType()); }
    public String materialId() { RichStuffSlimeCatalog.MetalSlimeDef d = profile(); return d == null ? "unknown" : d.material(); }
    public int tier() { RichStuffSlimeCatalog.MetalSlimeDef d = profile(); return d == null ? 1 : d.tier(); }
    public int materialColor() { RichStuffSlimeCatalog.MetalSlimeDef d = profile(); return d == null ? 0x66CC66 : (d.red() << 16) | (d.green() << 8) | d.blue(); }
    public ItemStack nuggetStack() {
        RichStuffSlimeCatalog.MetalSlimeDef d = profile();
        if (d == null || RichStuff.item(d.nuggetId()) == null) return ItemStack.EMPTY;
        return new ItemStack(RichStuff.item(d.nuggetId()).get());
    }

    public boolean canAcceptSlimeTreat() { return isAlive() && breedingCooldown <= 0; }
    public void markReadyToBreed() { breedReadyTicks = READY_TICKS; }
    public boolean readyToBreed() { return breedReadyTicks > 0 && breedingCooldown <= 0 && isAlive(); }
    private void clearBreedState() { breedReadyTicks = 0; breedingCooldown = BREEDING_COOLDOWN_TICKS; }
    public void playSlimeSquishSound() { level().playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.7F, 1.0F); }

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
        child.moveTo((getX() + mate.getX()) * 0.5D, Math.max(getY(), mate.getY()) + 0.15D, (getZ() + mate.getZ()) * 0.5D,
                server.random.nextFloat() * 360.0F, 0.0F);
        child.setSize(1, true);
        child.setHealth(child.getMaxHealth());
        child.breedingCooldown = BREEDING_COOLDOWN_TICKS / 2;
        server.addFreshEntity(child);
        clearBreedState();
        mate.clearBreedState();
        server.sendParticles(ParticleTypes.HEART, child.getX(), child.getY(0.6D), child.getZ(), 12, 0.45D, 0.25D, 0.45D, 0.02D);
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

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("RichStuffMetal", materialId());
        tag.putInt("SpecialCooldown", specialCooldown);
        tag.putInt("BreedReadyTicks", breedReadyTicks);
        tag.putInt("BreedingCooldown", breedingCooldown);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        specialCooldown = tag.getInt("SpecialCooldown");
        breedReadyTicks = tag.getInt("BreedReadyTicks");
        breedingCooldown = tag.getInt("BreedingCooldown");
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        int size = tier() >= 7 ? 4 : tier() >= 5 ? 3 : 1 + random.nextInt(2);
        setSize(size, true);
        setHealth(getMaxHealth());
        return data;
    }

    @Override public void aiStep() {
        super.aiStep();
        if (specialCooldown > 0) specialCooldown--;
        if (breedReadyTicks > 0) breedReadyTicks--;
        if (breedingCooldown > 0) breedingCooldown--;
        if (tier() < 5 || level().isClientSide || specialCooldown > 0 || getTarget() == null || distanceToSqr(getTarget()) > 100.0D) return;
        specialCooldown = tier() >= 7 ? 70 : 120;
        if (level() instanceof ServerLevel server) {
            double radius = tier() >= 7 ? 7.0D : 4.0D;
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius))) {
                double dx = player.getX() - getX(), dz = player.getZ() - getZ();
                double length = Math.max(0.25D, Math.sqrt(dx * dx + dz * dz));
                player.hurt(damageSources().mobAttack(this), (float) (4.0D + tier() * 2.0D));
                player.push(dx / length * (0.75D + tier() * 0.08D), 0.45D + tier() * 0.04D, dz / length * (0.75D + tier() * 0.08D));
            }
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5D, getZ(), 50 + tier() * 10, radius * 0.55D, 0.8D, radius * 0.55D, 0.12D);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                    getX(), getY() + 0.15D, getZ(), 35, radius * 0.45D, 0.2D, radius * 0.45D, 0.05D);
            server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.65F);
        }
    }

    public static boolean checkMetalSlimeSpawnRules(EntityType<? extends RichStuffMetalSlime> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!RichStuffConfig.ENABLE_METAL_SLIMES.get() || level.getDifficulty() == Difficulty.PEACEFUL) return false;
        RichStuffSlimeCatalog.MetalSlimeDef def = RichStuff.profileForSlime((EntityType<?>) type);
        if (def == null) return false;
        if (pos.getY() < def.minY() || pos.getY() > def.maxY()) return false;
        float chance = (float) Math.min(1.0D, def.spawnChance() * RichStuffConfig.METAL_SLIME_SPAWN_MULTIPLIER.get());
        if (random.nextFloat() > chance) return false;
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
