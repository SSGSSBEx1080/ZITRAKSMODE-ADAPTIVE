package com.example.zitraksmode.client;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Клиентские события мода (только для Dist.CLIENT).
 * Подключение рендереров и т.п.
 *
 * ВАЖНО: класс автоматически подписывается на mod event bus благодаря
 * @Mod.EventBusSubscriber. Никакой ручной регистрации не нужно — он сам
 * найдётся при загрузке.
 */
@Mod.EventBusSubscriber(
        modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Подменяем стандартный VillagerRenderer на наш — чтобы прятать нос у Подрывника
        event.registerEntityRenderer(EntityType.VILLAGER, NoseRemoverVillagerRenderer::new);
    }
}
