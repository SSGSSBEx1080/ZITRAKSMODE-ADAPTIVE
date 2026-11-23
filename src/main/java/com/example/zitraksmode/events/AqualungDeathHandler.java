package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.AqualungItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource; // ✅
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AqualungDeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // ✅ Проверка утопления для 1.19.2
        if (event.getSource() != DamageSource.DROWN) return;

        // Проверяем, что на груди был акваланг
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.AQUALUNG.get())) {
            // Проверяем, что воздух полностью кончился
            if (AqualungItem.getAir(chest) <= 0) {
                if (player instanceof ServerPlayer sp) {
                    ZitraksMode.AQUALUNG_DROWN.trigger(sp);
                }
            }
        }
    }
}