package com.example.zitraksmode.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public enum DildoMaterial {
    MUD("mud", "Грязь", SoundType.HONEY_BLOCK, 0.5f, 0.5f, false),
    STONE("stone", "Камень", SoundType.HONEY_BLOCK, 1.5f, 6.0f, true),
    IRON("iron", "Железо", SoundType.HONEY_BLOCK, 5.0f, 6.0f, true),
    GOLD("gold", "Золото", SoundType.HONEY_BLOCK, 3.0f, 6.0f, true),
    DIAMOND("diamond", "Алмаз", SoundType.HONEY_BLOCK, 5.0f, 6.0f, true),
    EMERALD("emerald", "Изумруд", SoundType.HONEY_BLOCK, 5.0f, 6.0f, true),
    OBSIDIAN("obsidian", "Обсидиан", SoundType.HONEY_BLOCK, 50.0f, 1200.0f, true),
    NETHERITE("netherite", "Незерит", SoundType.HONEY_BLOCK, 50.0f, 1200.0f, true),
    TNT("tnt", "TNT", SoundType.HONEY_BLOCK, 0.0f, 0.0f, false);

    public final String name;
    public final String displayName;
    public final SoundType soundType;
    public final float destroyTime;
    public final float explosionResistance;
    public final boolean requiresCorrectTool;

    DildoMaterial(String name, String displayName, SoundType soundType,
                  float destroyTime, float explosionResistance, boolean requiresCorrectTool) {
        this.name = name;
        this.displayName = displayName;
        this.soundType = soundType;
        this.destroyTime = destroyTime;
        this.explosionResistance = explosionResistance;
        this.requiresCorrectTool = requiresCorrectTool;
    }

    public BlockBehaviour.Properties getProperties() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.copy(getBaseBlock())
                .strength(destroyTime, explosionResistance)
                .sound(soundType);

        if (requiresCorrectTool) {
            props = props.requiresCorrectToolForDrops();
        }

        return props;
    }

    public Item getBedWarsDrop() {
        return switch (this) {
            case MUD -> Blocks.DIRT.asItem();
            case STONE -> Blocks.STONE.asItem();
            case IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            case DIAMOND -> Items.DIAMOND;
            case EMERALD -> Items.EMERALD;
            case OBSIDIAN -> Blocks.OBSIDIAN.asItem();
            case NETHERITE -> Items.NETHERITE_INGOT;
            case TNT -> Items.GUNPOWDER;
        };
    }

    private net.minecraft.world.level.block.Block getBaseBlock() {
        return switch (this) {
            case MUD -> Blocks.MUD;
            case STONE -> Blocks.STONE;
            case IRON -> Blocks.IRON_BLOCK;
            case GOLD -> Blocks.GOLD_BLOCK;
            case DIAMOND -> Blocks.DIAMOND_BLOCK;
            case EMERALD -> Blocks.EMERALD_BLOCK;
            case OBSIDIAN -> Blocks.OBSIDIAN;
            case NETHERITE -> Blocks.NETHERITE_BLOCK;
            case TNT -> Blocks.TNT;
        };
    }
}