package com.example.zitraksmode.network.labubu;

import com.example.zitraksmode.ZitraksMode;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class LabubuCasePackets {

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZitraksMode.MODID, "labubu_case"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(OpenLabubuCasePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenLabubuCasePacket::encode)
                .decoder(OpenLabubuCasePacket::decode)
                .consumerMainThread(OpenLabubuCasePacket::handle)
                .add();

        CHANNEL.messageBuilder(ClaimLabubuCaseRewardPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClaimLabubuCaseRewardPacket::encode)
                .decoder(ClaimLabubuCaseRewardPacket::decode)
                .consumerMainThread(ClaimLabubuCaseRewardPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }
}