package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class BedWarsEnchantment extends Enchantment {

    public BedWarsEnchantment() {
        // 🔥 МЕНЯЕМ BREAKABLE → ModEnchantmentCategories.DILDO
        super(Rarity.UNCOMMON, ModEnchantmentCategories.DILDO, new EquipmentSlot[0]);
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
        // ИЛИ на любой дильдо
        return stack.getItem() == ModItems.MUD_DILDO.get()
                || stack.getItem() == ModItems.STONE_DILDO.get()
                || stack.getItem() == ModItems.IRON_DILDO.get()
                || stack.getItem() == ModItems.GOLD_DILDO.get()
                || stack.getItem() == ModItems.DIAMOND_DILDO.get()
                || stack.getItem() == ModItems.EMERALD_DILDO.get()
                || stack.getItem() == ModItems.OBSIDIAN_DILDO.get()
                || stack.getItem() == ModItems.NETHERITE_DILDO.get()
                || stack.getItem() == ModItems.TNT_DILDO.get();
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
        return 8;
    }

    @Override
    public int getMaxCost(int level) {
        return 25;
    }
}