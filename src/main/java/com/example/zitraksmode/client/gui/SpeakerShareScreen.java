package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.ShareTrackPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpeakerShareScreen extends Screen {

    private final Screen parent;
    private final SpeakerTrackMeta track;
    private PlayerList playerList;

    public SpeakerShareScreen(Screen parent, SpeakerTrackMeta track) {
        super(Component.literal("Поделиться треком"));
        this.parent = parent;
        this.track = track;
    }

    @Override
    protected void init() {
        super.init();
        this.playerList = new PlayerList(this.minecraft, this.width, this.height, 32, this.height - 34, 22);
        this.addWidget(this.playerList);
        this.playerList.rebuildEntries(loadPlayers());

        this.addRenderableWidget(new Button(this.width / 2 - 75, this.height - 26, 150, 20, Component.literal("Назад"), button -> onClose()));
    }

    private List<PlayerInfo> loadPlayers() {
        List<PlayerInfo> result = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            return result;
        }

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            GameProfile profile = info.getProfile();
            if (profile == null || profile.getId() == null) {
                continue;
            }
            if (profile.getId().equals(mc.player.getUUID())) {
                continue;
            }
            result.add(info);
        }

        result.sort(Comparator.comparing(info -> info.getProfile().getName().toLowerCase()));
        return result;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        this.playerList.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("§7" + track.getTitle()), this.width / 2, 20, 0xAAAAAA);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private class PlayerList extends ObjectSelectionList<PlayerList.PlayerEntry> {

        public PlayerList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
            super(minecraft, width, height, top, bottom, itemHeight);
        }

        public void rebuildEntries(List<PlayerInfo> players) {
            clearEntries();
            for (PlayerInfo info : players) {
                addEntry(new PlayerEntry(info));
            }
        }

        @Override
        public int getRowWidth() {
            return 220;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 114;
        }

        private class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {

            private final PlayerInfo info;

            private PlayerEntry(PlayerInfo info) {
                this.info = info;
            }

            @Override
            public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                int color = isMouseOver ? 0xFFFF88 : 0xFFFFFF;
                font.draw(poseStack, info.getProfile().getName(), left + 8, top + 7, color);
            }

            @Override
            public Component getNarration() {
                return Component.literal(info.getProfile().getName());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    PacketHandler.sendToServer(new ShareTrackPacket(track.getTrackId(), info.getProfile().getId()));
                    onClose();
                    return true;
                }
                return false;
            }
        }
    }
}
