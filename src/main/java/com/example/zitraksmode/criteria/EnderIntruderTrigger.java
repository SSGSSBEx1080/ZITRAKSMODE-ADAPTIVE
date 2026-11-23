package com.example.zitraksmode.criteria;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.example.zitraksmode.ZitraksMode;

public class EnderIntruderTrigger extends SimpleCriterionTrigger<EnderIntruderTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "ender_intruder_caught");

    @Override public ResourceLocation getId() { return ID; }

    @Override protected Instance createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext ctx) {
        return new Instance(player);
    }

    public void trigger(ServerPlayer owner) { this.trigger(owner, instance -> true); }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(EntityPredicate.Composite player) { super(ID, player); }
    }
}