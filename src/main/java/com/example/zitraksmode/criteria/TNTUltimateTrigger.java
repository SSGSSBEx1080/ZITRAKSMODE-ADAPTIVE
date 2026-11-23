package com.example.zitraksmode.criteria;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.example.zitraksmode.ZitraksMode;

public class TNTUltimateTrigger extends SimpleCriterionTrigger<TNTUltimateTrigger.Instance> {
    
    // Важно: ID без пробелов
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "tnt_ultimate");

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

    // Метод для вызова из AbilitySystem.triggerExplosion
    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(EntityPredicate.Composite player) {
            super(ID, player);
        }
    }
}