package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class PeacemakerEnchantment extends Enchantment {

    public PeacemakerEnchantment() {
        // 🔥 МЕНЯЕМ ARMOR → ModEnchantmentCategories.TNT_ARMOR
        super(Rarity.RARE,
                ModEnchantmentCategories.TNT_ARMOR,
                new EquipmentSlot[]{
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET
                });
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
        // ИЛИ на TNT броню
        return stack.getItem() == ModItems.ZITRAKS_HELMET.get()
                || stack.getItem() == ModItems.ZITRAKS_CHESTPLATE.get()
                || stack.getItem() == ModItems.ZITRAKS_LEGGINGS.get()
                || stack.getItem() == ModItems.ZITRAKS_BOOTS.get();
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
        return getMinCost(level) + 35; // Lv1: 45, Lv2: 55, Lv3: 65
    }
}