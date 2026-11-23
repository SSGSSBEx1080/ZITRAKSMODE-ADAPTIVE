package com.example.zitraksmode.items;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

public enum LabubuRarity {
    // ВАЖНО: Вторым аргументом в Rarity.create мы теперь передаем УНИКАЛЬНОЕ имя.
    // Это заставит игру создать НОВУЮ редкость с НОВЫМ цветом, а не брать стандартную.

    // Обычные - БЕЛЫЕ
    COMMON(40, ChatFormatting.WHITE, Rarity.create("LABUBU_COMMON", ChatFormatting.WHITE)),
    
    // Необычные - ЗЕЛЕНЫЕ (Теперь точно будут зелеными)
    UNCOMMON(30, ChatFormatting.GREEN, Rarity.create("LABUBU_UNCOMMON", ChatFormatting.GREEN)), 
    
    // Редкие - СИНИЕ
    RARE(15, ChatFormatting.BLUE, Rarity.create("LABUBU_RARE", ChatFormatting.BLUE)),

    // Эпические - ФИОЛЕТОВЫЕ
    EPIC(10, ChatFormatting.LIGHT_PURPLE, Rarity.create("LABUBU_EPIC", ChatFormatting.LIGHT_PURPLE)),
    
    // Легендарные - ЯРКО-ЖЕЛТЫЕ (YELLOW вместо GOLD для более желтого оттенка)
    LEGENDARY(5, ChatFormatting.YELLOW, Rarity.create("LABUBU_LEGENDARY", ChatFormatting.YELLOW));

    public final int weight;
    public final ChatFormatting color;
    public final Rarity minecraftRarity;

    LabubuRarity(int weight, ChatFormatting color, Rarity minecraftRarity) {
        this.weight = weight;
        this.color = color;
        this.minecraftRarity = minecraftRarity;
    }
}