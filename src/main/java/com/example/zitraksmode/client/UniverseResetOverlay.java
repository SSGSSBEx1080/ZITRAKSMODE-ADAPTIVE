package com.example.zitraksmode.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UniverseResetOverlay {
    
    private static long flashStartTime = 0;
    private static final long FLASH_DURATION = 3000; // 3 секунды белой вспышки
    
    public static void triggerFlash() {
        flashStartTime = System.currentTimeMillis();
    }
    
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (flashStartTime == 0) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - flashStartTime;
        
        if (elapsed > FLASH_DURATION) {
            flashStartTime = 0;
            return;
        }
        
        // Рассчитываем альфа-канал для fade in/out эффекта
        float alpha;
        
        if (elapsed < 500) {
            // Быстрое появление (0.5 сек)
            alpha = elapsed / 500f;
        } else if (elapsed > FLASH_DURATION - 1000) {
            // Медленное исчезновение (1 сек)
            alpha = (FLASH_DURATION - elapsed) / 1000f;
        } else {
            // Полная яркость
            alpha = 1.0f;
        }
        
        // Рендерим белый экран
        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        
        // Белый прямоугольник на весь экран (используем GuiComponent.fill для 1.19.2)
        int color = ((int)(alpha * 255) << 24) | 0xFFFFFF;
        GuiComponent.fill(poseStack, 0, 0, screenWidth, screenHeight, color);
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
