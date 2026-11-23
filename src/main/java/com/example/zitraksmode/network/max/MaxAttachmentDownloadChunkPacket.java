package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class MaxAttachmentDownloadChunkPacket {

    public final UUID attachmentId;
    public final byte[] chunkData;

    public MaxAttachmentDownloadChunkPacket(UUID attachmentId, byte[] chunkData) {
        this.attachmentId = attachmentId;
        this.chunkData = chunkData == null ? new byte[0] : chunkData;
    }

    public static void encode(MaxAttachmentDownloadChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
        buf.writeByteArray(packet.chunkData);
    }

    public static MaxAttachmentDownloadChunkPacket decode(FriendlyByteBuf buf) {
        return new MaxAttachmentDownloadChunkPacket(buf.readUUID(), buf.readByteArray());
    }
}