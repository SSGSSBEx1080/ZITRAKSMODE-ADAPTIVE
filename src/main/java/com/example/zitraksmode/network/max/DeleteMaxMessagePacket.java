package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxChatMessage;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteMaxMessagePacket {

    private final long messageId;

    public DeleteMaxMessagePacket(long messageId) {
        this.messageId = messageId;
    }

    public static void encode(DeleteMaxMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.messageId);
    }

    public static DeleteMaxMessagePacket decode(FriendlyByteBuf buf) {
        return new DeleteMaxMessagePacket(buf.readLong());
    }

    public static void handle(DeleteMaxMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }

            MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
            if (!data.deleteMessage(sender, packet.messageId)) {
                return;
            }

            MaxChatMessage message = data.getMessage(packet.messageId);
            if (message == null) {
                return;
            }
            MaxSyncHelper.syncPair(sender.getServer(), data, message.getSenderId(), message.getRecipientId());
        });
        ctx.get().setPacketHandled(true);
    }
}
