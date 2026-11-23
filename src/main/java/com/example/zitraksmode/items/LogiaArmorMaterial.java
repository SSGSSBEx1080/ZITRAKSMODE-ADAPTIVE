package com.example.zitraksmode.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Physical armor statistics of the cloaks. The Logia gauge still handles
 * phasing/immunity; these numbers are ordinary vanilla chestplate stats.
 */
public enum LogiaArmorMaterial implements ArmorMaterial {
    // ArmorMaterial array order is FEET, LEGS, CHEST, HEAD. CHEST is index 2.
    CROCODILE("cloak_crocodile", 30, new int[] {0, 0, 5, 0}, 15,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, () -> Ingredient.EMPTY),

    ADMIRAL("cloak_admiral", 36, new int[] {0, 0, 9, 0}, 18,
            SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.0F, () -> Ingredient.EMPTY),

    FINAL("cloak_final", 42, new int[] {0, 0, 10, 0}, 20,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.5F, () -> Ingredient.EMPTY);

    private static final int[] HEALTH_PER_SLOT = new int[] {13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protection;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    /** Vanilla value is a fraction: 0.5F means 50% knockback resistance. */
    private final float knockbackResistance;
    private final LazyLoadedValue<Ingredient> repairIngredient;

    LogiaArmorMaterial(String name, int durabilityMultiplier, int[] protection,
                       int enchantability, SoundEvent equipSound, float toughness,
                       float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protection = protection;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = new LazyLoadedValue<>(repairIngredient);
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return HEALTH_PER_SLOT[slot.getIndex()] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return protection[slot.getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return "zitraksmode:" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
