package com.example.zitraksmode.events;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.example.zitraksmode.ZitraksMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID)
public class EnchantingTableHandler {
    private static final Logger LOGGER = LogManager.getLogger("ZitraksMode/EnchantingTable");
    
    public static int CURRENT_TNT_COUNT = 0;

    // ПРАВИЛЬНЫЙ поиск TNT вокруг стола зачарований
    private static int countTntAround(Level level, BlockPos tablePos) {
        int count = 0;
        LOGGER.info("[TNT SCAN] Начинаем подсчёт TNT вокруг стола на позиции {}", tablePos);
        
        // Правильная схема: TNT должны стоять на том же уровне, что и книжные полки
        // Координаты относительно стола зачарований:
        // Y = 0 (тот же уровень), X и Z от -2 до +2, пропуская центральный квадрат 1x1
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Пропускаем сам стол (x=0, z=0)
                if (x == 0 && z == 0) continue;
                
                BlockPos checkPos = tablePos.offset(x, 0, z);
                
                // Проверяем блок на уровне стола
                if (level.getBlockState(checkPos).is(Blocks.TNT)) {
                    count++;
                    LOGGER.info("[TNT SCAN] TNT найден на позиции: x={}, y={}, z={}", 
                        checkPos.getX(), checkPos.getY(), checkPos.getZ());
                }
                
                // Также проверяем блок на +1 Y (над TNT должен быть воздух)
                BlockPos abovePos = checkPos.above();
                if (level.getBlockState(abovePos).is(Blocks.TNT)) {
                    count++;
                    LOGGER.info("[TNT SCAN] TNT найден на позиции: x={}, y={}, z={} (на уровне выше)", 
                        abovePos.getX(), abovePos.getY(), abovePos.getZ());
                }
            }
        }
        
        LOGGER.info("[TNT SCAN] ИТОГО найдено TNT: {}", count);
        return count;
    }

    @SubscribeEvent
    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        LOGGER.info("=== EnchantmentLevelSetEvent СРАБОТАЛ ===");
        LOGGER.info("Позиция стола: {}", event.getPos());
        LOGGER.info("Базовая сила зачарования: {}", event.getPower());
        LOGGER.info("Уровень опыта (уровень 1-30): {}", event.getEnchantLevel());
        
        CURRENT_TNT_COUNT = countTntAround(event.getLevel(), event.getPos());
        LOGGER.info("CURRENT_TNT_COUNT установлен в: {}", CURRENT_TNT_COUNT);
        
        if (CURRENT_TNT_COUNT > 0) {
            int newPower = Math.min(30, event.getPower() + CURRENT_TNT_COUNT * 2);
            LOGGER.info("Повышаем силу зачарования с {} до {}", event.getPower(), newPower);
            trySetPower(event, newPower);
        } else {
            LOGGER.info("TNT не найдены, сила зачарования не изменяется");
        }
        
        LOGGER.info("=== EnchantmentLevelSetEvent ЗАВЕРШЁН ===");
    }
    
    private static void trySetPower(EnchantmentLevelSetEvent event, int newPower) {
        try {
            java.lang.reflect.Field field = EnchantmentLevelSetEvent.class.getDeclaredField("power");
            field.setAccessible(true);
            field.setInt(event, newPower);
            LOGGER.info("Сила зачарования успешно изменена на {}", newPower);
        } catch (Exception e) {
            LOGGER.error("Не удалось изменить силу зачарования через рефлексию!", e);
        }
    }
}