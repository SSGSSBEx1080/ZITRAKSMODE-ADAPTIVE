package com.example.zitraksmode.criteria;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.ModEnchantments;

public class BombardierSetTrigger extends SimpleCriterionTrigger<BombardierSetTrigger.Instance> {
    
    // Важно: ID без пробелов, иначе JSON не найдёт триггер
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "bombardier_set");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(
            JsonObject json, 
            EntityPredicate.Composite player, 
            DeserializationContext conditions) {
        return new Instance(player);
    }

    // Метод для вызова из AbilitySystem или другого места
    public void checkAndTrigger(ServerPlayer player) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        boolean isFullSet = true;

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) { 
                isFullSet = false; 
                break; 
            }
            
            int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BOMBARDIER.get(), stack);
            if (level < 3) { 
                isFullSet = false; 
                break; 
            }
        }

        if (isFullSet) {
            this.trigger(player, instance -> true);
        }
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(EntityPredicate.Composite player) {
            super(ID, player);
        }
    }
}