package com.example.zitraksmode.items;

import com.example.zitraksmode.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LabubuLootTable {

    private static final Random RANDOM = new Random();
    private static final List<LabubuType> LOOT_TABLE = new ArrayList<>();

    static {
        for (LabubuType type : LabubuType.values()) {
            int weight = type.rarity.weight;
            for (int i = 0; i < weight; i++) {
                LOOT_TABLE.add(type);
            }
        }
    }

    public static LabubuType getRandomType() {
        if (LOOT_TABLE.isEmpty()) {
            return LabubuType.RED;
        }
        return LOOT_TABLE.get(RANDOM.nextInt(LOOT_TABLE.size()));
    }

    public static ItemStack getRandomFigurine() {
        return getFigurineStack(getRandomType());
    }

    public static ItemStack getFigurineStack(LabubuType type) {
        try {
            return new ItemStack(ModItems.LABUBU_FIGURINES.get(type).get());
        } catch (Exception e) {
            return new ItemStack(Items.PAPER);
        }
    }

    public static List<LabubuType> createRouletteSequence(LabubuType winner, int size, int winnerIndex) {
        List<LabubuType> result = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            result.add(getRandomType());
        }

        if (winnerIndex >= 0 && winnerIndex < result.size()) {
            result.set(winnerIndex, winner);
        }

        return result;
    }
}