package com.example.zitraksmode.jbl;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeakerTrackStorage {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final long MAX_UPLOAD_SIZE = 32L * 1024L * 1024L;
    public static final int MAX_TRACKS_PER_PLAYER = 512;

    private static final Map<UUID, SpeakerTrackUploadSession> UPLOAD_SESSIONS = new ConcurrentHashMap<>();

    private final MinecraftServer server;

    private SpeakerTrackStorage(MinecraftServer server) {
        this.server = server;
    }

    public static SpeakerTrackStorage forServer(MinecraftServer server) {
        return new SpeakerTrackStorage(server);
    }

    public void beginUpload(ServerPlayer player,
                            UUID sessionId,
                            String fileName,
                            String title,
                            String author,
                            long expectedSize,
                            long durationMs) throws IOException {
        if (expectedSize <= 0L) {
            throw new IOException("Empty upload is not allowed");
        }
        if (expectedSize > MAX_UPLOAD_SIZE) {
            throw new IOException("Track is too large: " + expectedSize + " bytes");
        }
        if (getLibrary(player.getUUID()).size() >= MAX_TRACKS_PER_PLAYER) {
            throw new IOException("Library limit reached: " + MAX_TRACKS_PER_PLAYER);
        }

        SpeakerTrackUploadSession session = new SpeakerTrackUploadSession(
                sessionId,
                player.getUUID(),
                sanitizeFileName(fileName),
                sanitizeText(title, 128),
                sanitizeText(author, 128),
                expectedSize,
                Math.max(0L, durationMs)
        );

        UPLOAD_SESSIONS.put(sessionId, session);
        LOGGER.info("JBL: started upload session {} for {} ({})", sessionId, player.getGameProfile().getName(), fileName);
    }

    public void appendUploadChunk(ServerPlayer player, UUID sessionId, int chunkIndex, byte[] data) throws IOException {
        SpeakerTrackUploadSession session = requireOwnedSession(player, sessionId);
        session.appendChunk(chunkIndex, data, MAX_UPLOAD_SIZE);
    }

    public SpeakerTrackMeta finishUpload(ServerPlayer player, UUID sessionId) throws IOException {
        SpeakerTrackUploadSession session = requireOwnedSession(player, sessionId);
        byte[] bytes = session.finish();
        UPLOAD_SESSIONS.remove(sessionId);

        SpeakerTrackMeta meta = saveTrackForPlayer(
                player.getUUID(),
                session.getFileName(),
                session.getTitle(),
                session.getAuthor(),
                session.getDurationMs(),
                bytes
        );

        LOGGER.info("JBL: finished upload {} for {} -> {}", sessionId, player.getGameProfile().getName(), getTrackFile(player.getUUID(), meta.getTrackId()));
        return meta;
    }

    public SpeakerTrackMeta saveTrackForPlayer(UUID playerId,
                                               String fileName,
                                               String title,
                                               String author,
                                               long durationMs,
                                               byte[] bytes) throws IOException {
        if (bytes == null || bytes.length <= 0) {
            throw new IOException("Track bytes are empty");
        }
        if (bytes.length > MAX_UPLOAD_SIZE) {
            throw new IOException("Track is too large: " + bytes.length + " bytes");
        }
        if (getLibrary(playerId).size() >= MAX_TRACKS_PER_PLAYER) {
            throw new IOException("Library limit reached: " + MAX_TRACKS_PER_PLAYER);
        }

        String trackId = createTrackId(title);
        String sha1 = sha1Hex(bytes);

        SpeakerTrackMeta meta = new SpeakerTrackMeta(
                trackId,
                playerId,
                ensureOggFileName(fileName, title),
                sanitizeText(title, 128),
                sanitizeText(author, 128),
                Math.max(0L, durationMs),
                bytes.length,
                0,
                System.currentTimeMillis(),
                sha1
        );

        Path trackFile = getTrackFile(playerId, trackId);
        Path metaFile = getMetaFile(playerId, trackId);
        Files.createDirectories(trackFile.getParent());
        Files.createDirectories(metaFile.getParent());

        Files.write(trackFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        writeMeta(metaFile, meta);
        return meta;
    }

    public List<SpeakerTrackMeta> getLibrary(UUID playerId) throws IOException {
        Path metaDir = getMetaDir(playerId);
        List<SpeakerTrackMeta> result = new ArrayList<>();

        if (!Files.exists(metaDir)) {
            return result;
        }

        try (var stream = Files.list(metaDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".nbt"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            SpeakerTrackMeta meta = readMeta(path);
                            if (meta != null) {
                                result.add(meta);
                            }
                        } catch (Exception e) {
                            LOGGER.error("JBL: failed to read meta {}", path, e);
                        }
                    });
        }

        result.sort(Comparator.comparingLong(SpeakerTrackMeta::getCreatedAt).reversed());
        return result;
    }

    public SpeakerTrackMeta getTrackMeta(UUID playerId, String trackId) throws IOException {
        Path metaFile = getMetaFile(playerId, trackId);
        if (!Files.exists(metaFile)) {
            return null;
        }
        return readMeta(metaFile);
    }

    public Path getTrackFile(UUID playerId, String trackId) {
        return getTrackDir(playerId).resolve(trackId + ".ogg");
    }

    public byte[] readTrackBytes(UUID playerId, String trackId) throws IOException {
        return Files.readAllBytes(getTrackFile(playerId, trackId));
    }

    public boolean deleteTrack(UUID playerId, String trackId) throws IOException {
        Path trackFile = getTrackFile(playerId, trackId);
        Path metaFile = getMetaFile(playerId, trackId);
        boolean deletedTrack = Files.deleteIfExists(trackFile);
        boolean deletedMeta = Files.deleteIfExists(metaFile);
        return deletedTrack || deletedMeta;
    }

    public SpeakerTrackMeta renameTrack(UUID playerId, String trackId, String newTitle) throws IOException {
        SpeakerTrackMeta meta = getTrackMeta(playerId, trackId);
        if (meta == null) {
            return null;
        }
        meta.setTitle(sanitizeText(newTitle, 128));
        writeMeta(getMetaFile(playerId, trackId), meta);
        return meta;
    }

    public SpeakerTrackMeta incrementLike(UUID playerId, String trackId) throws IOException {
        SpeakerTrackMeta meta = getTrackMeta(playerId, trackId);
        if (meta == null) {
            return null;
        }
        meta.setLikes(meta.getLikes() + 1);
        writeMeta(getMetaFile(playerId, trackId), meta);
        return meta;
    }

    private SpeakerTrackUploadSession requireOwnedSession(ServerPlayer player, UUID sessionId) throws IOException {
        SpeakerTrackUploadSession session = UPLOAD_SESSIONS.get(sessionId);
        if (session == null) {
            throw new IOException("Upload session not found: " + sessionId);
        }
        if (!session.getPlayerId().equals(player.getUUID())) {
            throw new IOException("Upload session does not belong to player: " + player.getGameProfile().getName());
        }
        return session;
    }

    private Path getLibraryRoot() {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("zitraksmode")
                .resolve("jbl_library")
                .resolve("players");
    }

    private Path getPlayerRoot(UUID playerId) {
        return getLibraryRoot().resolve(playerId.toString());
    }

    private Path getTrackDir(UUID playerId) {
        return getPlayerRoot(playerId).resolve("tracks");
    }

    private Path getMetaDir(UUID playerId) {
        return getPlayerRoot(playerId).resolve("meta");
    }

    private Path getMetaFile(UUID playerId, String trackId) {
        return getMetaDir(playerId).resolve(trackId + ".nbt");
    }

    private void writeMeta(Path path, SpeakerTrackMeta meta) throws IOException {
        CompoundTag tag = meta.toTag();
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            NbtIo.writeCompressed(tag, outputStream);
        }
    }

    private SpeakerTrackMeta readMeta(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            CompoundTag tag = NbtIo.readCompressed(inputStream);
            if (tag == null) {
                return null;
            }
            return SpeakerTrackMeta.fromTag(tag);
        }
    }

    private static String createTrackId(String title) {
        String base = sanitizeText(title, 48).toLowerCase();
        base = base.replaceAll("[^a-z0-9а-яё_-]", "_");
        base = base.replaceAll("_+", "_");
        if (base.isBlank()) {
            base = "track";
        }
        return base + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String ensureOggFileName(String fileName, String fallbackTitle) {
        String cleaned = sanitizeFileName(fileName);
        if (cleaned.toLowerCase().endsWith(".ogg")) {
            return cleaned;
        }
        String fallback = sanitizeFileName(fallbackTitle);
        if (fallback.isBlank()) {
            fallback = "track";
        }
        return fallback + ".ogg";
    }

    private static String sanitizeFileName(String value) {
        String cleaned = value == null ? "" : value.trim();
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|]", "_");
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.length() > 128) {
            cleaned = cleaned.substring(0, 128);
        }
        return cleaned;
    }

    private static String sanitizeText(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private static String sha1Hex(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 is not available", e);
        }
    }
}
