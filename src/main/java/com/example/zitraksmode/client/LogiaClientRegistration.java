package com.example.zitraksmode.client;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.client.armor.LogiaCloakArmorRenderer;
import com.example.zitraksmode.items.LogiaCloakItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

/** Dedicated GeckoLib 3 registration. Keeping it separate means the existing
 * large ClientSetup.java does not need another fragile manual edit. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LogiaClientRegistration {
    private LogiaClientRegistration() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                GeoArmorRenderer.registerArmorRenderer(LogiaCloakItem.class, LogiaCloakArmorRenderer::new));
    }
}
