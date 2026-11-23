package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class WorldSlashEnchantment extends Enchantment {

    public WorldSlashEnchantment() {
        // 🔥 МЕНЯЕМ WEAPON → ModEnchantmentCategories.KATANA
        super(Rarity.RARE, ModEnchantmentCategories.KATANA, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // Можно поставить на книгу
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return true;
        }
        // ИЛИ на катану
        return stack.getItem() == ModItems.KATANA.get();
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
        return 10 + (level - 1) * 10; // Lv1: 10, Lv2: 20, Lv3: 30, Lv4: 40, Lv5: 50
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30; // Lv1: 40, Lv2: 50, Lv3: 60, Lv4: 70, Lv5: 80
    }
}