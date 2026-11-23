package com.example.zitraksmode.items.katana;

import com.example.zitraksmode.ModSounds;
import com.mojang.logging.LogUtils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "zitraksmode")
public class KatanaParryHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isHoldingKatana(player)) return;
        if (!ParryManager.isParrying(player)) return;

        DamageSource source = event.getSource();

        if (isParriableDamage(source)) {
            LOGGER.info("🛡️ PARRIED: {}", source.getMsgId());

            // Звук ПАРИРОВАНИЯ — ТОЛЬКО УСПЕШНОЕ ПАРИ
            int soundIndex = RANDOM.nextInt(3) + 1;
            switch (soundIndex) {
                case 1 -> player.level.playSound(null, player.blockPosition(), ModSounds.KATANA_PARRY_1.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
                case 2 -> player.level.playSound(null, player.blockPosition(), ModSounds.KATANA_PARRY_2.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
                case 3 -> player.level.playSound(null, player.blockPosition(), ModSounds.KATANA_PARRY_3.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
            }

            spawnParryEffects(player);
            event.setCanceled(true);

            // Ачивка за парирование
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.example.zitraksmode.ZitraksMode.KATANA_PARRY.trigger(sp);
            }

            // Контратака
            if (source.getEntity() != null) {
                float counterDamage = event.getAmount();
                source.getEntity().hurt(DamageSource.playerAttack(player), counterDamage);

                // Ачивка за убийство отражённой стрелой
                if (source.isProjectile() &&
                    source.getEntity() instanceof Skeleton &&
                    !source.getEntity().isAlive()) {

                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        com.example.zitraksmode.ZitraksMode.KATANA_DEFLECT_ARROW_KILL.trigger(sp);
                    }
                }
            }

            ParryManager.resetParry(player);
        }
    }

    private static boolean isHoldingKatana(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.getItem() instanceof KatanaItem ||
               offHand.getItem() instanceof KatanaItem;
    }

    private static boolean isParriableDamage(DamageSource source) {
        if (source.isProjectile()) {
            return true;
        }

        if (source.getDirectEntity() != null && source.getEntity() != null) {
            return true;
        }

        if (source.isExplosion()) {
            return true;
        }

        return false;
    }

    private static void spawnParryEffects(Player player) {
        if (player.level.isClientSide) return;

        ServerLevel level = (ServerLevel) player.level;

        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2 * i) / 30;
            double radius = 1.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 1.2;

            level.sendParticles(
                    ParticleTypes.CRIT,
                    x, y, z,
                    1,
                    Math.cos(angle) * 0.5,
                    0.1,
                    Math.sin(angle) * 0.5,
                    0.2
            );

            level.sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    x, y, z,
                    2,
                    Math.cos(angle) * 0.3,
                    0.2,
                    Math.sin(angle) * 0.3,
                    0.1
            );
        }
    }
}