package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UploadMaxAttachmentPacket {

    private final UUID recipientId;
    private final String caption;
    private final long replyToMessageId;
    private final String fileName;
    private final String mimeType;
    private final byte[] data;

    public UploadMaxAttachmentPacket(UUID recipientId, String caption, long replyToMessageId, String fileName, String mimeType, byte[] data) {
        this.recipientId = recipientId;
        this.caption = caption;
        this.replyToMessageId = replyToMessageId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.data = data == null ? new byte[0] : data;
    }

    public static void encode(UploadMaxAttachmentPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.recipientId);
        buf.writeUtf(packet.caption == null ? "" : packet.caption, 4096);
        buf.writeLong(packet.replyToMessageId);
        buf.writeUtf(packet.fileName, 256);
        buf.writeUtf(packet.mimeType, 128);
        buf.writeByteArray(packet.data);
    }

    public static UploadMaxAttachmentPacket decode(FriendlyByteBuf buf) {
        return new UploadMaxAttachmentPacket(
                buf.readUUID(),
                buf.readUtf(4096),
                buf.readLong(),
                buf.readUtf(256),
                buf.readUtf(128),
                buf.readByteArray()
        );
    }

    public static void handle(UploadMaxAttachmentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }
            MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
            try {
                data.sendAttachmentMessage(sender, packet.recipientId, packet.caption, packet.replyToMessageId, packet.fileName, packet.mimeType, packet.data);
                MaxSyncHelper.syncPair(sender.getServer(), data, sender.getUUID(), packet.recipientId);
            } catch (Exception ignored) {
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
