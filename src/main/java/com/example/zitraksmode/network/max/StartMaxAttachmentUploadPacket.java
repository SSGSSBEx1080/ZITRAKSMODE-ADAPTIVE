package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class StartMaxAttachmentUploadPacket {

    public final UUID attachmentId;
    public final UUID recipientId;
    public final String caption;
    public final long replyToMessageId;
    public final String fileName;
    public final String mimeType;
    public final long totalBytes;
    public final byte[] previewBytes;

    public StartMaxAttachmentUploadPacket(UUID attachmentId, UUID recipientId, String caption, long replyToMessageId, String fileName, String mimeType, long totalBytes, byte[] previewBytes) {
        this.attachmentId = attachmentId;
        this.recipientId = recipientId;
        this.caption = caption;
        this.replyToMessageId = replyToMessageId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.totalBytes = totalBytes;
        this.previewBytes = previewBytes == null ? new byte[0] : previewBytes;
    }

    public static void encode(StartMaxAttachmentUploadPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
        buf.writeUUID(packet.recipientId);
        buf.writeUtf(packet.caption == null ? "" : packet.caption, 4096);
        buf.writeLong(packet.replyToMessageId);
        buf.writeUtf(packet.fileName, 256);
        buf.writeUtf(packet.mimeType, 128);
        buf.writeLong(packet.totalBytes);
        buf.writeByteArray(packet.previewBytes);
    }

    public static StartMaxAttachmentUploadPacket decode(FriendlyByteBuf buf) {
        return new StartMaxAttachmentUploadPacket(
                buf.readUUID(),
                buf.readUUID(),
                buf.readUtf(4096),
                buf.readLong(),
                buf.readUtf(256),
                buf.readUtf(128),
                buf.readLong(),
                buf.readByteArray()
        );
    }
}