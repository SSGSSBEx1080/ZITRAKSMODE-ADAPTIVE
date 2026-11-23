package com.example.zitraksmode.client;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.MedusaOrbEntity;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Uses vanilla thrown-item rendering; green activation particles supply the glow. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class MedusaClientRendererRegistration {
    private MedusaClientRendererRegistration() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MEDUSA_ORB.get(),
                context -> new ThrownItemRenderer<MedusaOrbEntity>(context, 1.0F, true));
    }
}
