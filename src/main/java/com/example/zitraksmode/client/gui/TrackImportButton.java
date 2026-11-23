package com.example.zitraksmode.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Кнопка для импорта OGG - открывает экран ввода пути!
 */
public class TrackImportButton extends Button {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Screen currentScreen;
    private final Runnable onImportComplete;
    
    public TrackImportButton(int x, int y, int width, int height, 
                            Screen currentScreen, Runnable onImportComplete) {
        super(x, y, width, height, Component.literal("📁 Import OGG"), 
              button -> ((TrackImportButton)button).doImport());
        this.currentScreen = currentScreen;
        this.onImportComplete = onImportComplete;
    }
    
    public TrackImportButton(int x, int y, Screen currentScreen, Runnable onImportComplete) {
        this(x, y, 100, 20, currentScreen, onImportComplete);
    }
    
    private void doImport() {
        LOGGER.info("🎵 Import button clicked!");
        LOGGER.info("Opening Import OGG Screen...");
        
        // Открываем экран ввода пути
        Minecraft.getInstance().setScreen(
            new ImportOGGScreen(currentScreen, onImportComplete)
        );
    }
    
    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderButton(poseStack, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onPress() {
        doImport();
    }
}
