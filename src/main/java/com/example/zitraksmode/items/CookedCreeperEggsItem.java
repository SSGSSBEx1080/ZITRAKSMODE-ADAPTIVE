package com.example.zitraksmode.items;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class CookedCreeperEggsItem extends Item {
    
    // Жареные яйца - как жареная свинина
    private static final FoodProperties COOKED_FOOD = new FoodProperties.Builder()
        .nutrition(8)           // 4 голодомера
        .saturationMod(0.8F)    // Как стейк
        .meat()
        .build();
    
    public CookedCreeperEggsItem(Properties properties) {
        super(properties.food(COOKED_FOOD));
    }
}
