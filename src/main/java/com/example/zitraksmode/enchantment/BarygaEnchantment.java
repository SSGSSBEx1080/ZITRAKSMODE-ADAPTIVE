package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class BarygaEnchantment extends Enchantment {

    public BarygaEnchantment() {
        // 🔥 МЕНЯЕМ BREAKABLE → ModEnchantmentCategories.PRINTER_3D
        super(Rarity.UNCOMMON, ModEnchantmentCategories.PRINTER_3D, new EquipmentSlot[0]);
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // Можно поставить на книгу
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return true;
        }
        // ИЛИ на 3D принтер
        return stack.getItem() == ModItems.PRINTER_3D_ITEM.get();
    }

    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    public boolean isTreasureOnly() {
        return false;
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 12; // Lv1: 10, Lv2: 22
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30; // Lv1: 40, Lv2: 52
    }
}