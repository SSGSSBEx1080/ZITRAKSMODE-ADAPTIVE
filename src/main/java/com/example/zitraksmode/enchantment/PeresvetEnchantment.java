package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class PeresvetEnchantment extends Enchantment {

    public PeresvetEnchantment() {
        // 🔥 МЕНЯЕМ ARMOR_HEAD → ModEnchantmentCategories.MINER_HELMET
        super(Rarity.RARE, ModEnchantmentCategories.MINER_HELMET, new EquipmentSlot[]{EquipmentSlot.HEAD});
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // Можно поставить на книгу
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return true;
        }
        // ИЛИ на шахтёрский шлем
        return stack.getItem() == ModItems.MINER_HELMET.get();
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
        return 10 + (level - 1) * 10; // Lv1: 10, Lv2: 20, Lv3: 30
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 25; // Lv1: 35, Lv2: 45, Lv3: 55
    }
}