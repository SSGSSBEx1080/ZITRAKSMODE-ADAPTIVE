package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.RenameTrackPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SpeakerRenameScreen extends Screen {

    private final Screen parent;
    private final SpeakerTrackMeta track;
    private EditBox nameBox;

    public SpeakerRenameScreen(Screen parent, SpeakerTrackMeta track) {
        super(Component.literal("Переименовать трек"));
        this.parent = parent;
        this.track = track;
    }

    @Override
    protected void init() {
        super.init();

        this.nameBox = new EditBox(this.font, this.width / 2 - 110, this.height / 2 - 12, 220, 20, Component.literal("Название"));
        this.nameBox.setMaxLength(128);
        this.nameBox.setValue(track.getTitle());
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        this.addRenderableWidget(new Button(this.width / 2 - 110, this.height / 2 + 18, 106, 20, Component.literal("Сохранить"), button -> save()));
        this.addRenderableWidget(new Button(this.width / 2 + 4, this.height / 2 + 18, 106, 20, Component.literal("Отмена"), button -> onClose()));
    }

    private void save() {
        String newTitle = this.nameBox.getValue().trim();
        if (!newTitle.isBlank()) {
            PacketHandler.sendToServer(new RenameTrackPacket(track.getTrackId(), newTitle));
        }
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameBox.keyPressed(keyCode, scanCode, modifiers) || this.nameBox.canConsumeInput()) {
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.nameBox.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, this.height / 2 - 38, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("§7" + track.getTitle()), this.width / 2, this.height / 2 - 26, 0xAAAAAA);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.nameBox.render(poseStack, mouseX, mouseY, partialTick);
    }
}
