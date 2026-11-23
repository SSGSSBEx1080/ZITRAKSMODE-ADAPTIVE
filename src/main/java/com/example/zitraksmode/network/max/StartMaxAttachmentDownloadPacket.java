package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class StartMaxAttachmentDownloadPacket {

    public final UUID attachmentId;
    public final String fileName;
    public final String mimeType;
    public final long totalBytes;

    public StartMaxAttachmentDownloadPacket(UUID attachmentId, String fileName, String mimeType, long totalBytes) {
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.totalBytes = totalBytes;
    }

    public static void encode(StartMaxAttachmentDownloadPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
        buf.writeUtf(packet.fileName, 256);
        buf.writeUtf(packet.mimeType, 128);
        buf.writeLong(packet.totalBytes);
    }

    public static StartMaxAttachmentDownloadPacket decode(FriendlyByteBuf buf) {
        return new StartMaxAttachmentDownloadPacket(buf.readUUID(), buf.readUtf(256), buf.readUtf(128), buf.readLong());
    }
}