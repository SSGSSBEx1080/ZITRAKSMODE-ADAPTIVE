package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxAttachmentStorage;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MaxAttachmentUploadManager {

    private static final Map<UUID, UploadSession> SESSIONS = new HashMap<>();

    private MaxAttachmentUploadManager() {
    }

    public static synchronized void start(MinecraftServer server, UUID attachmentId, UUID senderId, UUID recipientId, String caption, long replyToMessageId, String fileName, String mimeType, long totalBytes, byte[] previewBytes) throws IOException {
        cleanupDead(server);
        Path tempPath = MaxAttachmentStorage.createTempPath(server, attachmentId);
        Files.deleteIfExists(tempPath);
        Files.createFile(tempPath);
        SESSIONS.put(attachmentId, new UploadSession(attachmentId, senderId, recipientId, caption, replyToMessageId, fileName, mimeType, totalBytes, previewBytes, tempPath));
    }

    public static synchronized void append(MinecraftServer server, UUID attachmentId, byte[] chunk) throws IOException {
        UploadSession session = SESSIONS.get(attachmentId);
        if (session == null) {
            return;
        }
        try (OutputStream output = Files.newOutputStream(session.tempPath, StandardOpenOption.APPEND)) {
            output.write(chunk);
        }
        session.receivedBytes += chunk.length;
        session.lastTouch = System.currentTimeMillis();
    }

    public static synchronized UploadResult finish(MinecraftServer server, MaxMessengerSavedData data, ServerPlayer sender, UUID attachmentId) throws IOException {
        UploadSession session = SESSIONS.remove(attachmentId);
        if (session == null) {
            return null;
        }
        if (session.receivedBytes != session.totalBytes) {
            Files.deleteIfExists(session.tempPath);
            return null;
        }
        MaxAttachmentStorage.storeFromTemp(server, attachmentId, session.tempPath);
        data.sendAttachmentMessageMetadata(sender, session.recipientId, session.caption, session.replyToMessageId, attachmentId, session.fileName, session.mimeType, session.totalBytes, session.previewBytes);
        return new UploadResult(session.senderId, session.recipientId);
    }

    public static synchronized void cleanupDead(MinecraftServer server) {
        long now = System.currentTimeMillis();
        SESSIONS.values().removeIf(session -> {
            if (now - session.lastTouch > 10 * 60 * 1000L) {
                try {
                    Files.deleteIfExists(session.tempPath);
                } catch (IOException ignored) {
                }
                return true;
            }
            return false;
        });
    }

    public record UploadResult(UUID senderId, UUID recipientId) {
    }

    private static final class UploadSession {
        private final UUID attachmentId;
        private final UUID senderId;
        private final UUID recipientId;
        private final String caption;
        private final long replyToMessageId;
        private final String fileName;
        private final String mimeType;
        private final long totalBytes;
        private final byte[] previewBytes;
        private final Path tempPath;
        private long receivedBytes;
        private long lastTouch;

        private UploadSession(UUID attachmentId, UUID senderId, UUID recipientId, String caption, long replyToMessageId, String fileName, String mimeType, long totalBytes, byte[] previewBytes, Path tempPath) {
            this.attachmentId = attachmentId;
            this.senderId = senderId;
            this.recipientId = recipientId;
            this.caption = caption == null ? "" : caption;
            this.replyToMessageId = replyToMessageId;
            this.fileName = fileName == null ? "file.bin" : fileName;
            this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
            this.totalBytes = totalBytes;
            this.previewBytes = previewBytes == null ? new byte[0] : previewBytes;
            this.tempPath = tempPath;
            this.receivedBytes = 0L;
            this.lastTouch = System.currentTimeMillis();
        }
    }
}
