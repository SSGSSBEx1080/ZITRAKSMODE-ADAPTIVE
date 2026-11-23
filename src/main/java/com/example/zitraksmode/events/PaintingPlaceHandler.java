package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.blocks.ModPaintings;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PaintingPlaceHandler {

    @SubscribeEvent
    public static void onPaintingSpawned(EntityJoinLevelEvent event) {
        // Проверяем что это картина и событие на сервере
        if (!(event.getEntity() instanceof Painting painting) || event.getLevel().isClientSide) return;

        // ✅ В 1.19.2 getVariant() возвращает Optional<Holder<PaintingVariant>>
        // unwrapKey() даёт Optional<ResourceKey<PaintingVariant>>
        painting.getVariant().unwrapKey().ifPresent(resourceKey -> {
            // Проверяем что это наша картина "boobs"
            if (resourceKey.location().getNamespace().equals(ZitraksMode.MODID) && 
                resourceKey.location().getPath().equals("boobs")) {
                
                // Ищем ближайшего игрока (кто поставил)
                var nearest = event.getLevel().getNearestPlayer(
                    painting.getX(), painting.getY(), painting.getZ(), 8.0, false
                );
                
                if (nearest instanceof net.minecraft.server.level.ServerPlayer sp) {
                    // ✅ Выдаём ачивку
                    ZitraksMode.PAINTING_BOOBS_PLACED.trigger(sp);
                    System.out.println("🎨 Ачивка BOOBS выдана игроку: " + sp.getName().getString());
                }
            }
        });
    }
}