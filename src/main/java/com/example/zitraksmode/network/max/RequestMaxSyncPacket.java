package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestMaxSyncPacket {

    public static void encode(RequestMaxSyncPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestMaxSyncPacket decode(FriendlyByteBuf buf) {
        return new RequestMaxSyncPacket();
    }

    public static void handle(RequestMaxSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.getServer() == null) {
                return;
            }

            MaxMessengerSavedData data = MaxMessengerSavedData.get(player.getServer());
            data.ensureOnlineProfiles(player.getServer());
            data.ensureProfile(player);
            MaxPackets.sendToPlayer(SyncMaxStatePacket.fromServerState(player, data), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
