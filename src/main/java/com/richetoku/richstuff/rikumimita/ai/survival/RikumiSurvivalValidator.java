package com.richetoku.richstuff.rikumimita.ai.survival;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Central survival-mode gate used before externally requested fake-player actions. */
public final class RikumiSurvivalValidator {
    public static final double MAX_REACH = 6.0D;
    private static final double MAX_REACH_SQUARED = MAX_REACH * MAX_REACH;

    private RikumiSurvivalValidator() {}

    public static void enforce(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        var abilities = player.getAbilities();
        abilities.instabuild = false;
        abilities.mayfly = false;
        abilities.flying = false;
        abilities.invulnerable = false;
    }

    public static void assertReachable(ServerPlayer player, BlockPos target) {
        assertReachable(player, Vec3.atCenterOf(target));
    }

    public static void assertReachable(ServerPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double distance = eye.distanceToSqr(target);
        if (distance > MAX_REACH_SQUARED) {
            throw new IllegalArgumentException("Target is outside Rikumi's survival reach");
        }
    }

    public static void assertCanBreak(ServerPlayer player, BlockPos target) {
        assertReachable(player, target);
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            throw new IllegalStateException("Rikumi must remain in survival mode");
        }
        if (player.level().getBlockState(target).requiresCorrectToolForDrops()
                && !player.hasCorrectToolForDrops(player.level().getBlockState(target))) {
            throw new IllegalArgumentException("Rikumi does not have the correct tool for " + target);
        }
    }
}
