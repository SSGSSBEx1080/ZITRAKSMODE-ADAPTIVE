package com.example.zitraksmode.client.audio;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.RequestSharedTrackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeakerAudioManager {

    // ═══════════════════════════════════════════════════════════════
    // РАДИУС ЗВУКА КОЛОНКИ
    // Крути эти константы для баланса:
    //   FULL_VOLUME_DISTANCE — до этого расстояния слышно на 100%
    //   FADE_END_DISTANCE    — после этого расстояния не слышно вообще
    // Между ними — плавное квадратичное затухание.
    // ═══════════════════════════════════════════════════════════════
    private static final float FULL_VOLUME_DISTANCE = 6.0F;
    private static final float FADE_END_DISTANCE = 40.0F;

    private static final Map<Integer, ActiveSpeakerState> ACTIVE_SPEAKERS = new ConcurrentHashMap<>();
    private static final Map<String, DownloadSession> DOWNLOADS = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED_TRACKS = ConcurrentHashMap.newKeySet();

    private static String hudTrackName = "";
    private static float hudVolume = 1.0F;
    private static boolean hudVisible = false;

    private SpeakerAudioManager() {
    }

    public static void handleSpeakerState(int ownerEntityId,
                                          UUID ownerUuid,
                                          boolean active,
                                          boolean paused,
                                          SpeakerTrackMeta meta,
                                          float volume,
                                          long startGameTime,
                                          long positionMs) {
        if (!active || meta == null) {
            stopPlaying(ownerEntityId);
            return;
        }

        ActiveSpeakerState oldState = ACTIVE_SPEAKERS.get(ownerEntityId);
        boolean forceRestart = false;
        if (oldState != null) {
            String oldTrackId = oldState.trackMeta == null ? "" : oldState.trackMeta.getTrackId();
            String newTrackId = meta.getTrackId();
            if (!oldTrackId.equals(newTrackId)) {
                forceRestart = true;
            } else if (oldState.paused != paused) {
                forceRestart = true;
            } else if (Math.abs(oldState.positionMs - positionMs) > 300L) {
                forceRestart = true;
            } else if (oldState.startGameTime != startGameTime) {
                forceRestart = true;
            }
        }

        ActiveSpeakerState state = ACTIVE_SPEAKERS.computeIfAbsent(ownerEntityId, id -> new ActiveSpeakerState());
        state.ownerEntityId = ownerEntityId;
        state.ownerUuid = ownerUuid;
        state.trackMeta = meta;
        state.volume = clamp(volume);
        state.startGameTime = startGameTime;
        state.positionMs = Math.max(0L, positionMs);
        state.paused = paused;

        if (forceRestart && state.player != null) {
            state.player.stop();
            state.player = null;
        }

        if (paused) {
            if (state.player != null) {
                state.player.stop();
                state.player = null;
            }
        } else {
            tryStartPlayback(state);
        }
    }

    public static void stopPlaying(int ownerEntityId) {
        ActiveSpeakerState state = ACTIVE_SPEAKERS.remove(ownerEntityId);
        if (state != null && state.player != null) {
            state.player.stop();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == ownerEntityId) {
            clearHud();
        }
    }

    public static void beginTrackDownload(SpeakerTrackMeta meta, int totalChunks, int totalSize) {
        DOWNLOADS.put(trackKey(meta.getOwnerId(), meta.getTrackId()), new DownloadSession(meta, totalChunks, totalSize));
    }

    public static void acceptTrackChunk(UUID ownerUuid, String trackId, int chunkIndex, byte[] data) {
        DownloadSession session = DOWNLOADS.get(trackKey(ownerUuid, trackId));
        if (session != null) {
            session.acceptChunk(chunkIndex, data);
        }
    }

    public static void finishTrackDownload(UUID ownerUuid, String trackId) {
        String key = trackKey(ownerUuid, trackId);
        DownloadSession session = DOWNLOADS.remove(key);
        if (session == null) {
            REQUESTED_TRACKS.remove(key);
            return;
        }

        try {
            byte[] data = session.assemble();
            SpeakerClientTrackCache.storeTrack(session.meta, data);
        } catch (Exception ignored) {
        }

        REQUESTED_TRACKS.remove(key);

        for (ActiveSpeakerState state : ACTIVE_SPEAKERS.values()) {
            if (state.trackMeta != null && key.equals(trackKey(state.ownerUuid, state.trackMeta.getTrackId())) && !state.paused) {
                if (state.player != null) {
                    state.player.stop();
                    state.player = null;
                }
                tryStartPlayback(state);
            }
        }
    }

    public static void startPlaying(int ownerEntityId, String unusedTrackName, float volume) {
        // compat
    }

    public static void setVolume(int ownerEntityId, float volume) {
        float clamped = clamp(volume);

        ActiveSpeakerState state = ACTIVE_SPEAKERS.get(ownerEntityId);
        if (state != null) {
            state.volume = clamped;
            // Не выставляем громкость напрямую — она обновится в следующем tick()
            // с учётом расстояния до слушателя
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == ownerEntityId) {
            hudVolume = clamped;
        }
    }

    public static boolean isHudVisible() {
        return hudVisible;
    }

    public static String getHudTrackName() {
        return hudTrackName;
    }

    public static float getHudVolume() {
        return hudVolume;
    }

    public static boolean hasLocalPlayback() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ACTIVE_SPEAKERS.containsKey(mc.player.getId());
    }

    public static boolean isLocalPaused() {
        ActiveSpeakerState state = getLocalState();
        return state != null && state.paused;
    }

    public static String getLocalTrackId() {
        ActiveSpeakerState state = getLocalState();
        return state != null && state.trackMeta != null ? state.trackMeta.getTrackId() : "";
    }

    public static String getLocalTrackTitle() {
        ActiveSpeakerState state = getLocalState();
        return state != null && state.trackMeta != null ? state.trackMeta.getTitle() : "";
    }

    public static long getLocalTrackDurationMs() {
        ActiveSpeakerState state = getLocalState();
        return state != null && state.trackMeta != null ? state.trackMeta.getDurationMs() : 0L;
    }

    public static long getLocalProgressMs() {
        ActiveSpeakerState state = getLocalState();
        if (state == null || state.trackMeta == null) {
            return 0L;
        }

        long progress = state.paused ? state.positionMs : getProgressMs(state, Minecraft.getInstance());
        if (state.trackMeta.getDurationMs() > 0L) {
            progress = Math.min(progress, state.trackMeta.getDurationMs());
        }
        return progress;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            for (ActiveSpeakerState state : ACTIVE_SPEAKERS.values()) {
                if (state.player != null) {
                    state.player.stop();
                }
            }
            ACTIVE_SPEAKERS.clear();
            clearHud();
            return;
        }

        boolean foundOwnSpeaker = false;

        for (Map.Entry<Integer, ActiveSpeakerState> entry : ACTIVE_SPEAKERS.entrySet()) {
            ActiveSpeakerState state = entry.getValue();
            Entity entity = minecraft.level.getEntity(state.ownerEntityId);

            if (!(entity instanceof Player player) || !player.isAlive()) {
                stopPlaying(state.ownerEntityId);
                continue;
            }

            long progressMs = getProgressMs(state, minecraft);
            if (state.trackMeta.getDurationMs() > 0L && progressMs >= state.trackMeta.getDurationMs()) {
                if (state.player != null) {
                    state.player.stop();
                    state.player = null;
                }
                if (minecraft.player.getId() == state.ownerEntityId) {
                    clearHud();
                }
                continue;
            }

            if (!state.paused) {
                tryStartPlayback(state);
                if (state.player != null) {
                    // Считаем громкость с учётом расстояния до владельца колонки
                    float distanceMultiplier = computeDistanceVolume(minecraft.player, player);
                    float finalVolume = state.volume * distanceMultiplier;
                    state.player.setVolume(finalVolume);
                    state.player.update(player.getX(), player.getY() + 1.0D, player.getZ());
                }
            }

            if (minecraft.player.getId() == state.ownerEntityId) {
                foundOwnSpeaker = true;
                hudVisible = true;
                hudTrackName = state.trackMeta.getTitle();
                hudVolume = state.volume;
            }
        }

        if (!foundOwnSpeaker) {
            clearHud();
        }
    }

    private static void tryStartPlayback(ActiveSpeakerState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || state.trackMeta == null || state.paused) {
            return;
        }

        Entity entity = minecraft.level.getEntity(state.ownerEntityId);
        if (!(entity instanceof Player player)) {
            return;
        }

        File cachedTrack = SpeakerClientTrackCache.getTrackFile(state.trackMeta);
        if (!cachedTrack.isFile()) {
            requestTrackIfNeeded(state);
            return;
        }

        long offsetMs = getProgressMs(state, minecraft);
        if (state.trackMeta.getDurationMs() > 0L && offsetMs >= state.trackMeta.getDurationMs()) {
            return;
        }

        boolean shouldRestart = state.player == null
                || state.player.getCurrentFile() == null
                || !cachedTrack.equals(state.player.getCurrentFile())
                || !state.player.isPlaying();

        if (shouldRestart) {
            if (state.player != null) {
                state.player.stop();
            }
            state.player = new PositionalMusicPlayer();
            // Стартовая громкость с учётом дистанции — обновится в tick()
            float distanceMultiplier = computeDistanceVolume(minecraft.player, player);
            state.player.start(cachedTrack, state.volume * distanceMultiplier,
                    player.getX(), player.getY() + 1.0D, player.getZ(), offsetMs);
        }
    }

    private static void requestTrackIfNeeded(ActiveSpeakerState state) {
        String key = trackKey(state.ownerUuid, state.trackMeta.getTrackId());
        if (REQUESTED_TRACKS.add(key)) {
            PacketHandler.sendToServer(new RequestSharedTrackPacket(state.ownerUuid, state.trackMeta.getTrackId()));
        }
    }

    private static long getProgressMs(ActiveSpeakerState state, Minecraft minecraft) {
        if (state.paused || minecraft.level == null) {
            return state.positionMs;
        }
        long elapsedMs = Math.max(0L, (minecraft.level.getGameTime() - state.startGameTime) * 50L);
        return state.positionMs + elapsedMs;
    }

    private static ActiveSpeakerState getLocalState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        return ACTIVE_SPEAKERS.get(mc.player.getId());
    }

    private static void clearHud() {
        hudVisible = false;
        hudTrackName = "";
        hudVolume = 1.0F;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /**
     * Считает множитель громкости (0.0-1.0) в зависимости от расстояния между
     * слушателем и владельцем колонки. Владелец колонки всегда слышит на 100%.
     */
    private static float computeDistanceVolume(Player listener, Player speakerOwner) {
        if (listener == null || speakerOwner == null) {
            return 1.0F;
        }

        // Локальный игрок = владелец колонки → полная громкость (колонка у него в руках)
        if (listener.getId() == speakerOwner.getId()) {
            return 1.0F;
        }

        double dx = listener.getX() - speakerOwner.getX();
        double dy = listener.getY() - speakerOwner.getY();
        double dz = listener.getZ() - speakerOwner.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= FULL_VOLUME_DISTANCE) {
            return 1.0F;
        }
        if (distance >= FADE_END_DISTANCE) {
            return 0.0F;
        }

        // Плавное квадратичное затухание от 100% до 0%
        float t = (float)((distance - FULL_VOLUME_DISTANCE) / (FADE_END_DISTANCE - FULL_VOLUME_DISTANCE));
        float linear = 1.0F - t;
        return linear * linear;
    }

    private static String trackKey(UUID ownerUuid, String trackId) {
        return ownerUuid + ":" + trackId;
    }

    private static class ActiveSpeakerState {
        private int ownerEntityId;
        private UUID ownerUuid;
        private SpeakerTrackMeta trackMeta;
        private float volume;
        private long startGameTime;
        private long positionMs;
        private boolean paused;
        private PositionalMusicPlayer player;
    }

    private static class DownloadSession {
        private final SpeakerTrackMeta meta;
        private final int totalChunks;
        private final int totalSize;
        private final byte[][] chunks;

        private DownloadSession(SpeakerTrackMeta meta, int totalChunks, int totalSize) {
            this.meta = meta;
            this.totalChunks = totalChunks;
            this.totalSize = totalSize;
            this.chunks = new byte[totalChunks][];
        }

        private void acceptChunk(int chunkIndex, byte[] data) {
            if (chunkIndex >= 0 && chunkIndex < chunks.length) {
                chunks[chunkIndex] = data;
            }
        }

        private byte[] assemble() {
            byte[] result = new byte[totalSize];
            int offset = 0;
            for (int i = 0; i < totalChunks; i++) {
                byte[] chunk = chunks[i];
                if (chunk == null) {
                    throw new IllegalStateException("Missing JBL track chunk: " + i + "/" + totalChunks);
                }
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }
            return result;
        }
    }
}