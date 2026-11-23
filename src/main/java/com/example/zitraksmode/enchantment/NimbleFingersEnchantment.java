package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class NimbleFingersEnchantment extends Enchantment {

    public NimbleFingersEnchantment() {
        // 🔥 МЕНЯЕМ BREAKABLE → ModEnchantmentCategories.SNIPER
        super(Rarity.UNCOMMON, ModEnchantmentCategories.SNIPER, new net.minecraft.world.entity.EquipmentSlot[0]);
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
        // ИЛИ на любую снайперку
        return stack.getItem() == ModItems.IRON_SNIPER.get()
                || stack.getItem() == ModItems.GOLDEN_SNIPER.get()
                || stack.getItem() == ModItems.DIAMOND_SNIPER.get()
                || stack.getItem() == ModItems.NETHERITE_SNIPER.get();
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
        return 8 + (level - 1) * 8; // Lv1: 8, Lv2: 16, Lv3: 24
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 25; // Lv1: 33, Lv2: 41, Lv3: 49
    }
}