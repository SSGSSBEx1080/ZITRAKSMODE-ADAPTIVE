package com.example.zitraksmode.network.max;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class RequestMaxAttachmentDownloadPacket {

    private final UUID attachmentId;

    public RequestMaxAttachmentDownloadPacket(UUID attachmentId) {
        this.attachmentId = attachmentId;
    }

    public UUID getAttachmentId() {
        return attachmentId;
    }

    public static void encode(RequestMaxAttachmentDownloadPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
    }

    public static RequestMaxAttachmentDownloadPacket decode(FriendlyByteBuf buf) {
        return new RequestMaxAttachmentDownloadPacket(buf.readUUID());
    }
}