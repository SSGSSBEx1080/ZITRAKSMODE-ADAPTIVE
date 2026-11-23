package com.example.zitraksmode.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum SpanishBootsMaterial implements ArmorMaterial {
    INSTANCE;

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET ? 450 : 0;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET ? 2 : 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.COPPER_INGOT);
    }

    @Override
    public String getName() {
        return "zitraksmode_spanish_boots";
    }

    @Override
    public float getToughness() {
        return 0.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0f;
    }
}