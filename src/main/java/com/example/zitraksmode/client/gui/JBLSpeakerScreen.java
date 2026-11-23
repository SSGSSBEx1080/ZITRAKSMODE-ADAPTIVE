package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.client.audio.MusicTrackManager;
import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.network.DeleteTrackPacket;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.PauseSpeakerPacket;
import com.example.zitraksmode.network.SeekSpeakerPacket;
import com.example.zitraksmode.network.SelectTrackPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JBLSpeakerScreen extends Screen {

    private static final int LIST_WIDTH = 308;
    private static final int ROW_WIDTH = 288;
    private static final int ACTION_SIZE = 16;
    private static final int ACTION_GAP = 5;
    private static final long DOUBLE_CLICK_MS = 250L;

    private final InteractionHand hand;

    private TrackList trackList;
    private final List<SpeakerTrackMeta> sortedTracks = new ArrayList<>();

    private String statusText = "";
    private String lastLibraryFingerprint = "";

    private int progressBarX;
    private int progressBarY;
    private int progressBarWidth;
    private int progressBarHeight;
    private boolean draggingSeek = false;
    private long pendingSeekMs = -1L;

    private String lastClickedTrackId = "";
    private long lastClickTimeMs = 0L;

    public JBLSpeakerScreen(InteractionHand hand) {
        super(Component.literal("JBL Колонка"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        this.trackList = new TrackList(this.minecraft, LIST_WIDTH, this.height, 42, this.height - 94, 36);
        this.trackList.setLeftPos(this.width / 2 - 172);
        this.trackList.setRenderBackground(false);
        this.trackList.setRenderTopAndBottom(false);
        this.addWidget(this.trackList);

        this.progressBarWidth = 330;
        this.progressBarHeight = 6;
        this.progressBarX = this.width / 2 - this.progressBarWidth / 2;
        this.progressBarY = this.height - 24;

        MusicTrackManager.getInstance().requestLibraryRefresh();
        rebuildTrackList();
    }

    @Override
    public void tick() {
        super.tick();
        String fingerprint = buildLibraryFingerprint();
        if (!fingerprint.equals(this.lastLibraryFingerprint)) {
            rebuildTrackList();
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        this.statusText = "Импорт...";
        MusicTrackManager.getInstance().uploadTracksAsync(paths, status -> {
            this.statusText = status;
            rebuildTrackList();
        });
    }

    private void refresh() {
        this.statusText = "Обновление...";
        MusicTrackManager.getInstance().requestLibraryRefresh();
        rebuildTrackList();
    }

    private void rebuildTrackList() {
        SpeakerTrackMeta selectedBefore = this.trackList == null || this.trackList.getSelected() == null ? null : this.trackList.getSelected().track;

        this.sortedTracks.clear();
        this.sortedTracks.addAll(MusicTrackManager.getInstance().getLibrary());
        this.sortedTracks.sort(
                Comparator.comparingInt(SpeakerTrackMeta::getLikes).reversed()
                        .thenComparing(Comparator.comparingLong(SpeakerTrackMeta::getCreatedAt).reversed())
                        .thenComparing(meta -> meta.getTitle().toLowerCase(Locale.ROOT))
        );
        this.lastLibraryFingerprint = buildLibraryFingerprint();

        this.trackList.rebuildEntries(this.sortedTracks);
        if (selectedBefore != null) {
            this.trackList.restoreSelectionByTrackId(selectedBefore.getTrackId());
        }
    }

    private String buildLibraryFingerprint() {
        StringBuilder builder = new StringBuilder();
        for (SpeakerTrackMeta meta : MusicTrackManager.getInstance().getLibrary()) {
            builder.append(meta.getTrackId()).append('|')
                    .append(meta.getTitle()).append('|')
                    .append(meta.getLikes()).append('|')
                    .append(meta.getCreatedAt()).append(';');
        }
        return builder.toString();
    }

    private void playTrack(SpeakerTrackMeta track) {
        if (track == null) {
            this.statusText = "Выбери трек";
            return;
        }
        PacketHandler.sendToServer(new SelectTrackPacket(hand, track.getTrackId()));
        this.statusText = "Играет: " + track.getTitle();
    }

    private void playSelected() {
        TrackList.TrackEntry selected = this.trackList.getSelected();
        playTrack(selected == null ? null : selected.track);
    }

    private void togglePlayPause() {
        if (SpeakerAudioManager.hasLocalPlayback()) {
            PacketHandler.sendToServer(new PauseSpeakerPacket(hand, !SpeakerAudioManager.isLocalPaused()));
        } else {
            playSelected();
        }
    }

    private void prevTrack() {
        SpeakerTrackMeta target = getNeighborTrack(-1);
        if (target != null) {
            playTrack(target);
            this.trackList.restoreSelectionByTrackId(target.getTrackId());
        }
    }

    private void nextTrack() {
        SpeakerTrackMeta target = getNeighborTrack(1);
        if (target != null) {
            playTrack(target);
            this.trackList.restoreSelectionByTrackId(target.getTrackId());
        }
    }

    private SpeakerTrackMeta getNeighborTrack(int delta) {
        if (this.sortedTracks.isEmpty()) {
            return null;
        }

        String currentTrackId = SpeakerAudioManager.getLocalTrackId();
        if (currentTrackId == null || currentTrackId.isEmpty()) {
            TrackList.TrackEntry selected = this.trackList.getSelected();
            return selected != null ? selected.track : this.sortedTracks.get(0);
        }

        for (int i = 0; i < this.sortedTracks.size(); i++) {
            if (currentTrackId.equals(this.sortedTracks.get(i).getTrackId())) {
                int nextIndex = (i + delta + this.sortedTracks.size()) % this.sortedTracks.size();
                return this.sortedTracks.get(nextIndex);
            }
        }

        return this.sortedTracks.get(0);
    }

    private void likeSelected() {
        TrackList.TrackEntry selected = this.trackList.getSelected();
        if (selected != null) {
            PacketHandler.sendToServer(new com.example.zitraksmode.network.LikeTrackPacket(selected.track.getTrackId()));
            MusicTrackManager.getInstance().requestLibraryRefresh();
        }
    }

    private void renameSelected() {
        TrackList.TrackEntry selected = this.trackList.getSelected();
        if (selected != null) {
            this.minecraft.setScreen(new SpeakerRenameScreen(this, selected.track));
        }
    }

    private void shareSelected() {
        TrackList.TrackEntry selected = this.trackList.getSelected();
        if (selected != null) {
            this.minecraft.setScreen(new SpeakerShareScreen(this, selected.track));
        }
    }

    private void deleteSelected() {
        TrackList.TrackEntry selected = this.trackList.getSelected();
        if (selected != null) {
            PacketHandler.sendToServer(new DeleteTrackPacket(selected.track.getTrackId()));
            MusicTrackManager.getInstance().requestLibraryRefresh();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (getRefreshRect().contains(mouseX, mouseY)) {
                refresh();
                return true;
            }
            if (handleActionIconClick(mouseX, mouseY)) {
                return true;
            }
            if (getPrevRect().contains(mouseX, mouseY)) {
                prevTrack();
                return true;
            }
            if (getPlayPauseRect().contains(mouseX, mouseY)) {
                togglePlayPause();
                return true;
            }
            if (getNextRect().contains(mouseX, mouseY)) {
                nextTrack();
                return true;
            }
            if (isOverProgressBar(mouseX, mouseY) && SpeakerAudioManager.hasLocalPlayback()) {
                draggingSeek = true;
                this.pendingSeekMs = computeSeekMs(mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSeek) {
            this.pendingSeekMs = computeSeekMs(mouseX);
            this.statusText = "Перемотка: " + formatDuration(this.pendingSeekMs);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSeek) {
            draggingSeek = false;
            long targetMs = this.pendingSeekMs;
            this.pendingSeekMs = -1L;
            if (targetMs >= 0L) {
                PacketHandler.sendToServer(new SeekSpeakerPacket(hand, targetMs));
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private long computeSeekMs(double mouseX) {
        long durationMs = SpeakerAudioManager.getLocalTrackDurationMs();
        if (durationMs <= 0L) {
            return 0L;
        }
        double progress = (mouseX - progressBarX) / (double) progressBarWidth;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return (long) (durationMs * progress);
    }

    private boolean handleActionIconClick(double mouseX, double mouseY) {
        TrackList.TrackEntry selected = this.trackList.getSelected();
        if (selected == null) {
            return false;
        }

        Rect like = getActionRect(0);
        Rect rename = getActionRect(1);
        Rect share = getActionRect(2);
        Rect delete = getActionRect(3);

        if (like.contains(mouseX, mouseY)) {
            likeSelected();
            return true;
        }
        if (rename.contains(mouseX, mouseY)) {
            renameSelected();
            return true;
        }
        if (share.contains(mouseX, mouseY)) {
            shareSelected();
            return true;
        }
        if (delete.contains(mouseX, mouseY)) {
            deleteSelected();
            return true;
        }
        return false;
    }

    private boolean isOverProgressBar(double mouseX, double mouseY) {
        return mouseX >= progressBarX
                && mouseX <= progressBarX + progressBarWidth
                && mouseY >= progressBarY - 3
                && mouseY <= progressBarY + progressBarHeight + 3;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        fillGradient(poseStack, 0, 0, this.width, this.height, 0xFF090909, 0xFF131313);
        fillGradient(poseStack, this.width / 2 - 188, 38, this.width / 2 + 188, this.height - 90, 0xCC111111, 0xAA0D0D0D);
        fill(poseStack, this.width / 2 - 188, 38, this.width / 2 + 188, 39, 0xFF1DB954);

        this.trackList.render(poseStack, mouseX, mouseY, partialTick);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("§7Перетащи .ogg / .mp3 прямо сюда для добавления в библиотеку"), this.width / 2, 22, 0xFFFFFF);
        if (!this.statusText.isEmpty()) {
            drawCenteredString(poseStack, this.font, Component.literal("§8" + trimToWidth(this.statusText, this.width - 40)), this.width / 2, 34, 0xFFFFFF);
        }

        renderRefreshIcon(poseStack, mouseX, mouseY);
        renderSelectedTrackActions(poseStack, mouseX, mouseY);
        renderBottomControlBar(poseStack, mouseX, mouseY);
        renderHoveredTooltip(poseStack, mouseX, mouseY);
    }

    private void renderRefreshIcon(PoseStack poseStack, int mouseX, int mouseY) {
        Rect rect = getRefreshRect();
        int bg = rect.contains(mouseX, mouseY) ? 0xFF323232 : 0xCC1A1A1A;
        fill(poseStack, rect.x1, rect.y1, rect.x2, rect.y2, bg);
        drawCenteredString(poseStack, this.font, Component.literal("⟳"), (rect.x1 + rect.x2) / 2, rect.y1 + 6, 0xFFFFFF);
    }

    private void renderSelectedTrackActions(PoseStack poseStack, int mouseX, int mouseY) {
        if (this.trackList.getSelected() == null) {
            return;
        }
        drawActionIcon(poseStack, getActionRect(0), "❤", mouseX, mouseY);
        drawActionIcon(poseStack, getActionRect(1), "✎", mouseX, mouseY);
        drawActionIcon(poseStack, getActionRect(2), "↗", mouseX, mouseY);
        drawActionIcon(poseStack, getActionRect(3), "✖", mouseX, mouseY);
    }

    private void drawActionIcon(PoseStack poseStack, Rect rect, String text, int mouseX, int mouseY) {
        int bg = rect.contains(mouseX, mouseY) ? 0xFF254B2D : 0xCC181818;
        fill(poseStack, rect.x1, rect.y1, rect.x2, rect.y2, bg);
        drawCenteredString(poseStack, this.font, Component.literal(text), (rect.x1 + rect.x2) / 2, rect.y1 + 4, 0xFFFFFF);
    }

    private void renderBottomControlBar(PoseStack poseStack, int mouseX, int mouseY) {
        int boxX1 = this.width / 2 - 196;
        int boxY1 = this.height - 82;
        int boxX2 = this.width / 2 + 196;
        int boxY2 = this.height - 6;

        fillGradient(poseStack, boxX1, boxY1, boxX2, boxY2, 0xDD151515, 0xEE0F0F0F);
        fill(poseStack, boxX1, boxY1, boxX2, boxY1 + 1, 0xFF1DB954);

        String title = SpeakerAudioManager.hasLocalPlayback() ? SpeakerAudioManager.getLocalTrackTitle() : "Ничего не играет";
        title = trimToWidth(title, 260);
        drawCenteredString(poseStack, this.font, Component.literal(SpeakerAudioManager.hasLocalPlayback() ? "§f" + title : "§7" + title), this.width / 2, this.height - 76, 0xFFFFFF);

        drawControlButton(poseStack, getPrevRect(), "⏮", mouseX, mouseY, false);
        drawControlButton(poseStack, getPlayPauseRect(), SpeakerAudioManager.hasLocalPlayback() && !SpeakerAudioManager.isLocalPaused() ? "⏸" : "▶", mouseX, mouseY, true);
        drawControlButton(poseStack, getNextRect(), "⏭", mouseX, mouseY, false);

        long durationMs = SpeakerAudioManager.getLocalTrackDurationMs();
        long progressMs = draggingSeek && pendingSeekMs >= 0L ? pendingSeekMs : SpeakerAudioManager.getLocalProgressMs();

        fill(poseStack, progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, 0xFF2C2C2C);
        int fillWidth = durationMs <= 0L ? 0 : (int) ((progressMs / (double) durationMs) * progressBarWidth);
        fillWidth = Math.max(0, Math.min(progressBarWidth, fillWidth));
        fill(poseStack, progressBarX, progressBarY, progressBarX + fillWidth, progressBarY + progressBarHeight, 0xFF1DB954);

        int knobX = progressBarX + fillWidth;
        fill(poseStack, knobX - 2, progressBarY - 2, knobX + 2, progressBarY + progressBarHeight + 2, 0xFFFFFFFF);

        this.font.draw(poseStack, Component.literal("§7" + formatDuration(progressMs)), progressBarX, progressBarY + 10, 0xFFFFFF);
        this.font.draw(poseStack, Component.literal("§7" + formatDuration(durationMs)), progressBarX + progressBarWidth - 30, progressBarY + 10, 0xFFFFFF);
    }

    private void drawControlButton(PoseStack poseStack, Rect rect, String icon, int mouseX, int mouseY, boolean accent) {
        int bg;
        if (rect.contains(mouseX, mouseY)) {
            bg = accent ? 0xFF2ADB69 : 0xFF2C2C2C;
        } else {
            bg = accent ? 0xFF1DB954 : 0xFF202020;
        }
        fill(poseStack, rect.x1, rect.y1, rect.x2, rect.y2, bg);
        drawCenteredString(poseStack, this.font, Component.literal(icon), (rect.x1 + rect.x2) / 2, rect.y1 + (accent ? 7 : 5), 0xFFFFFF);
    }

    private void renderHoveredTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        Component tooltip = null;

        if (getRefreshRect().contains(mouseX, mouseY)) {
            tooltip = Component.literal("Обновить библиотеку");
        } else if (this.trackList.getSelected() != null) {
            if (getActionRect(0).contains(mouseX, mouseY)) {
                tooltip = Component.literal("Лайкнуть");
            } else if (getActionRect(1).contains(mouseX, mouseY)) {
                tooltip = Component.literal("Переименовать");
            } else if (getActionRect(2).contains(mouseX, mouseY)) {
                tooltip = Component.literal("Поделиться игроку");
            } else if (getActionRect(3).contains(mouseX, mouseY)) {
                tooltip = Component.literal("Удалить трек");
            }
        }

        if (tooltip == null && getPrevRect().contains(mouseX, mouseY)) {
            tooltip = Component.literal("Предыдущий");
        } else if (tooltip == null && getPlayPauseRect().contains(mouseX, mouseY)) {
            tooltip = Component.literal(SpeakerAudioManager.hasLocalPlayback() && !SpeakerAudioManager.isLocalPaused() ? "Пауза" : "Играть");
        } else if (tooltip == null && getNextRect().contains(mouseX, mouseY)) {
            tooltip = Component.literal("Следующий");
        } else if (tooltip == null && isOverProgressBar(mouseX, mouseY)) {
            tooltip = Component.literal("Перемотка");
        }

        if (tooltip != null) {
            renderTooltip(poseStack, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Rect getRefreshRect() {
        return new Rect(this.width - 30, 8, this.width - 8, 30);
    }

    private Rect getPrevRect() {
        return new Rect(this.width / 2 - 66, this.height - 58, this.width / 2 - 40, this.height - 36);
    }

    private Rect getPlayPauseRect() {
        return new Rect(this.width / 2 - 18, this.height - 60, this.width / 2 + 18, this.height - 32);
    }

    private Rect getNextRect() {
        return new Rect(this.width / 2 + 40, this.height - 58, this.width / 2 + 66, this.height - 36);
    }

    private Rect getActionRect(int index) {
        int rowTop = this.trackList.getSelectedRowTop();
        int rowLeft = this.trackList.getRowLeft();
        int rowWidth = this.trackList.getRowWidth();
        int x = rowLeft + rowWidth + 10 + index * (ACTION_SIZE + ACTION_GAP);
        int y = rowTop + 10;
        return new Rect(x, y, x + ACTION_SIZE, y + ACTION_SIZE);
    }

    private String trimToWidth(String text, int width) {
        if (text == null) {
            return "";
        }
        if (this.font.width(text) <= width) {
            return text;
        }
        String result = text;
        while (!result.isEmpty() && this.font.width(result + "...") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static String formatDate(long millis) {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(millis));
    }

    private static class Rect {
        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;

        private Rect(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
        }
    }

    private class TrackList extends ObjectSelectionList<TrackList.TrackEntry> {

        public TrackList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
            super(minecraft, width, height, top, bottom, itemHeight);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            JBLSpeakerScreen.this.fillGradient(poseStack, this.x0 - 6, this.y0 - 4, this.x1 + 6, this.y1 + 4, 0xCC101010, 0xCC0C0C0C);
            JBLSpeakerScreen.this.fill(poseStack, this.x0 - 6, this.y0 - 4, this.x1 + 6, this.y0 - 3, 0xFF1DB954);
            super.render(poseStack, mouseX, mouseY, partialTick);
        }

        public void rebuildEntries(List<SpeakerTrackMeta> tracks) {
            clearEntries();
            for (SpeakerTrackMeta track : tracks) {
                addEntry(new TrackEntry(track));
            }
        }

        public void restoreSelectionByTrackId(String trackId) {
            for (TrackEntry entry : this.children()) {
                if (entry.track.getTrackId().equals(trackId)) {
                    setSelected(entry);
                    break;
                }
            }
        }

        public int getSelectedRowTop() {
            TrackEntry selected = getSelected();
            if (selected == null) {
                return this.y0;
            }
            int index = this.children().indexOf(selected);
            return index < 0 ? this.y0 : this.getRowTop(index);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 8;
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        public class TrackEntry extends ObjectSelectionList.Entry<TrackEntry> {

            private final SpeakerTrackMeta track;

            public TrackEntry(SpeakerTrackMeta track) {
                this.track = track;
            }

            @Override
            public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                Minecraft minecraft = Minecraft.getInstance();
                boolean isSelected = TrackList.this.getSelected() == this;

                if (isSelected) {
                    JBLSpeakerScreen.this.fill(poseStack, left, top, left + 3, top + height - 2, 0xFF1DB954);
                }

                int color = isSelected ? 0xFFFFFF : (isMouseOver ? 0xFFFFAA : 0xE0E0E0);
                minecraft.font.draw(poseStack, "♫", left + 8, top + 12, color);

                List<FormattedCharSequence> titleLines = minecraft.font.split(Component.literal(track.getTitle()), width - 36);
                if (!titleLines.isEmpty()) {
                    minecraft.font.draw(poseStack, titleLines.get(0), left + 22, top + 5, color);
                }

                String infoLine = "§b" + track.getAuthor()
                        + "  §8•  §7" + formatDuration(track.getDurationMs())
                        + "  §8•  §e❤ " + track.getLikes()
                        + "  §8•  §7" + formatDate(track.getCreatedAt());
                minecraft.font.draw(poseStack, Component.literal(infoLine), left + 22, top + 18, 0xAAAAAA);
            }

            @Override
            public Component getNarration() {
                return Component.literal(track.getTitle());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) {
                    return false;
                }

                TrackList.this.setSelected(this);
                long now = System.currentTimeMillis();
                boolean doubleClick = track.getTrackId().equals(lastClickedTrackId) && (now - lastClickTimeMs) <= DOUBLE_CLICK_MS;
                lastClickedTrackId = track.getTrackId();
                lastClickTimeMs = now;

                if (doubleClick) {
                    playTrack(track);
                }
                return true;
            }
        }
    }
}
