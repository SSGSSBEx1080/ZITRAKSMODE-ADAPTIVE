package com.example.zitraksmode.client.katana;

import com.example.zitraksmode.items.katana.KatanaItem;
import com.example.zitraksmode.items.katana.KatanaCooldownManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD для катаны - 1.19.2 версия
 */
@Mod.EventBusSubscriber(modid = "zitraksmode")
public class KatanaHUD {
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) return;
        
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        boolean holdingKatana = mainHand.getItem() instanceof KatanaItem || 
                               offHand.getItem() instanceof KatanaItem;
        
        if (!holdingKatana) return;
        
        // В 1.19.2 используем event.getPoseStack() напрямую
        PoseStack poseStack = event.getPoseStack();
        Font font = mc.font;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Cooldown ультимейта
        if (!KatanaCooldownManager.isUltimateReady(player)) {
            int remainingTicks = KatanaCooldownManager.getUltimateRemainingTicks(player);
            int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
            
            String cooldownText = "§cUltimate: " + remainingSeconds + "s";
            
            int x = screenWidth - font.width(cooldownText) - 10;
            int y = screenHeight - 40;
            
            font.draw(poseStack, cooldownText, x, y, 0xFF5555);
            
            // Прогресс бар
            int barWidth = 100;
            int barHeight = 4;
            int barX = screenWidth - barWidth - 10;
            int barY = screenHeight - 30;
            
            float progress = 1.0f - ((float) remainingTicks / KatanaCooldownManager.ULTIMATE_COOLDOWN);
            int filledWidth = (int) (barWidth * progress);
            
            fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0x80000000);
            fill(poseStack, barX, barY, barX + filledWidth, barY + barHeight, 0xFFFF5555);
            
        } else {
            String readyText = "§a✓ Ultimate Ready";
            
            int x = screenWidth - font.width(readyText) - 10;
            int y = screenHeight - 40;
            
            font.draw(poseStack, readyText, x, y, 0x55FF55);
        }
    }
    
    // Helper метод для рисования прямоугольников
    private static void fill(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        net.minecraft.client.gui.GuiComponent.fill(poseStack, x1, y1, x2, y2, color);
    }
}
