package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxChatMessage;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ForwardMaxMessagePacket {

    private final long sourceMessageId;
    private final UUID newRecipientId;

    public ForwardMaxMessagePacket(long sourceMessageId, UUID newRecipientId) {
        this.sourceMessageId = sourceMessageId;
        this.newRecipientId = newRecipientId;
    }

    public static void encode(ForwardMaxMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.sourceMessageId);
        buf.writeUUID(packet.newRecipientId);
    }

    public static ForwardMaxMessagePacket decode(FriendlyByteBuf buf) {
        return new ForwardMaxMessagePacket(buf.readLong(), buf.readUUID());
    }

    public static void handle(ForwardMaxMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }
            MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
            MaxChatMessage forwarded = data.forwardMessage(sender, packet.newRecipientId, packet.sourceMessageId);
            if (forwarded == null) {
                return;
            }
            MaxSyncHelper.syncPair(sender.getServer(), data, sender.getUUID(), packet.newRecipientId);
        });
        ctx.get().setPacketHandled(true);
    }
}
