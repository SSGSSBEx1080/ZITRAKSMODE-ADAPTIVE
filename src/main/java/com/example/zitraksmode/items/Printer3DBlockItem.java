package com.example.zitraksmode.items;

import com.example.zitraksmode.blocks.Printer3DBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class Printer3DBlockItem extends BlockItem {

    public Printer3DBlockItem(Printer3DBlock block, Properties properties) {
        super(block, properties);
    }

    // ===== 🔥 ДОБАВЛЯЕМ =====
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    // ===== 🔥 ДОБАВЛЯЕМ =====
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15; // Как у алмазных инструментов
    }
}