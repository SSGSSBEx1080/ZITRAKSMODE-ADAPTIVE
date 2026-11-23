package com.example.zitraksmode.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.File;

/**
 * Кнопка которая открывает папку zitraksmode/music/ в проводнике!
 * ИСПРАВЛЕНО: Запускает explorer.exe напрямую!
 */
public class OpenFolderButton extends Button {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public OpenFolderButton(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("📂 Open Folder"), 
              button -> ((OpenFolderButton)button).openFolder());
    }
    
    public OpenFolderButton(int x, int y) {
        this(x, y, 100, 20);
    }
    
    private void openFolder() {
        LOGGER.info("════════════════════════════════════════");
        LOGGER.info("📂 OPEN FOLDER CLICKED!");
        
        new Thread(() -> {
            try {
                File musicFolder = new File(
                    net.minecraft.client.Minecraft.getInstance().gameDirectory,
                    "zitraksmode/music"
                );
                
                // Создаём папку если её нет
                if (!musicFolder.exists()) {
                    LOGGER.info("Creating folder: {}", musicFolder.getAbsolutePath());
                    musicFolder.mkdirs();
                }
                
                String path = musicFolder.getAbsolutePath();
                LOGGER.info("Opening folder: {}", path);
                
                // ОПРЕДЕЛЯЕМ ОС
                String os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("win")) {
                    // WINDOWS: explorer.exe
                    LOGGER.info("OS: Windows - using explorer.exe");
                    Runtime.getRuntime().exec("explorer.exe \"" + path + "\"");
                    
                } else if (os.contains("mac")) {
                    // MAC: open
                    LOGGER.info("OS: Mac - using open");
                    Runtime.getRuntime().exec("open \"" + path + "\"");
                    
                } else if (os.contains("nix") || os.contains("nux")) {
                    // LINUX: xdg-open
                    LOGGER.info("OS: Linux - using xdg-open");
                    Runtime.getRuntime().exec("xdg-open \"" + path + "\"");
                    
                } else {
                    LOGGER.error("❌ Unknown OS: {}", os);
                    showMessage("§cUnknown OS!\n\nManually open: " + path);
                    return;
                }
                
                LOGGER.info("✅ Folder opened!");
                showMessage("§aFolder opened!\n\n" + path + "\n\nAdd OGG files, then click Refresh!");
                
            } catch (Exception e) {
                LOGGER.error("❌ Failed to open folder!", e);
                
                File musicFolder = new File(
                    net.minecraft.client.Minecraft.getInstance().gameDirectory,
                    "zitraksmode/music"
                );
                
                showMessage("§cCannot open folder automatically!\n\n§7Manually open:\n" + musicFolder.getAbsolutePath());
            }
        }, "OpenFolder").start();
        
        LOGGER.info("════════════════════════════════════════");
    }
    
    private void showMessage(String message) {
        try {
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                Component.literal(message), 
                false
            );
        } catch (Exception e) {
            LOGGER.warn("Could not show message", e);
        }
    }
    
    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderButton(poseStack, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onPress() {
        openFolder();
    }
}
