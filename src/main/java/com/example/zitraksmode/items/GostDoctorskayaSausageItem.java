package com.example.zitraksmode.items;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/** Doctor's sausage from Valya's GOST point. It has no crafting recipe. */
public final class GostDoctorskayaSausageItem extends Item {
    public GostDoctorskayaSausageItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(10)
                .saturationMod(0.8F)
                .meat()
                .build()));
    }
}
