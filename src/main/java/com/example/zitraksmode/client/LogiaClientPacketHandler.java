package com.example.zitraksmode.client;

import com.example.zitraksmode.network.LogiaNetwork;
import com.example.zitraksmode.util.LogiaGaugeCapability;
import net.minecraft.client.Minecraft;

/** Client-only endpoint for the gauge synchronization packet. */
public final class LogiaClientPacketHandler {
    private LogiaClientPacketHandler() {
    }

    public static void apply(LogiaNetwork.SyncGaugePacket packet) {
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            data.setGauge(packet.gauge());
            data.setKizaruCooldown(packet.kizaruCooldown());
            data.setWhitebeardLockTicks(packet.whitebeardLockTicks());
            data.setWhitebeardCooldownTicks(packet.whitebeardCooldownTicks());
            data.clearDirty();
        });
    }
}
