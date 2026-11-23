package com.example.zitraksmode.client.screen;

import com.example.zitraksmode.items.DeathNoteItem;
import com.example.zitraksmode.network.ModMessages;
import com.example.zitraksmode.network.RequestDeathPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class DeathNoteScreen extends Screen {
    private final ItemStack itemStack;
    private EditBox nameField;

    private static final int BG_COLOR = 0xFF000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int HOVER_COLOR = 0xFF666666;
    private static final int BUTTON_COLOR = 0xFF333333;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public DeathNoteScreen(ItemStack stack) {
        super(Component.literal("Death Note"));
        this.itemStack = stack;
    }

    @Override
    protected void init() {
        super.init();
        int guiWidth = 220;
        int guiHeight = 120;
        int guiLeft = (width - guiWidth) / 2;
        int guiTop = (height - guiHeight) / 2;

        nameField = new EditBox(font, guiLeft + 70, guiTop + 40, 130, 20, Component.literal(""));
        nameField.setMaxLength(16);
        nameField.setTextColor(0xFFFFFF);
        nameField.setBordered(true);
        addRenderableWidget(nameField);

        addRenderableWidget(new FancyButton(
                guiLeft + (guiWidth - 100) / 2,
                guiTop + guiHeight - 35,
                100, 22,
                Component.literal("Записать"),
                btn -> commit()
        ));
    }

    private void commit() {
        String target = nameField.getValue().trim();
        if (!target.isEmpty()) {
            ModMessages.INSTANCE.sendToServer(new RequestDeathPacket(target));

            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F)
            );

            if (Minecraft.getInstance().player != null) {
                DeathNoteItem.clearAnim(Minecraft.getInstance().player);
            }

            this.onClose();
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int guiWidth = 220;
        int guiHeight = 120;
        int guiLeft = (width - guiWidth) / 2;
        int guiTop = (height - guiHeight) / 2;

        fill(pose, guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, BG_COLOR);

        fill(pose, guiLeft, guiTop, guiLeft + guiWidth, guiTop + 2, BORDER_COLOR);
        fill(pose, guiLeft, guiTop + guiHeight - 2, guiLeft + guiWidth, guiTop + guiHeight, BORDER_COLOR);
        fill(pose, guiLeft, guiTop, guiLeft + 2, guiTop + guiHeight, BORDER_COLOR);
        fill(pose, guiLeft + guiWidth - 2, guiTop, guiLeft + guiWidth, guiTop + guiHeight, BORDER_COLOR);

        drawCenteredString(pose, font, "DEATH NOTE", guiLeft + guiWidth / 2, guiTop + 10, TEXT_COLOR);
        drawString(pose, font, "Имя цели:", guiLeft + 10, guiTop + 45, TEXT_COLOR);

        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) {
            commit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (Minecraft.getInstance().player != null) {
            DeathNoteItem.clearAnim(Minecraft.getInstance().player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class FancyButton extends Button {
        public FancyButton(int x, int y, int w, int h, Component text, OnPress cb) {
            super(x, y, w, h, text, cb);
        }

        @Override
        public void renderButton(PoseStack ps, int mx, int my, float t) {
            int bg = isHoveredOrFocused() ? HOVER_COLOR : BUTTON_COLOR;
            fill(ps, x, y, x + width, y + height, bg);

            fill(ps, x, y, x + width, y + 1, BORDER_COLOR);
            fill(ps, x, y + height - 1, x + width, y + height, BORDER_COLOR);
            fill(ps, x, y, x + 1, y + height, BORDER_COLOR);
            fill(ps, x + width - 1, y, x + width, y + height, BORDER_COLOR);

            drawCenteredString(ps, DeathNoteScreen.this.font, getMessage(),
                    x + width / 2, y + (height - 8) / 2, TEXT_COLOR);
        }
    }
}