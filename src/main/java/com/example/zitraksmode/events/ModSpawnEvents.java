package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.entities.AdunVillager;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Класс для регистрации правил появления Адуна в мире.
 */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSpawnEvents {

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        // Атрибуты регистрируются в главном классе ZitraksMode
    }

    /**
     * Регистрация условий спавна. 
     * Вызывается через bus.addListener(ModSpawnEvents::setupSpawns) в главном классе.
     */
    public static void setupSpawns(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Регистрируем только Адуна
            // Используем кастомный метод canSpawn из AdunVillager для контроля плотности
            SpawnPlacements.register(ModEntities.ADUN_VILLAGER.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    AdunVillager::canSpawn);
        });
    }
}