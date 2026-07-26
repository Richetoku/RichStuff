package com.richetoku.richstuff;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class RichStuffMetalSlime extends Slime {
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.20D)
            .add(Attributes.ATTACK_DAMAGE, 4.0D)
            .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public RichStuffMetalSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    public String materialId() {
        return RichStuff.materialForSlime((EntityType<?>) this.getType());
    }

    public int materialColor() {
        RichStuffSlimeCatalog.MetalSlimeDef def = RichStuff.profileForSlime((EntityType<?>) this.getType());
        if (def == null) return 0x66CC66;
        return (def.red() << 16) | (def.green() << 8) | def.blue();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("RichStuffMetal", materialId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.setSize(1 + this.random.nextInt(2), true);
        return data;
    }

    public static boolean checkMetalSlimeSpawnRules(EntityType<? extends RichStuffMetalSlime> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!RichStuffConfig.ENABLE_METAL_SLIMES.get()) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        RichStuffSlimeCatalog.MetalSlimeDef def = RichStuff.profileForSlime((EntityType<?>) type);
        if (def == null) return false;
        if (pos.getY() < def.minY() || pos.getY() > def.maxY()) return false;
        float chance = (float) Math.min(1.0D, def.spawnChance() * RichStuffConfig.METAL_SLIME_SPAWN_MULTIPLIER.get());
        if (random.nextFloat() > chance) return false;
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
            && level.getBlockState(pos).isAir()
            && level.getBlockState(pos.above()).isAir();
    }
}
