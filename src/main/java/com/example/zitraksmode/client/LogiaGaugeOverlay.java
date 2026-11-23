package com.example.zitraksmode.client;

import com.example.zitraksmode.items.LogiaCloakItem;
import com.example.zitraksmode.items.LogiaCloakType;
import com.example.zitraksmode.util.LogiaGaugeCapability;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * A compact vertical cloak gauge. It deliberately stays in the top-left corner
 * so it never covers the crosshair, hotbar, boss bars, or custom combat HUDs.
 */
public final class LogiaGaugeOverlay {
    public static final IGuiOverlay HUD = LogiaGaugeOverlay::render;

    // Near the bottom-left corner; Y is a local drawing origin translated at render time.
    private static final int X = 14;
    private static final int Y = 0;
    private static final int OUTER_W = 34;
    private static final int OUTER_H = 154;
    private static final int BAR_X = X + 11;
    private static final int BAR_Y = Y + 20;
    private static final int BAR_W = 12;
    private static final int BAR_H = 116;

    private LogiaGaugeOverlay() {
    }

    private static void render(Gui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        LogiaCloakItem cloak = LogiaCloakItem.wornBy(minecraft.player);
        if (cloak == null) return;
        LogiaCloakType type = cloak.getCloakType();

        minecraft.player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            int max = type.maxGauge;
            int charge = Mth.clamp(data.getGauge(), 0, max);
            float percent = charge / (float) max;
            float time = (minecraft.level == null ? 0.0F : minecraft.level.getGameTime()) + partialTick;

            // All HUD drawing below uses local Y=0, then moves to the bottom-left corner.
            poseStack.pushPose();
            poseStack.translate(0.0D, screenHeight - OUTER_H - 4.0D, 0.0D);

            // Soft universal shadow; all themed frames are drawn on top.
            GuiComponent.fill(poseStack, X - 3, Y - 3, X + OUTER_W + 3, Y + OUTER_H + 3, 0x90000000);
            drawTheme(poseStack, type, percent, time);

            int fillHeight = Math.round(BAR_H * percent);
            int fillTop = BAR_Y + BAR_H - fillHeight;
            drawBarInterior(poseStack, type, fillTop, fillHeight, time);

            // No cape name here by design. The value is kept tiny below the vertical bar.
            Font font = minecraft.font;
            String value = charge + "/" + max;
            int valueColor = type == LogiaCloakType.WHITEBEARD ? 0xFFF3F5FF : 0xFFFFE29A;
            font.drawShadow(poseStack, value, X + (OUTER_W - font.width(value)) / 2.0F, Y + OUTER_H - 13, valueColor);

            if (type == LogiaCloakType.KIZARU && data.getKizaruCooldown() > 0) {
                drawSmallText(font, poseStack, "СВЕТ " + ceilSeconds(data.getKizaruCooldown()) + "с", 0xFFFFF18A);
            } else if (type == LogiaCloakType.WHITEBEARD && data.getWhitebeardLockTicks() > 0) {
                drawSmallText(font, poseStack, "ЯРОСТЬ " + ceilSeconds(data.getWhitebeardLockTicks()) + "с", 0xFFF7F8FF);
            } else if (type == LogiaCloakType.WHITEBEARD && data.getWhitebeardCooldownTicks() > 0) {
                drawSmallText(font, poseStack, "РУБЕЖ " + ceilSeconds(data.getWhitebeardCooldownTicks()) + "с", 0xFFBFC9D8);
            }
            poseStack.popPose();
        });
    }

    private static void drawTheme(PoseStack poseStack, LogiaCloakType type, float percent, float time) {
        switch (type) {
            case CROCODILE -> drawCrocodileFrame(poseStack, percent, time);
            case AKAINU -> drawAkainuFrame(poseStack, percent, time);
            case AOKIJI -> drawAokijiFrame(poseStack, percent, time);
            case KIZARU -> drawKizaruFrame(poseStack, percent, time);
            case SENGOKU -> drawSengokuFrame(poseStack, percent, time);
            case WHITEBEARD -> drawWhitebeardFrame(poseStack, percent, time);
        }
    }

    private static void drawBarInterior(PoseStack poseStack, LogiaCloakType type, int fillTop, int fillHeight, float time) {
        GuiComponent.fill(poseStack, BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, 0xD0101010);
        if (fillHeight <= 0) return;

        switch (type) {
            case CROCODILE -> {
                GuiComponent.fill(poseStack, BAR_X + 2, fillTop, BAR_X + BAR_W - 2, BAR_Y + BAR_H, 0xFFE1B65D);
                int stream = BAR_Y + (int) ((time * 2.0F) % (BAR_H - 5));
                GuiComponent.fill(poseStack, BAR_X + 5, stream, BAR_X + 7, Math.min(BAR_Y + BAR_H, stream + 6), 0xFFFFE4A0);
            }
            case AKAINU -> {
                GuiComponent.fill(poseStack, BAR_X + 1, fillTop, BAR_X + BAR_W - 1, BAR_Y + BAR_H, 0xFFD9340F);
                GuiComponent.fill(poseStack, BAR_X + 3, fillTop, BAR_X + BAR_W - 3, BAR_Y + BAR_H, 0xFFFF8B18);
                int wave = (int) (Math.sin(time * 0.45F) * 2.0F);
                GuiComponent.fill(poseStack, BAR_X + 2, fillTop + 2 + wave, BAR_X + BAR_W - 2, fillTop + 4 + wave, 0xFFFFDC43);
            }
            case AOKIJI -> {
                GuiComponent.fill(poseStack, BAR_X + 1, fillTop, BAR_X + BAR_W - 1, BAR_Y + BAR_H, 0xFF78DFFF);
                GuiComponent.fill(poseStack, BAR_X + 3, fillTop, BAR_X + 6, BAR_Y + BAR_H, 0xFFDDFBFF);
                for (int i = 0; i < 5; i++) {
                    int snow = fillTop + (int) ((time * 1.7F + i * 22) % Math.max(1, fillHeight));
                    GuiComponent.fill(poseStack, BAR_X + 2 + (i % 3), snow, BAR_X + 4 + (i % 3), snow + 2, 0xFFFFFFFF);
                }
            }
            case KIZARU -> {
                GuiComponent.fill(poseStack, BAR_X + 1, fillTop, BAR_X + BAR_W - 1, BAR_Y + BAR_H, 0xFFFFC91E);
                GuiComponent.fill(poseStack, BAR_X + 4, fillTop, BAR_X + 7, BAR_Y + BAR_H, 0xFFFFFFB5);
                int flash = (int) ((Math.sin(time * 0.6F) + 1.0F) * 2.0F);
                GuiComponent.fill(poseStack, BAR_X + 1, fillTop + flash, BAR_X + BAR_W - 1, fillTop + flash + 2, 0xFFFFF7A3);
            }
            case SENGOKU -> {
                GuiComponent.fill(poseStack, BAR_X + 1, fillTop, BAR_X + BAR_W - 1, BAR_Y + BAR_H, 0xFFD08A00);
                GuiComponent.fill(poseStack, BAR_X + 3, fillTop, BAR_X + 7, BAR_Y + BAR_H, 0xFFFFE572);
                GuiComponent.fill(poseStack, BAR_X + 8, fillTop, BAR_X + 10, BAR_Y + BAR_H, 0xFF8A4A00);
            }
            case WHITEBEARD -> {
                GuiComponent.fill(poseStack, BAR_X + 1, fillTop, BAR_X + BAR_W - 1, BAR_Y + BAR_H, 0xFFE7ECF5);
                GuiComponent.fill(poseStack, BAR_X + 3, fillTop, BAR_X + 5, BAR_Y + BAR_H, 0xFFFFFFFF);
                GuiComponent.fill(poseStack, BAR_X + 8, fillTop, BAR_X + 10, BAR_Y + BAR_H, 0xFF8C97A8);
            }
        }
    }

    /** Sand clock: tapered brass frame, falling grains and a small lower bulb. */
    private static void drawCrocodileFrame(PoseStack p, float percent, float time) {
        GuiComponent.fill(p, X + 4, Y + 3, X + OUTER_W - 4, Y + 6, 0xFF9A6824);
        GuiComponent.fill(p, X + 5, Y + 7, X + OUTER_W - 5, Y + 9, 0xFFFFD77B);
        GuiComponent.fill(p, X + 6, BAR_Y - 3, X + 9, BAR_Y + BAR_H + 4, 0xFF956126);
        GuiComponent.fill(p, X + OUTER_W - 9, BAR_Y - 3, X + OUTER_W - 6, BAR_Y + BAR_H + 4, 0xFF956126);
        GuiComponent.fill(p, X + 7, BAR_Y - 1, X + OUTER_W - 7, BAR_Y + 1, 0xFFFFD77B);
        GuiComponent.fill(p, X + 5, BAR_Y + BAR_H + 2, X + OUTER_W - 5, BAR_Y + BAR_H + 5, 0xFF9A6824);
        int grainY = BAR_Y + (int) ((time * 1.7F) % BAR_H);
        GuiComponent.fill(p, X + OUTER_W / 2 - 1, grainY, X + OUTER_W / 2 + 1, grainY + 5, 0xFFFFDEA0);
    }

    /** Molten basalt frame with animated lava bubbles and dripping drops. */
    private static void drawAkainuFrame(PoseStack p, float percent, float time) {
        GuiComponent.fill(p, X + 3, Y + 3, X + OUTER_W - 3, Y + OUTER_H - 16, 0xFF280500);
        GuiComponent.fill(p, X + 5, Y + 5, X + OUTER_W - 5, Y + OUTER_H - 18, 0xFF70200C);
        GuiComponent.fill(p, X + 7, BAR_Y - 3, X + 9, BAR_Y + BAR_H + 3, 0xFFFF5920);
        GuiComponent.fill(p, X + OUTER_W - 9, BAR_Y - 3, X + OUTER_W - 7, BAR_Y + BAR_H + 3, 0xFFFF5920);
        int bubbleA = BAR_Y + 10 + (int) ((time * 1.5F) % 34);
        int bubbleB = BAR_Y + 55 + (int) ((time * 1.1F) % 38);
        GuiComponent.fill(p, X + 5, bubbleA, X + 8, bubbleA + 3, 0xFFFFCA38);
        GuiComponent.fill(p, X + OUTER_W - 8, bubbleB, X + OUTER_W - 5, bubbleB + 4, 0xFFFF7D18);
        int drip = Y + 8 + (int) ((time * 1.2F) % 12);
        GuiComponent.fill(p, X + OUTER_W - 5, drip, X + OUTER_W - 3, drip + 5, 0xFFFF6B1A);
    }

    /** Blue crystal and snow frame. */
    private static void drawAokijiFrame(PoseStack p, float percent, float time) {
        GuiComponent.fill(p, X + 3, Y + 3, X + OUTER_W - 3, Y + OUTER_H - 16, 0xFF153A61);
        GuiComponent.fill(p, X + 5, Y + 5, X + OUTER_W - 5, Y + OUTER_H - 18, 0xFF68CDEE);
        GuiComponent.fill(p, X + 7, BAR_Y - 4, X + 10, BAR_Y + BAR_H + 3, 0xFFD6FAFF);
        GuiComponent.fill(p, X + OUTER_W - 10, BAR_Y - 4, X + OUTER_W - 7, BAR_Y + BAR_H + 3, 0xFFD6FAFF);
        // Icicles at the lower edge.
        GuiComponent.fill(p, X + 4, Y + OUTER_H - 21, X + 7, Y + OUTER_H - 13, 0xFFB7F4FF);
        GuiComponent.fill(p, X + 12, Y + OUTER_H - 19, X + 15, Y + OUTER_H - 11, 0xFFB7F4FF);
        GuiComponent.fill(p, X + 23, Y + OUTER_H - 21, X + 26, Y + OUTER_H - 14, 0xFFB7F4FF);
        for (int i = 0; i < 4; i++) {
            int snowY = Y + 7 + (int) ((time * 1.1F + i * 29) % 112);
            int snowX = X + 4 + ((i * 7) % 24);
            GuiComponent.fill(p, snowX, snowY, snowX + 2, snowY + 2, 0xFFFFFFFF);
        }
    }

    /** Clean gold-white light frame with pulsing rays and flashes. */
    private static void drawKizaruFrame(PoseStack p, float percent, float time) {
        int pulse = (int) ((Math.sin(time * 0.35F) + 1.0F) * 20.0F);
        GuiComponent.fill(p, X + 3, Y + 3, X + OUTER_W - 3, Y + OUTER_H - 16, 0xFF6A4B00);
        GuiComponent.fill(p, X + 5, Y + 5, X + OUTER_W - 5, Y + OUTER_H - 18, 0xFFFFD32E);
        GuiComponent.fill(p, X + 7, BAR_Y - 4, X + 10, BAR_Y + BAR_H + 3, 0xFFFFFF9A);
        GuiComponent.fill(p, X + OUTER_W - 10, BAR_Y - 4, X + OUTER_W - 7, BAR_Y + BAR_H + 3, 0xFFFFFF9A);
        GuiComponent.fill(p, X + OUTER_W / 2 - 2, Y + 2, X + OUTER_W / 2 + 2, Y + 11, 0xFFFFFFFF);
        GuiComponent.fill(p, X + 4 - pulse / 20, Y + 6, X + OUTER_W - 4 + pulse / 20, Y + 8, 0xFFFFFFAF);
        GuiComponent.fill(p, X + 1, Y + 12, X + 5, Y + 14, 0xFFFFE85B);
        GuiComponent.fill(p, X + OUTER_W - 5, Y + 12, X + OUTER_W - 1, Y + 14, 0xFFFFE85B);
    }

    /** Heavy pure-gold frame with a small halo / lotus motif for Buddha Sengoku. */
    private static void drawSengokuFrame(PoseStack p, float percent, float time) {
        GuiComponent.fill(p, X + 2, Y + 2, X + OUTER_W - 2, Y + OUTER_H - 16, 0xFF704000);
        GuiComponent.fill(p, X + 4, Y + 4, X + OUTER_W - 4, Y + OUTER_H - 18, 0xFFFFC92D);
        GuiComponent.fill(p, X + 6, Y + 6, X + OUTER_W - 6, Y + OUTER_H - 20, 0xFF9F6200);
        GuiComponent.fill(p, X + 8, BAR_Y - 3, X + 10, BAR_Y + BAR_H + 3, 0xFFFFE979);
        GuiComponent.fill(p, X + OUTER_W - 10, BAR_Y - 3, X + OUTER_W - 8, BAR_Y + BAR_H + 3, 0xFFFFE979);
        // Halo with eight small gold rays.
        int cx = X + OUTER_W / 2;
        int cy = Y + 11;
        GuiComponent.fill(p, cx - 2, cy - 8, cx + 2, cy - 4, 0xFFFFF1B5);
        GuiComponent.fill(p, cx - 2, cy + 4, cx + 2, cy + 8, 0xFFFFF1B5);
        GuiComponent.fill(p, cx - 8, cy - 2, cx - 4, cy + 2, 0xFFFFF1B5);
        GuiComponent.fill(p, cx + 4, cy - 2, cx + 8, cy + 2, 0xFFFFF1B5);
        GuiComponent.fill(p, cx - 3, cy - 3, cx + 3, cy + 3, 0xFFFFFFFF);
        int shineY = BAR_Y + (int) ((time * 1.2F) % BAR_H);
        GuiComponent.fill(p, X + 5, shineY, X + OUTER_W - 5, shineY + 1, 0x88FFFFFF);
    }

    /** White captain's frame: silver metal, quake cracks and drifting white dust. */
    private static void drawWhitebeardFrame(PoseStack p, float percent, float time) {
        GuiComponent.fill(p, X + 2, Y + 2, X + OUTER_W - 2, Y + OUTER_H - 16, 0xFF333947);
        GuiComponent.fill(p, X + 4, Y + 4, X + OUTER_W - 4, Y + OUTER_H - 18, 0xFFBEC8D8);
        GuiComponent.fill(p, X + 6, Y + 6, X + OUTER_W - 6, Y + OUTER_H - 20, 0xFF5D6777);
        GuiComponent.fill(p, X + 8, BAR_Y - 3, X + 10, BAR_Y + BAR_H + 3, 0xFFFFFFFF);
        GuiComponent.fill(p, X + OUTER_W - 10, BAR_Y - 3, X + OUTER_W - 8, BAR_Y + BAR_H + 3, 0xFFFFFFFF);
        // Two permanent crack lines around the frame.
        GuiComponent.fill(p, X + 4, Y + 25, X + 8, Y + 27, 0xFF4B5362);
        GuiComponent.fill(p, X + 7, Y + 27, X + 10, Y + 32, 0xFF4B5362);
        GuiComponent.fill(p, X + OUTER_W - 9, Y + 62, X + OUTER_W - 5, Y + 64, 0xFF4B5362);
        GuiComponent.fill(p, X + OUTER_W - 12, Y + 64, X + OUTER_W - 9, Y + 70, 0xFF4B5362);
        int pulse = (int) ((Math.sin(time * 0.5F) + 1.0F) * 2.0F);
        GuiComponent.fill(p, X + 5, Y + 8 + pulse, X + OUTER_W - 5, Y + 10 + pulse, 0x88FFFFFF);
    }

    private static int ceilSeconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    private static void drawSmallText(Font font, PoseStack poseStack, String text, int color) {
        font.drawShadow(poseStack, text, X + OUTER_W + 3, Y + OUTER_H - 22, color);
    }
}
