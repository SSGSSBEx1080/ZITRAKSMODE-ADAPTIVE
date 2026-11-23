package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MarkMaxConversationReadPacket {

    private final UUID otherUserId;

    public MarkMaxConversationReadPacket(UUID otherUserId) {
        this.otherUserId = otherUserId;
    }

    public static void encode(MarkMaxConversationReadPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.otherUserId);
    }

    public static MarkMaxConversationReadPacket decode(FriendlyByteBuf buf) {
        return new MarkMaxConversationReadPacket(buf.readUUID());
    }

    public static void handle(MarkMaxConversationReadPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer reader = ctx.get().getSender();
            if (reader == null || reader.getServer() == null) {
                return;
            }

            MaxMessengerSavedData data = MaxMessengerSavedData.get(reader.getServer());
            data.markConversationRead(reader.getUUID(), packet.otherUserId);
            MaxPackets.sendToPlayer(SyncMaxStatePacket.fromServerState(reader, data), reader);

            if (!reader.getUUID().equals(packet.otherUserId)) {
                ServerPlayer other = reader.getServer().getPlayerList().getPlayer(packet.otherUserId);
                if (other != null) {
                    MaxPackets.sendToPlayer(SyncMaxStatePacket.fromServerState(other, data), other);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
