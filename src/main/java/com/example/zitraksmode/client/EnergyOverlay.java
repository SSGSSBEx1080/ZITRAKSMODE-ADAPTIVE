package com.example.zitraksmode.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.abilities.AbilitySystem;

public class EnergyOverlay {
    private static final ResourceLocation ENERGY_BAR = new ResourceLocation(ZitraksMode.MODID, "textures/gui/bars.png");
    
    public static final IGuiOverlay HUD_ENERGY = (gui, poseStack, partialTick, width, height) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        // Проверяем, что надет полный сет брони
        if (AbilitySystem.isWearingFullTntArmor(minecraft.player)) {
            float energy = AbilitySystem.getEnergyLevel(minecraft.player);
            boolean ready = AbilitySystem.isReadyToExplode(minecraft.player);
            
            int x = width / 2 - 91;
            int y = height - 75;
            int barWidth = 182;
            int progressWidth = (int)((energy / 100.0f) * barWidth);
            
            // Рисуем фон полоски
            RenderSystem.setShaderTexture(0, ENERGY_BAR);
            GuiComponent.blit(poseStack, x, y, 0, 0, barWidth, 5, barWidth, 10);
            
            // Рисуем заполнение (меняем цвет в зависимости от готовности)
            if (ready) {
                RenderSystem.setShaderColor(0.0f, 1.0f, 0.0f, 1.0f); // Зеленый когда готово
            } else {
                RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 1.0f); // Красный
            }
            GuiComponent.blit(poseStack, x, y, 0, 5, progressWidth, 5, barWidth, 10);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Текст с процентом
            String text = ready ? "ГОТОВО! (R)" : String.format("%.0f%%", energy);
            int textWidth = minecraft.font.width(text);
            int textColor = ready ? 0xFF00FF00 : 0xFFFFFFFF;
            minecraft.font.drawShadow(poseStack, text, x + barWidth / 2 - textWidth / 2, y - 10, textColor);
        }
    };
}