package com.example.zitraksmode.client;

import com.example.zitraksmode.items.MadeInHeavenItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "zitraksmode", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MadeInHeavenCameraShake {
    
    private static final UUID SPEED_UUID = UUID.fromString("7f8e84f0-1234-4321-abcd-1234567890ab");
    private static final Random random = new Random();
    
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        // Проверяем есть ли активный Made in Heaven
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;
        
        var modifier = speedAttribute.getModifier(SPEED_UUID);
        if (modifier == null) return;
        
        // Получаем прогресс
        int currentTick = MadeInHeavenItem.currentTick;
        int resetTick = MadeInHeavenItem.resetTick;
        
        if (currentTick == 0) return;
        
        float progress = (float) currentTick / resetTick;
        
        // ===== ТРЯСКА КАМЕРЫ В ПОСЛЕДНИЕ 10 СЕКУНД =====
        // 520 tick = 26 сек (начало последних 10 секунд)
        if (currentTick > 520) {
            // Прогресс тряски (0.0 - 1.0) от 26 до 36 секунды
            float shakeProgress = (currentTick - 520) / 200.0f;
            
            // Интенсивность тряски нарастает
            float intensity;
            if (shakeProgress < 0.5f) {
                // Первые 5 секунд: слабая тряска
                intensity = shakeProgress * 0.4f; // 0.0 - 0.2
            } else if (shakeProgress < 0.8f) {
                // 5-8 секунд: средняя тряска
                intensity = 0.2f + (shakeProgress - 0.5f) * 1.33f; // 0.2 - 0.6
            } else {
                // Последние 2 секунды: БЕЗУМНАЯ тряска!
                intensity = 0.6f + (shakeProgress - 0.8f) * 4.0f; // 0.6 - 1.4
            }
            
            // Генерируем случайные углы тряски
            float yawShake = (random.nextFloat() - 0.5f) * 2.0f * intensity;
            float pitchShake = (random.nextFloat() - 0.5f) * 1.5f * intensity;
            float rollShake = (random.nextFloat() - 0.5f) * 0.5f * intensity;
            
            // Применяем тряску к камере
            event.setYaw((float) (event.getYaw() + yawShake));
            event.setPitch((float) (event.getPitch() + pitchShake));
            event.setRoll((float) (event.getRoll() + rollShake));
        }
    }
}