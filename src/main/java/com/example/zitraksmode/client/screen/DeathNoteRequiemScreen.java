package com.example.zitraksmode.client.screen;

import com.example.zitraksmode.items.DeathNoteRequiemItem;
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

public class DeathNoteRequiemScreen extends Screen {

    private final ItemStack itemStack;
    private EditBox nameField;
    private int selectedTime = 40;
    private int selectedMethod = 0;

    private static final int BG_COLOR = 0xFF000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int HOVER_COLOR = 0xFF666666;
    private static final int BUTTON_COLOR = 0xFF333333;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_COLOR = 0xFFAAAAAA;

    public DeathNoteRequiemScreen(ItemStack stack) {
        super(Component.literal("Death Note: Requiem"));
        this.itemStack = stack;
    }

    @Override
    protected void init() {
        super.init();
        int guiWidth = 260;
        int guiHeight = 200;
        int guiLeft = (width - guiWidth) / 2;
        int guiTop = (height - guiHeight) / 2;

        nameField = new EditBox(font, guiLeft + 70, guiTop + 30, 170, 20, Component.literal(""));
        nameField.setMaxLength(16);
        nameField.setTextColor(0xFFFFFF);
        nameField.setBordered(true);
        addRenderableWidget(nameField);

        int timeY = guiTop + 70;
        addTimeButton(guiLeft + 20, timeY, 50, 18, "10 сек", 10);
        addTimeButton(guiLeft + 75, timeY, 50, 18, "30 сек", 30);
        addTimeButton(guiLeft + 130, timeY, 50, 18, "40 сек", 40);
        addTimeButton(guiLeft + 185, timeY, 50, 18, "60 сек", 60);

        int methodStartY = guiTop + 110;
        int methodSpacing = 25;
        addMethodButton(guiLeft + 20, methodStartY, 70, 20, "Казнь", 0);
        addMethodButton(guiLeft + 95, methodStartY, 70, 20, "Наковальня", 1);
        addMethodButton(guiLeft + 170, methodStartY, 70, 20, "Лава", 2);
        addMethodButton(guiLeft + 20, methodStartY + methodSpacing, 70, 20, "Падение", 3);
        addMethodButton(guiLeft + 95, methodStartY + methodSpacing, 70, 20, "Молния", 4);
        addMethodButton(guiLeft + 170, methodStartY + methodSpacing, 70, 20, "Взрыв", 5);

        addRenderableWidget(new FancyButton(
                guiLeft + (guiWidth - 120) / 2,
                guiTop + guiHeight - 30,
                120, 22,
                Component.literal("Записать"),
                btn -> commit()
        ));
    }

    private void addTimeButton(int x, int y, int w, int h, String text, int time) {
        addRenderableWidget(new SelectableButton(x, y, w, h, Component.literal(text),
                btn -> selectedTime = time, () -> selectedTime == time));
    }

    private void addMethodButton(int x, int y, int w, int h, String text, int method) {
        addRenderableWidget(new SelectableButton(x, y, w, h, Component.literal(text),
                btn -> selectedMethod = method, () -> selectedMethod == method));
    }

    private void commit() {
        String target = nameField.getValue().trim();
        if (!target.isEmpty()) {
            ModMessages.INSTANCE.sendToServer(new RequestDeathPacket(target, selectedTime, selectedMethod));

            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F)
            );

            if (Minecraft.getInstance().player != null) {
                DeathNoteRequiemItem.clearAnim(Minecraft.getInstance().player);
            }
            this.onClose();
        }
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        int guiWidth = 260;
        int guiHeight = 200;
        int guiLeft = (width - guiWidth) / 2;
        int guiTop = (height - guiHeight) / 2;

        fill(pose, guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, BG_COLOR);
        fill(pose, guiLeft, guiTop, guiLeft + guiWidth, guiTop + 2, BORDER_COLOR);
        fill(pose, guiLeft, guiTop + guiHeight - 2, guiLeft + guiWidth, guiTop + guiHeight, BORDER_COLOR);
        fill(pose, guiLeft, guiTop, guiLeft + 2, guiTop + guiHeight, BORDER_COLOR);
        fill(pose, guiLeft + guiWidth - 2, guiTop, guiLeft + guiWidth, guiTop + guiHeight, BORDER_COLOR);

        drawCenteredString(pose, font, "DEATH NOTE: REQUIEM",
                guiLeft + guiWidth / 2, guiTop + 10, TEXT_COLOR);
        drawString(pose, font, "Имя цели:", guiLeft + 10, guiTop + 35, TEXT_COLOR);
        drawString(pose, font, "Время:", guiLeft + 10, guiTop + 60, TEXT_COLOR);
        drawString(pose, font, "Метод казни:", guiLeft + 10, guiTop + 100, TEXT_COLOR);

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
            DeathNoteRequiemItem.clearAnim(Minecraft.getInstance().player);
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
            drawCenteredString(ps, DeathNoteRequiemScreen.this.font, getMessage(),
                    x + width / 2, y + (height - 8) / 2, TEXT_COLOR);
        }
    }

    private class SelectableButton extends Button {
        private final java.util.function.Supplier<Boolean> isSelected;

        public SelectableButton(int x, int y, int w, int h, Component text,
                                OnPress cb, java.util.function.Supplier<Boolean> isSelected) {
            super(x, y, w, h, text, cb);
            this.isSelected = isSelected;
        }

        @Override
        public void renderButton(PoseStack ps, int mx, int my, float t) {
            int bg = isSelected.get() ? SELECTED_COLOR : (isHoveredOrFocused() ? HOVER_COLOR : BUTTON_COLOR);
            fill(ps, x, y, x + width, y + height, bg);
            fill(ps, x, y, x + width, y + 1, BORDER_COLOR);
            fill(ps, x, y + height - 1, x + width, y + height, BORDER_COLOR);
            fill(ps, x, y, x + 1, y + height, BORDER_COLOR);
            fill(ps, x + width - 1, y, x + width, y + height, BORDER_COLOR);
            int textColor = isSelected.get() ? 0xFF000000 : TEXT_COLOR;
            drawCenteredString(ps, DeathNoteRequiemScreen.this.font, getMessage(),
                    x + width / 2, y + (height - 8) / 2, textColor);
        }
    }
}