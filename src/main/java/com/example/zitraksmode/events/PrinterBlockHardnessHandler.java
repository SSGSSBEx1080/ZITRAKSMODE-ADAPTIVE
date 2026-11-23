package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.max.PrintedBlocksSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PrinterBlockHardnessHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Level level = event.getEntity().level;
        if (level.isClientSide()) return; 
        
        PrintedBlocksSavedData data = PrintedBlocksSavedData.get((ServerLevel) level);
        byte mod = data.getModifier(event.getPos());
        
        if (mod == 1) {
            // ZAEBIS: Прочность x2 -> Ломаем в 2 раза ДОЛЬШЕ (скорость 0.5)
            event.setNewSpeed(event.getOriginalSpeed() * 0.5f);
        } else if (mod == 2) {
            // NAHUYA: Прочность /2 -> Ломаем в 2 раза БЫСТРЕЕ (скорость 2.0)
            event.setNewSpeed(event.getOriginalSpeed() * 2.0f);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (!level.isClientSide()) {
            PrintedBlocksSavedData.get((ServerLevel) level).removeModifier(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            PrintedBlocksSavedData data = PrintedBlocksSavedData.get((ServerLevel) level);
            for (BlockPos pos : event.getAffectedBlocks()) {
                data.removeModifier(pos);
            }
        }
    }
}