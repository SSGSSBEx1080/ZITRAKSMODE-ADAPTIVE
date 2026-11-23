package com.example.zitraksmode.client;

import com.example.zitraksmode.ModBlockEntities;
import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ModMenuTypes;
import com.example.zitraksmode.client.ScooterHudOverlay;
import com.example.zitraksmode.client.renderer.ScooterRenderer;
import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.client.renderer.AntonChigurRenderer;
import com.example.zitraksmode.blocks.RandomArtPools;
import com.example.zitraksmode.client.gui.Printer3DScreen;
import com.example.zitraksmode.client.gui.screens.EnderSafeScreen;
import com.example.zitraksmode.client.EnderFurnaceScreen; 
import com.example.zitraksmode.client.renderer.BulletRenderer;
import com.example.zitraksmode.client.renderer.DeathNoteRenderer;
import com.example.zitraksmode.client.renderer.DildoSeatRenderer;
import com.example.zitraksmode.client.renderer.GazanRenderer;
import com.example.zitraksmode.client.renderer.HentaiBlockRenderer;
import com.example.zitraksmode.client.renderer.LaserBeamRenderer;
import com.example.zitraksmode.client.renderer.MahoragaCinematicRenderer;
import com.example.zitraksmode.client.renderer.MahoragaRenderer;
import com.example.zitraksmode.client.renderer.Printer3DRenderer;
import com.example.zitraksmode.client.renderer.SuperTNTRenderer;
import com.example.zitraksmode.client.renderer.entity.AdunVillagerRenderer;
import com.example.zitraksmode.client.audio.MusicTrackManager;
import com.example.zitraksmode.client.gui.Printer3DScreen;
import com.example.zitraksmode.client.renderer.LaserBeamRenderer;
import com.example.zitraksmode.entities.LaserBeamEntity;
import net.minecraft.client.renderer.entity.EntityRenderers;
// GeckoLib импорты
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import com.example.zitraksmode.client.katana.KatanaRenderer;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;
import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.client.renderer.DeathNoteRenderer;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;
import com.example.zitraksmode.client.renderer.GazanRenderer;
import com.example.zitraksmode.ModItems;
// Добавленные импорты для сущностей катаны
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;   // <-- если ругается, что ItemRenderer не виден
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.example.zitraksmode.client.armor.SpanishBootsArmorRenderer;
import com.example.zitraksmode.items.SpanishBootsItem;
import com.example.zitraksmode.client.renderer.Printer3DRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.example.zitraksmode.blocks.RandomArtPools;
// Импорты для оверлеев
import com.example.zitraksmode.client.MadeInHeavenFOVHandler;
import com.example.zitraksmode.client.MadeInHeavenCameraShake;
import com.example.zitraksmode.client.UniverseResetOverlay;
import com.example.zitraksmode.client.armor.AqualungArmorRenderer;
import com.example.zitraksmode.client.armor.SpanishBootsArmorRenderer;
import com.example.zitraksmode.items.AqualungItem;
import com.example.zitraksmode.items.SpanishBootsItem;

import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;
import com.example.zitraksmode.client.renderer.MahoragaRenderer;

@Mod.EventBusSubscriber(modid = "zitraksmode", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Регистрируем полоску энергии. 
        // "energy_bar" — это просто ID, можешь написать любой.
        event.registerAboveAll("energy_bar", EnergyOverlay.HUD_ENERGY);
        event.registerAboveAll("sniper_scope", SniperAimingOverlay.INSTANCE);
        event.registerAboveAll("aqualung_air", new AqualungHUD());
        event.registerAboveAll("scooter_hud", ScooterHudOverlay.HUD);
         event.registerAboveAll("logia_gauge", LogiaGaugeOverlay.HUD);
        }

        
    @SubscribeEvent
    
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftForge.EVENT_BUS.register(SniperAimingOverlay.INSTANCE);
            EntityRenderers.register(ModEntities.BULLET.get(), BulletRenderer::new);
            EntityRenderers.register(ModEntities.DILDO_SEAT.get(), DildoSeatRenderer::new);
            EntityRenderers.register(ModEntities.ADUN_VILLAGER.get(), AdunVillagerRenderer::new);
            EntityRenderers.register(ModEntities.SUPER_TNT.get(), SuperTNTRenderer::new);
            EntityRenderers.register(ModEntities.SUPER_TNT_MINECART.get(), TntMinecartRenderer::new);
            EntityRenderers.register(ModEntities.GAZAN.get(), GazanRenderer::new); 
            EntityRenderers.register(ModEntities.MAHORAGA.get(), MahoragaRenderer::new);
            EntityRenderers.register(ModEntities.LASER_BEAM.get(), LaserBeamRenderer::new);
            EntityRenderers.register(ModEntities.MAHORAGA_CINEMATIC.get(), MahoragaCinematicRenderer::new);
            EntityRenderers.register(ModEntities.SCOOTER.get(), ScooterRenderer::new);
            EntityRenderers.register(ModEntities.ANTON_CHIGUR.get(), AntonChigurRenderer::new);
            
            
            // --- РЕГИСТРАЦИЯ СУЩНОСТЕЙ КАТАНЫ ---
            // Регистрируем невидимые рендереры для эффектов ударов
            
            EntityRenderers.register(ModEntities.SLASH_ATTACK.get(), NoopRenderer::new);
            EntityRenderers.register(ModEntities.VERTICAL_SLASH.get(), com.example.zitraksmode.client.renderer.VerticalSlashRenderer::new);
            RandomArtPools.init();
            // --- СЛОИ ПРОЗРАЧНОСТИ БЛОКОВ ---
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.REINFORCED_ENDER_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENDER_DOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENDER_TRAPDOOR.get(), RenderType.cutout());
            BlockEntityRenderers.register(ModBlockEntities.PRINTER_3D.get(), ctx -> new Printer3DRenderer());
            EntityRenderers.register(ModEntities.STEEL_BALL.get(), SteelBallRenderer::new);
            MusicTrackManager.init();
            
            if (ModBlocks.LABUBU_FIGURINE_BLOCKS != null) {
                ModBlocks.LABUBU_FIGURINE_BLOCKS.values().forEach(obj -> 
                    ItemBlockRenderTypes.setRenderLayer(obj.get(), RenderType.cutout())
                );
            }
        
            // --- РЕГИСТРАЦИЯ ЭКРАНОВ (GUI) ---
            MenuScreens.register(ModMenuTypes.ENDER_FURNACE_MENU.get(), EnderFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.ENDER_SAFE_MENU.get(), EnderSafeScreen::new);
            MenuScreens.register(ModMenuTypes.PRINTER_3D_MENU.get(), Printer3DScreen::new);
            // --- MADE IN HEAVEN СОБЫТИЯ ---
            MinecraftForge.EVENT_BUS.register(UniverseResetOverlay.class);
            MinecraftForge.EVENT_BUS.register(MadeInHeavenFOVHandler.class);
            MinecraftForge.EVENT_BUS.register(MadeInHeavenCameraShake.class);
            GeoArmorRenderer.registerArmorRenderer(AqualungItem.class, AqualungArmorRenderer::new);
            GeoArmorRenderer.registerArmorRenderer(com.example.zitraksmode.items.SandevistanItem.class, com.example.zitraksmode.client.armor.SandevistanArmorRenderer::new);
            GeoArmorRenderer.registerArmorRenderer(SpanishBootsItem.class, SpanishBootsArmorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.HENTAI_BLOCK.get(), HentaiBlockRenderer::new);
        });
        
    }
    
}