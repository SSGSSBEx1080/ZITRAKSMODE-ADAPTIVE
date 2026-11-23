package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SendMaxMessagePacket {

    private final UUID recipientId;
    private final String text;
    private final long replyToMessageId;

    public SendMaxMessagePacket(UUID recipientId, String text) {
        this(recipientId, text, 0L);
    }

    public SendMaxMessagePacket(UUID recipientId, String text, long replyToMessageId) {
        this.recipientId = recipientId;
        this.text = text;
        this.replyToMessageId = replyToMessageId;
    }

    public static void encode(SendMaxMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.recipientId);
        buf.writeUtf(packet.text, 4096);
        buf.writeLong(packet.replyToMessageId);
    }

    public static SendMaxMessagePacket decode(FriendlyByteBuf buf) {
        return new SendMaxMessagePacket(buf.readUUID(), buf.readUtf(4096), buf.readLong());
    }

    public static void handle(SendMaxMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }

            String normalized = packet.text == null ? "" : packet.text.replace("\r", "").trim();
            if (normalized.isBlank()) {
                return;
            }

            MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
            data.ensureOnlineProfiles(sender.getServer());
            data.ensureProfile(sender);
            if (data.getProfile(packet.recipientId) == null) {
                return;
            }

            data.sendTextMessage(sender, packet.recipientId, normalized, packet.replyToMessageId);
            MaxSyncHelper.syncPair(sender.getServer(), data, sender.getUUID(), packet.recipientId);
        });
        ctx.get().setPacketHandled(true);
    }
}
