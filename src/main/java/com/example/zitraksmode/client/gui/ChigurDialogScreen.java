package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.network.chigur.ChigurDialogChoicePacket;
import com.example.zitraksmode.network.chigur.ChigurNetwork;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChigurDialogScreen extends Screen {
    private final int entityId;

    public ChigurDialogScreen(int entityId) {
        super(Component.literal("Антон Чигур"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        addRenderableWidget(new Button(width / 2 - 100, height / 2 - 22, 200, 20,
                Component.literal("Откуда ты?"), button -> choose(0)));
        addRenderableWidget(new Button(width / 2 - 100, height / 2 + 5, 200, 20,
                Component.literal("Извините, обознался"), button -> choose(1)));
    }

    private void choose(int choice) {
        ChigurNetwork.CHANNEL.sendToServer(new ChigurDialogChoicePacket(entityId, choice));
        onClose();
    }
}
