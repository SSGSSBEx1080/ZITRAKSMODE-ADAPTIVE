package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class DoublePenetrationEnchantment extends Enchantment {

    public DoublePenetrationEnchantment() {
        // 🔥 МЕНЯЕМ BREAKABLE → ModEnchantmentCategories.SNIPER
        super(Rarity.RARE, ModEnchantmentCategories.SNIPER, new net.minecraft.world.entity.EquipmentSlot[0]);
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
        return 15; // 🔥 ПОНЯЗИЛ (было 20)
    }

    @Override
    public int getMaxCost(int level) {
        return 40; // 🔥 ПОНЯЗИЛ (было 50)
    }
}