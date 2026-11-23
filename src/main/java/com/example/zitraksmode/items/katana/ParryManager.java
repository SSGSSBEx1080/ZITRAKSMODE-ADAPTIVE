package com.example.zitraksmode.items.katana;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Парирование с окном и кулдауном.
 * Звук и эффекты должны срабатывать ТОЛЬКО при успешном парировании,
 * а не при каждом нажатии.
 */
public class ParryManager {

    private static final Map<UUID, Long> parryStart = new HashMap<>();
    private static final Map<UUID, Long> parryLastUsed = new HashMap<>();

    // Окно парирования
    private static final long PARRY_DURATION = 5; // 5 тиков = 0.25 сек

    // Кулдаун между парри, чтобы не спамили и анимация успевала
    private static final long PARRY_COOLDOWN = 12; // 12 тиков = 0.6 сек

    public static void startParry(Player player) {
        long currentTime = player.level.getGameTime();
        UUID uuid = player.getUUID();

        Long lastUsed = parryLastUsed.get(uuid);
        if (lastUsed != null && (currentTime - lastUsed) < PARRY_COOLDOWN) {
            return;
        }

        parryStart.put(uuid, currentTime);
        parryLastUsed.put(uuid, currentTime);
    }

    public static boolean isParrying(Player player) {
        Long startTime = parryStart.get(player.getUUID());
        if (startTime == null) {
            return false;
        }

        long currentTime = player.level.getGameTime();
        long elapsed = currentTime - startTime;

        if (elapsed > PARRY_DURATION) {
            parryStart.remove(player.getUUID());
            return false;
        }

        return true;
    }

    public static void resetParry(Player player) {
        parryStart.remove(player.getUUID());
    }

    public static boolean canParry(Player player) {
        long currentTime = player.level.getGameTime();
        UUID uuid = player.getUUID();

        // Пока активное окно не закончилось — заново стартовать нельзя
        if (isParrying(player)) {
            return false;
        }

        Long lastUsed = parryLastUsed.get(uuid);
        if (lastUsed == null) {
            return true;
        }

        return (currentTime - lastUsed) >= PARRY_COOLDOWN;
    }

    public static void triggerParryAchievement(net.minecraft.server.level.ServerPlayer player) {
        if (player != null) {
            com.example.zitraksmode.ZitraksMode.KATANA_PARRY.trigger(player);
        }
    }
}