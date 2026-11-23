package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScooterCraftHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.getCrafting().is(ModItems.SCOOTER.get())) {
            return;
        }

        // Вместо вызова удаленного метода ensureScooterTag, пишем логику NBT прямо здесь:
        net.minecraft.world.item.ItemStack stack = event.getCrafting();
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("ScooterCharge")) tag.putInt("ScooterCharge", 15000);
        if (!tag.contains("ScooterDurability")) tag.putInt("ScooterDurability", 500);
        if (!tag.contains("ScooterSkin")) tag.putInt("ScooterSkin", event.getEntity().getRandom().nextInt(3));
        
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
            serverPlayer.containerMenu.broadcastChanges();
        }
    }
}