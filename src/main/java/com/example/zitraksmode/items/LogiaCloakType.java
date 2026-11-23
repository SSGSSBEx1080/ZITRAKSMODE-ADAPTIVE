package com.example.zitraksmode.items;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * Balance constants for One Piece cloaks. Gauge values are absolute units,
 * shared by every cloak belonging to one player.
 */
public enum LogiaCloakType {
    CROCODILE("crocodile", "Плащ Крокодайла", 500, 4.0F, 30, 18, 150,
            0.10F, true, Rarity.UNCOMMON, ChatFormatting.GOLD, 0xC7A35B),
    AKAINU("akainu", "Плащ Акаину", 1_400, 7.0F, 36, 22, 190,
            0.10F, true, Rarity.RARE, ChatFormatting.DARK_RED, 0xF14A24),
    AOKIJI("aokiji", "Плащ Аокидзи", 1_400, 7.0F, 36, 22, 190,
            0.10F, true, Rarity.RARE, ChatFormatting.AQUA, 0x77D9FF),
    KIZARU("kizaru", "Плащ Кизару", 1_400, 7.0F, 36, 22, 190,
            0.10F, true, Rarity.RARE, ChatFormatting.YELLOW, 0xFFE34D),
    SENGOKU("sengoku", "Плащ Сэнгоку", 3_000, 10.0F, 55, 40, 260,
            0.0F, false, Rarity.EPIC, ChatFormatting.GOLD, 0xFFD22E),
    WHITEBEARD("whitebeard", "Плащ Белоуса", 3_000, 10.0F, 55, 40, 260,
            0.0F, false, Rarity.EPIC, ChatFormatting.DARK_PURPLE, 0x8E2B38);

    public final String id;
    public final String displayName;
    public final int maxGauge;
    public final float baseRegenPerSecond;
    // Эти имена сохранены совместимыми с предыдущей версией системы плащей.
    public final int meleeBlockCost;
    public final int projectileBlockCost;
    public final int explosionBlockCost;
    public final float explosionDamageMul;
    public final boolean logia;
    public final Rarity rarity;
    public final ChatFormatting nameColor;
    /** RGB, без alpha. Используется в HUD. */
    public final int tint;

    LogiaCloakType(String id, String displayName, int maxGauge, float baseRegenPerSecond,
                   int meleeCost, int projectileCost, int explosionCost,
                   float explosionDamageMultiplier, boolean logia, Rarity rarity,
                   ChatFormatting nameColor, int hudColor) {
        this.id = id;
        this.displayName = displayName;
        this.maxGauge = maxGauge;
        this.baseRegenPerSecond = baseRegenPerSecond;
        this.meleeBlockCost = meleeCost;
        this.projectileBlockCost = projectileCost;
        this.explosionBlockCost = explosionCost;
        this.explosionDamageMul = explosionDamageMultiplier;
        this.logia = logia;
        this.rarity = rarity;
        this.nameColor = nameColor;
        this.tint = hudColor;
    }

    public static LogiaCloakType byId(String id) {
        for (LogiaCloakType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return CROCODILE;
    }
}
