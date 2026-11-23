package com.example.zitraksmode.criteria;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags; // ← ПРАВИЛЬНЫЙ ИМПОРТ ДЛЯ 1.19.2
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.example.zitraksmode.ZitraksMode;

public class SniperLongShotTrigger extends SimpleCriterionTrigger<SniperLongShotTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "sniper_long_shot");

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext ctx) {
        return new Instance(player);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(EntityPredicate.Composite player) { super(ID, player); }
    }

    @Mod.EventBusSubscriber(modid = ZitraksMode.MODID)
    public static class Listener {
        @SubscribeEvent
        public static void onEntityDeath(LivingDeathEvent event) {
            DamageSource source = event.getSource();
            Entity attacker = source.getEntity();
            
            if (attacker instanceof ServerPlayer player) {
                LivingEntity target = event.getEntity();
                
                // ✅ ПРОВЕРКА ДИСТАНЦИИ >= 100 БЛОКОВ
                if (player.distanceTo(target) >= 100.0) {
                    ItemStack mainHand = player.getMainHandItem();
                    
                    // ✅ ПРОВЕРКА КАСТОМНОГО ТЕГА (исправлено под 1.19.2)
                    if (mainHand.is(ItemTags.create(new ResourceLocation("zitraksmode", "snipers")))) {
                        ZitraksMode.SNIPER_LONG_SHOT.trigger(player);
                    }
                }
            }
        }
    }
}