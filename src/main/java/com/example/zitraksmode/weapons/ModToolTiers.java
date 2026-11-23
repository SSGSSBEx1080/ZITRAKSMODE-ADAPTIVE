package com.example.zitraksmode.weapons;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.function.Supplier;

public enum ModToolTiers implements Tier {
    SNIPER_IRON(2, 500, 6.0F, 15.0F, 15, () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT)),
    SNIPER_GOLD(2, 300, 12.0F, 18.0F, 22, () -> Ingredient.of(net.minecraft.world.item.Items.GOLD_INGOT)),
    SNIPER_DIAMOND(3, 1800, 8.0F, 22.0F, 10, () -> Ingredient.of(net.minecraft.world.item.Items.DIAMOND)),
    SNIPER_NETHERITE(4, 2200, 9.0F, 25.0F, 15, () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT));

    private final int level;
    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    ModToolTiers(int level, int uses, float speed, float attackDamageBonus, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackDamageBonus;
    }

    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}