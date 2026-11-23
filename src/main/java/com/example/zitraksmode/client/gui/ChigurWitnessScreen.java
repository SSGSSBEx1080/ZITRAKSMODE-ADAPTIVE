package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.network.chigur.ChigurNetwork;
import com.example.zitraksmode.network.chigur.ChigurWitnessChoicePacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Two-answer screen shown only to the player Chigur is questioning. */
public final class ChigurWitnessScreen extends Screen {
    private final int entityId;
    private final String huntedName;
    private final String refusalText;

    public ChigurWitnessScreen(int entityId, String huntedName, String refusalText) {
        super(Component.literal("Антон Чигур"));
        this.entityId = entityId;
        this.huntedName = huntedName;
        this.refusalText = refusalText;
    }

    @Override
    protected void init() {
        addRenderableWidget(new Button(width / 2 - 120, height / 2 + 2, 240, 20,
                Component.literal("Рассказать, где он"), button -> choose(0)));
        addRenderableWidget(new Button(width / 2 - 120, height / 2 + 28, 240, 20,
                Component.literal(refusalText), button -> choose(1)));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, "Вы знаете, где сейчас " + huntedName + "?", width / 2, height / 2 - 35, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void choose(int choice) {
        ChigurNetwork.CHANNEL.sendToServer(new ChigurWitnessChoicePacket(entityId, choice));
        onClose();
    }
}
