package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ZhirnyPidorEnchantment extends Enchantment {

    public ZhirnyPidorEnchantment() {
        // 🔥 МЕНЯЕМ ARMOR_CHEST → ModEnchantmentCategories.AQUALUNG
        super(Rarity.VERY_RARE, ModEnchantmentCategories.AQUALUNG, new EquipmentSlot[]{EquipmentSlot.CHEST});
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
        // ИЛИ на AQUALUNG
        return stack.getItem() == ModItems.AQUALUNG.get();
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
    public boolean isCurse() {
        return true;
    }

    @Override
    public int getMinCost(int level) {
        return 25; // VERY_RARE проклятие
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }
}