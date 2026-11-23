package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class FinishMaxAttachmentDownloadPacket {

    public final UUID attachmentId;
    public final boolean success;

    public FinishMaxAttachmentDownloadPacket(UUID attachmentId, boolean success) {
        this.attachmentId = attachmentId;
        this.success = success;
    }

    public static void encode(FinishMaxAttachmentDownloadPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
        buf.writeBoolean(packet.success);
    }

    public static FinishMaxAttachmentDownloadPacket decode(FriendlyByteBuf buf) {
        return new FinishMaxAttachmentDownloadPacket(buf.readUUID(), buf.readBoolean());
    }
}