package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class MaxAttachmentUploadChunkPacket {

    public final UUID attachmentId;
    public final byte[] chunkData;

    public MaxAttachmentUploadChunkPacket(UUID attachmentId, byte[] chunkData) {
        this.attachmentId = attachmentId;
        this.chunkData = chunkData == null ? new byte[0] : chunkData;
    }

    public static void encode(MaxAttachmentUploadChunkPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
        buf.writeByteArray(packet.chunkData);
    }

    public static MaxAttachmentUploadChunkPacket decode(FriendlyByteBuf buf) {
        return new MaxAttachmentUploadChunkPacket(buf.readUUID(), buf.readByteArray());
    }
}