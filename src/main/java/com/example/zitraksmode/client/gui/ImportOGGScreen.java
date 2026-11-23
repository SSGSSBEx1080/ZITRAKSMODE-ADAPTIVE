package com.example.zitraksmode.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.File;

/**
 * Экран для импорта OGG файла - с текстовым полем для пути!
 */
public class ImportOGGScreen extends Screen {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Screen previousScreen;
    private final Runnable onImportComplete;
    
    private EditBox pathField;
    private Button importButton;
    private Button cancelButton;
    
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    
    public ImportOGGScreen(Screen previousScreen, Runnable onImportComplete) {
        super(Component.literal("Import OGG File"));
        this.previousScreen = previousScreen;
        this.onImportComplete = onImportComplete;
    }
    
    @Override
    protected void init() {
        super.init();
        
        LOGGER.info("════════════════════════════════════════");
        LOGGER.info("🎵 IMPORT OGG SCREEN OPENED");
        
        // Текстовое поле для пути
        this.pathField = new EditBox(
            this.font,
            this.width / 2 - 200,  // x
            this.height / 2 - 20,  // y
            400,                    // width
            20,                     // height
            Component.literal("File path")
        );
        this.pathField.setMaxLength(500);
        this.pathField.setValue("C:\\");
        this.addWidget(this.pathField);
        
        // Кнопка Import
        this.importButton = this.addRenderableWidget(
            new Button(
                this.width / 2 - 100,
                this.height / 2 + 20,
                95,
                20,
                Component.literal("Import"),
                button -> doImport()
            )
        );
        
        // Кнопка Cancel
        this.cancelButton = this.addRenderableWidget(
            new Button(
                this.width / 2 + 5,
                this.height / 2 + 20,
                95,
                20,
                Component.literal("Cancel"),
                button -> this.minecraft.setScreen(previousScreen)
            )
        );
        
        LOGGER.info("✅ Import screen initialized");
        LOGGER.info("════════════════════════════════════════");
    }
    
    private void doImport() {
        String path = this.pathField.getValue().trim();
        
        LOGGER.info("════════════════════════════════════════");
        LOGGER.info("🎵 IMPORT CLICKED!");
        LOGGER.info("Path: {}", path);
        
        if (path.isEmpty()) {
            setStatus("§cPlease enter a file path!", 0xFF5555);
            LOGGER.warn("Empty path!");
            return;
        }
        
        File file = new File(path);
        
        if (!file.exists()) {
            setStatus("§cFile not found: " + path, 0xFF5555);
            LOGGER.error("File not found: {}", path);
            return;
        }
        
        if (!file.isFile()) {
            setStatus("§cNot a file: " + path, 0xFF5555);
            LOGGER.error("Not a file: {}", path);
            return;
        }
        
        if (!file.getName().toLowerCase().endsWith(".ogg")) {
            setStatus("§cNot an OGG file! Use Audacity to convert MP3 → OGG", 0xFF5555);
            LOGGER.error("Not an OGG file: {}", file.getName());
            return;
        }
        
        // Копируем файл
        try {
            File musicFolder = new File(
                this.minecraft.gameDirectory,
                "zitraksmode/music"
            );
            musicFolder.mkdirs();
            
            String fileName = file.getName();
            File targetFile = new File(musicFolder, fileName);
            
            // Если файл существует - добавляем номер
            int counter = 1;
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            while (targetFile.exists()) {
                targetFile = new File(musicFolder, nameWithoutExt + "_" + counter + ".ogg");
                counter++;
            }
            
            LOGGER.info("Copying from: {}", file.getAbsolutePath());
            LOGGER.info("Copying to: {}", targetFile.getAbsolutePath());
            
            java.nio.file.Files.copy(file.toPath(), targetFile.toPath());
            
            LOGGER.info("✅ IMPORT SUCCESSFUL!");
            LOGGER.info("Target: {}", targetFile.getName());
            
            setStatus("§aImported: " + targetFile.getName(), 0x55FF55);
            
            // Обновляем список
            if (onImportComplete != null) {
                onImportComplete.run();
            }
            
            // Закрываем экран через 1.5 секунды
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    this.minecraft.execute(() -> this.minecraft.setScreen(previousScreen));
                } catch (InterruptedException e) {
                    LOGGER.warn("Sleep interrupted", e);
                }
            }).start();
            
        } catch (Exception e) {
            LOGGER.error("❌ IMPORT FAILED!", e);
            setStatus("§cError: " + e.getMessage(), 0xFF5555);
        }
        
        LOGGER.info("════════════════════════════════════════");
    }
    
    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }
    
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        
        // Заголовок
        drawCenteredString(poseStack, this.font, this.title, 
            this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        
        // Инструкция
        drawCenteredString(poseStack, this.font, 
            Component.literal("§7Enter full path to OGG file:"), 
            this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        
        // Пример
        drawCenteredString(poseStack, this.font, 
            Component.literal("§8Example: C:\\Music\\song.ogg"), 
            this.width / 2, this.height / 2 - 25, 0xFFFFFF);
        
        // Текстовое поле
        this.pathField.render(poseStack, mouseX, mouseY, partialTick);
        
        // Статус
        if (!statusMessage.isEmpty()) {
            drawCenteredString(poseStack, this.font, 
                Component.literal(statusMessage), 
                this.width / 2, this.height / 2 + 50, statusColor);
        }
        
        // Подсказка про конвертацию
        drawCenteredString(poseStack, this.font, 
            Component.literal("§7To convert MP3 → OGG: Use Audacity (Export → OGG)"), 
            this.width / 2, this.height / 2 + 70, 0xFFFFFF);
        
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC - закрыть
        if (keyCode == 256) { // ESC
            this.minecraft.setScreen(previousScreen);
            return true;
        }
        
        // Enter - импорт
        if (keyCode == 257 && this.pathField.isFocused()) { // Enter
            doImport();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
