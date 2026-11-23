package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ZaebisEnchantment extends Enchantment {
    
    public ZaebisEnchantment() {
        // 🔥 МЕНЯЕМ BREAKABLE → ModEnchantmentCategories.PRINTER_3D
        super(Rarity.RARE, ModEnchantmentCategories.PRINTER_3D, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 1;
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
        return 15;
    }

    @Override
    public int getMaxCost(int level) {
        return 40;
    }
}