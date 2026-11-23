package com.example.zitraksmode.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "zitraksmode", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MadeInHeavenFOVHandler {
    
    private static final UUID SPEED_UUID = UUID.fromString("7f8e84f0-1234-4321-abcd-1234567890ab");
    
    @SubscribeEvent
    public static void onFOVUpdate(ComputeFovModifierEvent event) {
        Player player = Minecraft.getInstance().player;
        
        if (player == null) return;
        
        // Проверяем есть ли у игрока модификатор Made in Heaven
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;
        
        var modifier = speedAttribute.getModifier(SPEED_UUID);
        if (modifier != null) {
            // ===== ОТМЕНЯЕМ FOV ЭФФЕКТ ОТ СКОРОСТИ =====
            // Minecraft увеличивает FOV на основе скорости движения
            // Мы сбрасываем модификатор обратно к 1.0 (нормальный FOV)
            
            // Получаем текущий FOV modifier
            float currentFov = event.getFovModifier();
            
            // Вычисляем "нормальный" FOV без учёта скорости
            // Формула Minecraft: fov = 1.0 + (speed - baseSpeed) / baseSpeed * fovMultiplier
            // Мы возвращаем к 1.0
            
            // Простое решение: устанавливаем FOV на базовое значение
            event.setNewFovModifier(1.0F);
        }
    }
}