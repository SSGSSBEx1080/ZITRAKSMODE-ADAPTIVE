package com.example.zitraksmode.criteria;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.example.zitraksmode.ZitraksMode;

public class HentaiMemeFacesTrigger extends SimpleCriterionTrigger<HentaiMemeFacesTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "hentai_meme_faces");
    @Override public ResourceLocation getId() { return ID; }
    @Override protected Instance createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext ctx) { return new Instance(player); }
    public void trigger(ServerPlayer player) { this.trigger(player, instance -> true); }
    public static class Instance extends AbstractCriterionTriggerInstance { public Instance(EntityPredicate.Composite player) { super(ID, player); } }
}