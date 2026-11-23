package com.example.zitraksmode.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("zitraksmode", "deathnote_net"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(OpenDeathNotePacket.class, id())
                .encoder(OpenDeathNotePacket::encode)
                .decoder(OpenDeathNotePacket::decode)
                .consumerMainThread(OpenDeathNotePacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestDeathPacket.class, id())
                .encoder(RequestDeathPacket::encode)
                .decoder(RequestDeathPacket::decode)
                .consumerMainThread(RequestDeathPacket::handle)
                .add();

        INSTANCE.messageBuilder(DeathNoteMusicPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DeathNoteMusicPacket::encode)
                .decoder(DeathNoteMusicPacket::decode)
                .consumerMainThread(DeathNoteMusicPacket::handle)
                .add();

        INSTANCE.messageBuilder(SniperUltimatePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(SniperUltimatePacket::toBytes)
                .decoder(SniperUltimatePacket::new)
                .consumerMainThread(SniperUltimatePacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}