package com.example.zitraksmode.jbl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class SpeakerTrackUploadSession {

    private final UUID sessionId;
    private final UUID playerId;
    private final String fileName;
    private final String title;
    private final String author;
    private final long expectedSize;
    private final long durationMs;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private int nextChunkIndex = 0;
    private boolean finished = false;

    public SpeakerTrackUploadSession(UUID sessionId,
                                     UUID playerId,
                                     String fileName,
                                     String title,
                                     String author,
                                     long expectedSize,
                                     long durationMs) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.fileName = fileName;
        this.title = title;
        this.author = author;
        this.expectedSize = expectedSize;
        this.durationMs = durationMs;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public long getExpectedSize() {
        return expectedSize;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public synchronized void appendChunk(int chunkIndex, byte[] data, long maxUploadSize) throws IOException {
        if (finished) {
            throw new IOException("Upload session already finished: " + sessionId);
        }
        if (chunkIndex != nextChunkIndex) {
            throw new IOException("Invalid chunk order for session " + sessionId + ": expected=" + nextChunkIndex + ", got=" + chunkIndex);
        }
        if ((long) buffer.size() + data.length > maxUploadSize) {
            throw new IOException("Track is too large. Max size=" + maxUploadSize + " bytes");
        }

        buffer.write(data);
        nextChunkIndex++;
    }

    public synchronized byte[] finish() throws IOException {
        if (finished) {
            throw new IOException("Upload session already finished: " + sessionId);
        }

        finished = true;
        byte[] bytes = buffer.toByteArray();

        if (expectedSize > 0 && bytes.length != expectedSize) {
            throw new IOException("Upload size mismatch. Expected=" + expectedSize + ", actual=" + bytes.length);
        }

        return bytes;
    }
}
