package com.example.zitraksmode.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum AqualungMaterial implements ArmorMaterial {
    INSTANCE;

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? AqualungItem.MAX_AIR : 0;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? 10 : 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                Items.IRON_INGOT,
                Items.COPPER_INGOT,
                Items.NAUTILUS_SHELL
        );
    }

    @Override
    public String getName() {
        return "zitraksmode_aqualung";
    }

    @Override
    public float getToughness() {
        return 4.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1f;
    }
}