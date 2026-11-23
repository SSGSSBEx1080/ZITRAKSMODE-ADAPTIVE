package com.example.zitraksmode.client.gui.screens;

import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.items.LabubuLootTable;
import com.example.zitraksmode.items.LabubuRarity;
import com.example.zitraksmode.items.LabubuType;
import com.example.zitraksmode.network.labubu.ClaimLabubuCaseRewardPacket;
import com.example.zitraksmode.network.labubu.LabubuCasePackets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LabubuCaseScreen extends Screen {

    private static final int SPIN_TICKS = 100;      // 5 секунд
    private static final int REVEAL_TICKS = 20;     // 1 секунда показа
    private static final int SLOWDOWN_TICKS = 30;   // последние 1.5 секунды

    private static final int SLOT_W = 78;
    private static final int SLOT_H = 104;
    private static final int SLOT_SPACING = 86;

    private static final int EXTRA_SCROLL_SLOTS = 28;

    // Размер предметов в слотах
    private static final float SLOT_ITEM_SCALE = 2.4F;

    // Финальная фигурка: уменьшено на 20% относительно прошлой версии
    private static final float WINNER_MAX_SCALE = 10.0F;

    private final List<LabubuType> sequence;
    private final int winnerIndex;

    private int ticks;
    private boolean rewardClaimed;

    public LabubuCaseScreen(List<LabubuType> sequence, int winnerIndex) {
        super(Component.literal("Labubu Case"));
        this.sequence = sequence;
        this.winnerIndex = winnerIndex;
    }

    @Override
    protected void init() {
        super.init();

        this.minecraft.getSoundManager().play(
                new SimpleSoundInstance(
                        ModSounds.CASE_BATTLE.get(),
                        SoundSource.MASTER,
                        2.25F,
                        1.0F,
                        RandomSource.create(),
                        0,
                        0.0D, 0.0D
                )
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;

        if (!rewardClaimed && ticks >= SPIN_TICKS + REVEAL_TICKS) {
            rewardClaimed = true;
            LabubuCasePackets.sendToServer(new ClaimLabubuCaseRewardPacket());

            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int screenW = this.width;
        int screenH = this.height;

        // Полупрозрачный фон
        GuiComponent.fill(poseStack, 0, 0, screenW, screenH, 0x66000000);

        int reelW = Math.min(screenW - 50, 1200);
        int reelH = 136;
        int reelX = (screenW - reelW) / 2;
        int reelY = screenH / 2 - reelH / 2 - 6;

        // Главная панель
        fillGradient(poseStack, reelX, reelY, reelX + reelW, reelY + reelH, 0x88171721, 0xAA101018);

        // Тени
        GuiComponent.fill(poseStack, reelX - 3, reelY - 3, reelX + reelW + 3, reelY, 0x44000000);
        GuiComponent.fill(poseStack, reelX - 3, reelY + reelH, reelX + reelW + 3, reelY + reelH + 3, 0x44000000);
        GuiComponent.fill(poseStack, reelX - 3, reelY, reelX, reelY + reelH, 0x44000000);
        GuiComponent.fill(poseStack, reelX + reelW, reelY, reelX + reelW + 3, reelY + reelH, 0x44000000);

        // Верхний глянец
        fillGradient(poseStack, reelX + 1, reelY + 1, reelX + reelW - 1, reelY + 28, 0x35FFFFFF, 0x00000000);

        drawCenteredString(poseStack, this.font, "LABUBU CASE", screenW / 2, reelY - 20, 0xFFF5E4C5);

        int centerLineX = screenW / 2;
        int slotY = reelY + 15;

        double finalOffset = winnerIndex * SLOT_SPACING - (reelW / 2.0 - SLOT_W / 2.0);
        double startOffset = finalOffset + EXTRA_SCROLL_SLOTS * SLOT_SPACING;

        double t = Math.min(1.0, ticks / (double) SPIN_TICKS);
        double currentOffset = computeOffset(startOffset, finalOffset, t);

        for (int i = 0; i < sequence.size(); i++) {
            LabubuType type = sequence.get(i);

            int x = (int) (reelX + i * SLOT_SPACING - currentOffset);
            if (x + SLOT_W < reelX || x > reelX + reelW) {
                continue;
            }

            drawLabubuSlot(poseStack, x, slotY, type);
        }

        // Блики
        drawShine(poseStack, reelX, reelY, reelW, reelH);

        // Красная центральная линия
        drawCenterMarker(poseStack, centerLineX, reelY, reelH);

        if (ticks < SPIN_TICKS) {
            drawCenteredString(poseStack, this.font, "Открытие...", screenW / 2, reelY + reelH + 12, 0xFFFFFFFF);
        } else {
            LabubuType winner = sequence.get(winnerIndex);
            int winnerSlotCenterX = centerLineX;
            int winnerSlotCenterY = slotY + 34;
            drawWinnerReveal(poseStack, winner, winnerSlotCenterX, winnerSlotCenterY, screenW / 2, screenH / 2 - 170);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private double computeOffset(double startOffset, double finalOffset, double t) {
        double fastPhase = (SPIN_TICKS - SLOWDOWN_TICKS) / (double) SPIN_TICKS; // 0.7
        double fastDistanceRatio = 0.90;

        if (t <= fastPhase) {
            double lt = t / fastPhase;
            double phaseEnd = startOffset + (finalOffset - startOffset) * fastDistanceRatio;
            return Mth.lerp((float) lt, (float) startOffset, (float) phaseEnd);
        } else {
            double phaseStart = startOffset + (finalOffset - startOffset) * fastDistanceRatio;
            double lt = (t - fastPhase) / (1.0 - fastPhase);
            double eased = 1.0 - Math.pow(2.0, -10.0 * lt);
            return Mth.lerp((float) eased, (float) phaseStart, (float) finalOffset);
        }
    }

    private void drawCenterMarker(PoseStack poseStack, int x, int reelY, int reelH) {
        GuiComponent.fill(poseStack, x - 4, reelY + 6, x + 4, reelY + reelH - 6, 0x33FF2A2A);
        GuiComponent.fill(poseStack, x - 2, reelY + 1, x + 2, reelY + reelH - 1, 0xFFFF2A2A);
        GuiComponent.fill(poseStack, x - 1, reelY + 1, x + 1, reelY + reelH - 1, 0xFFFFE8E8);
    }

    private void drawLabubuSlot(PoseStack poseStack, int x, int y, LabubuType type) {
        int bg = rarityBackground(type.rarity);
        int border = rarityBorder(type.rarity);

        GuiComponent.fill(poseStack, x + 2, y + 2, x + SLOT_W + 2, y + SLOT_H + 2, 0x33000000);
        GuiComponent.fill(poseStack, x, y, x + SLOT_W, y + SLOT_H, bg);
        fillGradient(poseStack, x, y, x + SLOT_W, y + 24, 0x22FFFFFF, 0x00000000);

        GuiComponent.fill(poseStack, x, y, x + SLOT_W, y + 2, border);
        GuiComponent.fill(poseStack, x, y + SLOT_H - 2, x + SLOT_W, y + SLOT_H, border);
        GuiComponent.fill(poseStack, x, y, x + 2, y + SLOT_H, border);
        GuiComponent.fill(poseStack, x + SLOT_W - 2, y, x + SLOT_W, y + SLOT_H, border);

        ItemStack stack = LabubuLootTable.getFigurineStack(type);

        float centerX = x + SLOT_W / 2.0F;
        float centerY = y + 42.0F;
        renderItemModelCentered(poseStack, stack, centerX, centerY, SLOT_ITEM_SCALE);

        drawCenteredString(poseStack, this.font, rarityText(type.rarity), x + SLOT_W / 2, y + 81, border);
    }

    private void drawWinnerReveal(PoseStack poseStack, LabubuType winner, int fromCenterX, int fromCenterY, int centerX, int textY) {
        float t = Mth.clamp((ticks - SPIN_TICKS) / (float) REVEAL_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (float) Math.pow(1.0F - t, 4.0F);

        ItemStack winnerStack = LabubuLootTable.getFigurineStack(winner);
        int color = rarityBorder(winner.rarity);

        String name = winnerStack.getHoverName().getString();
        String rarity = rarityText(winner.rarity);

        float toCenterX = centerX;
        float toCenterY = this.height / 2.0F + 12.0F;

        float drawCenterX = Mth.lerp(eased, fromCenterX, toCenterX);
        float drawCenterY = Mth.lerp(eased, fromCenterY, toCenterY);

        float scale = Mth.lerp(eased, SLOT_ITEM_SCALE, WINNER_MAX_SCALE);

        drawGlow(poseStack, (int) toCenterX, (int) toCenterY, winner.rarity, eased);

        // Имя сверху
        drawFancyCenteredText(poseStack, "§l" + name, centerX, (int) (toCenterY - 90), 0xFFFFFFFF);

        // Редкость снизу
        drawFancyCenteredText(poseStack, "§l" + rarity, centerX, (int) (toCenterY + 100), color);

        renderItemModelCentered(poseStack, winnerStack, drawCenterX, drawCenterY, scale);
    }

    private void drawFancyCenteredText(PoseStack poseStack, String text, int centerX, int y, int color) {
        int width = this.font.width(text);
        this.font.drawShadow(poseStack, text, centerX - width / 2.0F, y, color);
    }

    private void renderItemModelCentered(PoseStack poseStack, ItemStack stack, float centerX, float centerY, float scale) {
        poseStack.pushPose();

        poseStack.translate(centerX, centerY, 300.0F);
        poseStack.scale(scale * 16.0F, -scale * 16.0F, scale * 16.0F);

        BakedModel bakedModel = this.minecraft.getItemRenderer().getModel(stack, null, null, 0);
        MultiBufferSource.BufferSource buffer = this.minecraft.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();

        this.minecraft.getItemRenderer().render(
                stack,
                ItemTransforms.TransformType.GUI,
                false,
                poseStack,
                buffer,
                15728880,
                OverlayTexture.NO_OVERLAY,
                bakedModel
        );

        buffer.endBatch();
        poseStack.popPose();
    }

    private void drawGlow(PoseStack poseStack, int centerX, int centerY, LabubuRarity rarity, float intensity) {
        int glow = rarityGlow(rarity);

        int a1 = (int) (0x18 * intensity);
        int a2 = (int) (0x36 * intensity);
        int a3 = (int) (0x72 * intensity);

        int c1 = (a1 << 24) | (glow & 0x00FFFFFF);
        int c2 = (a2 << 24) | (glow & 0x00FFFFFF);
        int c3 = (a3 << 24) | (glow & 0x00FFFFFF);

        GuiComponent.fill(poseStack, centerX - 180, centerY - 180, centerX + 180, centerY + 180, c1);
        GuiComponent.fill(poseStack, centerX - 120, centerY - 120, centerX + 120, centerY + 120, c2);
        GuiComponent.fill(poseStack, centerX - 70, centerY - 70, centerX + 70, centerY + 70, c3);
    }

    private void drawShine(PoseStack poseStack, int x, int y, int w, int h) {
        int localTick = ticks % 36;
        int shineX = x + (int) ((w + 180) * (localTick / 36.0)) - 90;

        GuiComponent.fill(poseStack, shineX - 18, y + 6, shineX - 8, y + h - 6, 0x0FFFFFFF);
        GuiComponent.fill(poseStack, shineX - 8, y + 6, shineX + 6, y + h - 6, 0x18FFFFFF);
        GuiComponent.fill(poseStack, shineX + 6, y + 6, shineX + 14, y + h - 6, 0x0FFFFFFF);
    }

    private int rarityBackground(LabubuRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xAA343434;
            case UNCOMMON -> 0xAA1C4B1C;
            case RARE -> 0xAA1B2E66;
            case EPIC -> 0xAA4B1D66;
            case LEGENDARY -> 0xAA6C5512;
        };
    }

    private int rarityBorder(LabubuRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFE0E0E0;
            case UNCOMMON -> 0xFF55FF55;
            case RARE -> 0xFF5599FF;
            case EPIC -> 0xFFFF55FF;
            case LEGENDARY -> 0xFFFFD54A;
        };
    }

    private int rarityGlow(LabubuRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFFFFFFF;
            case UNCOMMON -> 0xFF55FF55;
            case RARE -> 0xFF5599FF;
            case EPIC -> 0xFFFF55FF;
            case LEGENDARY -> 0xFFFFD54A;
        };
    }

    private String rarityText(LabubuRarity rarity) {
        return switch (rarity) {
            case COMMON -> "ОБЫЧНАЯ";
            case UNCOMMON -> "НЕОБЫЧНАЯ";
            case RARE -> "РЕДКАЯ";
            case EPIC -> "ЭПИЧЕСКАЯ";
            case LEGENDARY -> "ЛЕГЕНДАРНАЯ";
        };
    }
}