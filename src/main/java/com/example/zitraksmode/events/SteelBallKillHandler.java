package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.SteelBallEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID)
public class SteelBallKillHandler {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide) return;
        DamageSource source = event.getSource();
        if (source.getDirectEntity() instanceof SteelBallEntity ball) {
            if (ball.getOwner() instanceof ServerPlayer player) {
                LivingEntity target = event.getEntity();
                if (!(target instanceof Player)) {
                    ZitraksMode.STEEL_BALL_KILL_TRIGGER.trigger(player);
                    // Отладочный вывод (будет видно в консоли IDE/сервера)
                    System.out.println("[SteelBall] Убит моб: " + ForgeRegistries.ENTITY_TYPES.getKey(target.getType()));
                }
            }
        }
    }
}