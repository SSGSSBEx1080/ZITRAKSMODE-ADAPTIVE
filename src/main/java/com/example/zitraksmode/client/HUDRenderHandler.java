package com.example.zitraksmode.client;

import com.example.zitraksmode.ZitraksMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HUDRenderHandler {
    
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // Отрисовываем HUD JBL-колонки
        JBLSpeakerHUD.render(event.getPoseStack());
    }
}
