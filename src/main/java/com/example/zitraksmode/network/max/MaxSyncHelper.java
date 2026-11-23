package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class MaxSyncHelper {

    private MaxSyncHelper() {
    }

    public static void syncPair(MinecraftServer server, MaxMessengerSavedData data, UUID a, UUID b) {
        if (server == null || data == null) {
            return;
        }

        ServerPlayer playerA = server.getPlayerList().getPlayer(a);
        if (playerA != null) {
            MaxPackets.sendToPlayer(SyncMaxStatePacket.fromServerState(playerA, data), playerA);
        }

        if (b != null && !b.equals(a)) {
            ServerPlayer playerB = server.getPlayerList().getPlayer(b);
            if (playerB != null) {
                MaxPackets.sendToPlayer(SyncMaxStatePacket.fromServerState(playerB, data), playerB);
            }
        }
    }
}
