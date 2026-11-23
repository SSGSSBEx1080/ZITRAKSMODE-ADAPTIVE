package com.example.zitraksmode.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class DildoBlockItem extends BlockItem {

    public DildoBlockItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 10;
    }
}