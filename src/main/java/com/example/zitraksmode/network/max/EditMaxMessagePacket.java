package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxChatMessage;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class EditMaxMessagePacket {

    private final long messageId;
    private final String newText;

    public EditMaxMessagePacket(long messageId, String newText) {
        this.messageId = messageId;
        this.newText = newText;
    }

    public static void encode(EditMaxMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.messageId);
        buf.writeUtf(packet.newText, 4096);
    }

    public static EditMaxMessagePacket decode(FriendlyByteBuf buf) {
        return new EditMaxMessagePacket(buf.readLong(), buf.readUtf(4096));
    }

    public static void handle(EditMaxMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }

            MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
            if (!data.editMessage(sender, packet.messageId, packet.newText)) {
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
