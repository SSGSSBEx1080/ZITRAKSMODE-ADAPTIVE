package com.example.zitraksmode.items.katana;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система cooldown для скилов катаны
 * Исправлена проблема, когда при смене мира / откате времени
 * кд улетал в огромные значения.
 */
public class KatanaCooldownManager {

    public static final int ULTIMATE_COOLDOWN = 400;
    public static final int SLASH_CHARGE_TIME = 80;

    private static final Map<UUID, Long> ultimateCooldowns = new HashMap<>();
    private static final Map<UUID, Long> slashChargeStart = new HashMap<>();

    private static long now(Player player) {
        return player.level.getGameTime();
    }

    private static boolean isTimeInvalid(Player player, long storedTime) {
        // Если время мира меньше, чем сохранённое — значит игрок
        // сменил мир / время откатилось / старый static-кд уже невалиден
        return now(player) < storedTime;
    }

    public static void setUltimateCooldown(Player player) {
        ultimateCooldowns.put(player.getUUID(), now(player));
    }

    public static boolean isUltimateReady(Player player) {
        Long cooldownStart = ultimateCooldowns.get(player.getUUID());
        if (cooldownStart == null) {
            return true;
        }

        if (isTimeInvalid(player, cooldownStart)) {
            ultimateCooldowns.remove(player.getUUID());
            return true;
        }

        long elapsed = now(player) - cooldownStart;
        return elapsed >= ULTIMATE_COOLDOWN;
    }

    public static int getUltimateRemainingTicks(Player player) {
        Long cooldownStart = ultimateCooldowns.get(player.getUUID());
        if (cooldownStart == null) {
            return 0;
        }

        if (isTimeInvalid(player, cooldownStart)) {
            ultimateCooldowns.remove(player.getUUID());
            return 0;
        }

        long elapsed = now(player) - cooldownStart;
        return Math.max(0, (int) (ULTIMATE_COOLDOWN - elapsed));
    }

    public static void startSlashCharge(Player player) {
        slashChargeStart.put(player.getUUID(), now(player));
    }

    public static void stopSlashCharge(Player player) {
        slashChargeStart.remove(player.getUUID());
    }

    public static boolean isCharging(Player player) {
        Long chargeStart = slashChargeStart.get(player.getUUID());
        if (chargeStart == null) {
            return false;
        }

        if (isTimeInvalid(player, chargeStart)) {
            slashChargeStart.remove(player.getUUID());
            return false;
        }

        return true;
    }

    public static float getChargeProgress(Player player) {
        Long chargeStart = slashChargeStart.get(player.getUUID());
        if (chargeStart == null) {
            return 0.0f;
        }

        if (isTimeInvalid(player, chargeStart)) {
            slashChargeStart.remove(player.getUUID());
            return 0.0f;
        }

        long elapsed = now(player) - chargeStart;
        return Math.min(1.0f, (float) elapsed / SLASH_CHARGE_TIME);
    }

    public static boolean isFullyCharged(Player player) {
        return getChargeProgress(player) >= 1.0f;
    }

    public static void clearPlayer(Player player) {
        UUID uuid = player.getUUID();
        ultimateCooldowns.remove(uuid);
        slashChargeStart.remove(uuid);
    }

    public static void clearAll() {
        ultimateCooldowns.clear();
        slashChargeStart.clear();
    }
}