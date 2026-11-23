package com.example.zitraksmode.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ChargedCreeperEggsItem extends Item {
    
    public ChargedCreeperEggsItem(Properties properties) {
        super(properties
            .rarity(Rarity.RARE)      // Редкость (голубой цвет названия)
            .stacksTo(64));
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Энчант глинт (блеск как у зачарованных предметов)
        return true;
    }
}
