package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.AdunVillager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID)
public class AdunDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) return;
        if (!(event.getEntity() instanceof AdunVillager)) return;

        DamageSource source = event.getSource();
        Entity killer = source.getEntity();
        if (killer == null) {
            killer = source.getDirectEntity();
        }

        ServerPlayer player = null;
        if (killer instanceof ServerPlayer) {
            player = (ServerPlayer) killer;
        } else if (killer instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) {
            player = owner;
        }

        if (player != null) {
            ZitraksMode.KILL_ADUN.trigger(player);
            System.out.println("[AdunDeath] Triggered kill_adun for " + player.getName().getString());
        } else {
            System.out.println("[AdunDeath] Killer not found for " + event.getEntity());
        }
    }
}