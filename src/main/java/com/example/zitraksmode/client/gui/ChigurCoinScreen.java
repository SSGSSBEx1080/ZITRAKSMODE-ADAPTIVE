package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.network.chigur.ChigurCoinChoicePacket;
import com.example.zitraksmode.network.chigur.ChigurNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The player stays frozen in the world until one of the two coin sides is chosen. */
public final class ChigurCoinScreen extends Screen {
    private final int entityId;

    public ChigurCoinScreen(int entityId) {
        super(Component.literal("Монетка"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        addRenderableWidget(new Button(width / 2 - 92, height / 2, 88, 20,
                Component.literal("Орёл"), button -> choose(true)));
        addRenderableWidget(new Button(width / 2 + 4, height / 2, 88, 20,
                Component.literal("Решка"), button -> choose(false)));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, "Выберите сторону монеты", width / 2, height / 2 - 28, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void choose(boolean heads) {
        ChigurNetwork.CHANNEL.sendToServer(new ChigurCoinChoicePacket(entityId, heads));
        onClose();
    }
}
