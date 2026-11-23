package com.example.zitraksmode.client;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import com.example.zitraksmode.items.JBLSpeakerItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class JBLSpeakerHUD {

    private static final int HUD_WIDTH = 140;
    private static final int HUD_HEIGHT = 35;
    private static final int PADDING = 5;

    // Бегущая строка
    private static String currentTrackName = "";
    private static int scrollOffset = 0;
    private static int scrollDelay = 0;

    // Насколько долго ждём перед стартом прокрутки
    private static final int SCROLL_DELAY_FRAMES = 60;

    // ✅ НОВОЕ: двигаем текст только раз в N кадров (примерно в 10 раз медленнее)
    private static final int SCROLL_TICK_DELAY = 20;
    private static int scrollTickCounter = 0;

    public static void render(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) return;

        ItemStack speakerStack = ItemStack.EMPTY;
        if (player.getMainHandItem().is(ModItems.JBL_SPEAKER.get())) {
            speakerStack = player.getMainHandItem();
        } else if (player.getOffhandItem().is(ModItems.JBL_SPEAKER.get())) {
            speakerStack = player.getOffhandItem();
        }

        if (speakerStack.isEmpty()) {
            scrollOffset = 0;
            scrollDelay = 0;
            scrollTickCounter = 0;
            currentTrackName = "";
            return;
        }

        // HUD держится либо на активном состоянии менеджера, либо на состоянии предмета
        boolean showHud = SpeakerAudioManager.isHudVisible() || JBLSpeakerItem.isPlaying(speakerStack);

        if (!showHud) {
            scrollOffset = 0;
            scrollDelay = 0;
            scrollTickCounter = 0;
            currentTrackName = "";
            return;
        }

        float volume = SpeakerAudioManager.isHudVisible()
                ? SpeakerAudioManager.getHudVolume()
                : JBLSpeakerItem.getVolume(speakerStack);

        String trackName = SpeakerAudioManager.isHudVisible()
                ? SpeakerAudioManager.getHudTrackName()
                : JBLSpeakerItem.getCurrentTrack(speakerStack);

        String displayName = trackName.replace(".ogg", "");

        if (!displayName.equals(currentTrackName)) {
            currentTrackName = displayName;
            scrollOffset = 0;
            scrollDelay = 0;
            scrollTickCounter = 0;
        }

        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = PADDING;
        int y = screenHeight - HUD_HEIGHT - PADDING;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GuiComponent.fill(poseStack, x, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0xAA000000);

        GuiComponent.fill(poseStack, x, y, x + HUD_WIDTH, y + 1, 0xFF00FF00);
        GuiComponent.fill(poseStack, x, y + HUD_HEIGHT - 1, x + HUD_WIDTH, y + HUD_HEIGHT, 0xFF00FF00);
        GuiComponent.fill(poseStack, x, y, x + 1, y + HUD_HEIGHT, 0xFF00FF00);
        GuiComponent.fill(poseStack, x + HUD_WIDTH - 1, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0xFF00FF00);

        int noteColor = (int)((System.currentTimeMillis() / 500) % 2) == 0 ? 0x00FF00 : 0x00FFAA;
        minecraft.font.draw(poseStack, "♫", x + 4, y + 4, noteColor);

        String volumeText = (int)(volume * 100) + "%";
        int textWidth = minecraft.font.width(volumeText);
        minecraft.font.draw(poseStack, volumeText, x + HUD_WIDTH - textWidth - 4, y + 4, 0x00FF00);

        int textAreaX = x + 16;
        int textAreaY = y + 4;
        int textAreaWidth = HUD_WIDTH - 20 - textWidth - 8;

        drawScrollingText(poseStack, minecraft, displayName, textAreaX, textAreaY, textAreaWidth);

        int barX = x + 4;
        int barY = y + 20;
        int barWidth = HUD_WIDTH - 8;
        int barHeight = 8;

        GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, 0xFF222222);

        int filledWidth = (int)(barWidth * volume);
        int color = volume > 0.66f ? 0xFF00FF00 : (volume > 0.33f ? 0xFFFFFF00 : 0xFFFF0000);

        if (filledWidth > 0) {
            GuiComponent.fill(poseStack, barX, barY, barX + filledWidth, barY + barHeight, color);
        }

        GuiComponent.fill(poseStack, barX, barY, barX + barWidth, barY + 1, 0xFF00FF00);
        GuiComponent.fill(poseStack, barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFF00FF00);
        GuiComponent.fill(poseStack, barX, barY, barX + 1, barY + barHeight, 0xFF00FF00);
        GuiComponent.fill(poseStack, barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, 0xFF00FF00);

        RenderSystem.disableBlend();
    }

    private static void drawScrollingText(PoseStack poseStack, Minecraft minecraft, String text, int x, int y, int maxWidth) {
        int textWidth = minecraft.font.width(text);

        if (textWidth <= maxWidth) {
            minecraft.font.draw(poseStack, text, x, y, 0xFFFFFF);
            scrollOffset = 0;
            scrollDelay = 0;
            scrollTickCounter = 0;
        } else {
            if (scrollDelay < SCROLL_DELAY_FRAMES) {
                scrollDelay++;
                minecraft.font.draw(poseStack, text, x, y, 0xFFFFFF);
            } else {
                applyScissor(minecraft, x, y, maxWidth, 10);

                minecraft.font.draw(poseStack, text, x - scrollOffset, y, 0xFFFFFF);

                // Бесшовный повтор
                if (scrollOffset > 20) {
                    minecraft.font.draw(poseStack, text, x - scrollOffset + textWidth + 30, y, 0xFFFFFF);
                }

                // ✅ ДВИГАЕМ НЕ КАЖДЫЙ КАДР, А РАЗ В 10 КАДРОВ
                scrollTickCounter++;
                if (scrollTickCounter >= SCROLL_TICK_DELAY) {
                    scrollTickCounter = 0;
                    scrollOffset += 1;
                }

                if (scrollOffset > textWidth + 30) {
                    scrollOffset = 0;
                }

                RenderSystem.disableScissor();
            }
        }
    }

    private static void applyScissor(Minecraft mc, int x, int y, int width, int height) {
        double scale = mc.getWindow().getGuiScale();
        int screenX = (int) (x * scale);
        int screenY = (int) ((mc.getWindow().getGuiScaledHeight() - (y + height)) * scale);
        int screenW = (int) (width * scale);
        int screenH = (int) (height * scale);

        RenderSystem.enableScissor(screenX, screenY, screenW, screenH);
    }
}