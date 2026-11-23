package com.example.zitraksmode.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class CreeperEggsItem extends Item {
    
    // Сырые яйца - как сырая курица
    private static final FoodProperties RAW_FOOD = new FoodProperties.Builder()
        .nutrition(2)           // 1 голодомер
        .saturationMod(0.3F)    // Как сырая курица
        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.2F) // 20% шанс тошноты на 10 сек
        .meat()
        .build();
    
    public CreeperEggsItem(Properties properties) {
        super(properties.food(RAW_FOOD));
    }
}
