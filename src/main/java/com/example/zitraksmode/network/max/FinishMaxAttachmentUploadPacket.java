package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class FinishMaxAttachmentUploadPacket {

    public final UUID attachmentId;

    public FinishMaxAttachmentUploadPacket(UUID attachmentId) {
        this.attachmentId = attachmentId;
    }

    public static void encode(FinishMaxAttachmentUploadPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
    }

    public static FinishMaxAttachmentUploadPacket decode(FriendlyByteBuf buf) {
        return new FinishMaxAttachmentUploadPacket(buf.readUUID());
    }
}