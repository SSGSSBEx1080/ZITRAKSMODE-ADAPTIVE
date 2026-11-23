package com.example.zitraksmode.client;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Transparent crop and thin salt trail rendering. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class NinaClientRenderLayers {
    private NinaClientRenderLayers() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RADISH_CROP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SALT_TRAIL.get(), RenderType.cutout());
        });
    }
}
