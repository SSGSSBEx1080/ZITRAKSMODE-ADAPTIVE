package com.example.zitraksmode.jbl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class SpeakerTrackMeta {

    private final String trackId;
    private final UUID ownerId;
    private final String fileName;
    private String title;
    private String author;
    private final long durationMs;
    private final long sizeBytes;
    private int likes;
    private final long createdAt;
    private final String sha1;

    public SpeakerTrackMeta(String trackId,
                            UUID ownerId,
                            String fileName,
                            String title,
                            String author,
                            long durationMs,
                            long sizeBytes,
                            int likes,
                            long createdAt,
                            String sha1) {
        this.trackId = trackId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.title = title;
        this.author = author;
        this.durationMs = durationMs;
        this.sizeBytes = sizeBytes;
        this.likes = likes;
        this.createdAt = createdAt;
        this.sha1 = sha1;
    }

    public String getTrackId() {
        return trackId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getSha1() {
        return sha1;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("trackId", trackId);
        tag.putUUID("ownerId", ownerId);
        tag.putString("fileName", fileName);
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putLong("durationMs", durationMs);
        tag.putLong("sizeBytes", sizeBytes);
        tag.putInt("likes", likes);
        tag.putLong("createdAt", createdAt);
        tag.putString("sha1", sha1);
        return tag;
    }

    public static SpeakerTrackMeta fromTag(CompoundTag tag) {
        return new SpeakerTrackMeta(
                tag.getString("trackId"),
                tag.getUUID("ownerId"),
                tag.getString("fileName"),
                tag.getString("title"),
                tag.getString("author"),
                tag.getLong("durationMs"),
                tag.getLong("sizeBytes"),
                tag.getInt("likes"),
                tag.getLong("createdAt"),
                tag.getString("sha1")
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(trackId, 128);
        buf.writeUUID(ownerId);
        buf.writeUtf(fileName, 256);
        buf.writeUtf(title, 256);
        buf.writeUtf(author, 256);
        buf.writeVarLong(durationMs);
        buf.writeVarLong(sizeBytes);
        buf.writeVarInt(likes);
        buf.writeVarLong(createdAt);
        buf.writeUtf(sha1, 64);
    }

    public static SpeakerTrackMeta read(FriendlyByteBuf buf) {
        return new SpeakerTrackMeta(
                buf.readUtf(128),
                buf.readUUID(),
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readUtf(64)
        );
    }
}
