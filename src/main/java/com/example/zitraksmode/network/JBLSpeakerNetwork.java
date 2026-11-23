package com.example.zitraksmode.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class JBLSpeakerNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("zitraksmode", "jbl_speaker_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean registered = false;

    private JBLSpeakerNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.registerMessage(packetId++, UploadTrackStartPacket.class, UploadTrackStartPacket::encode, UploadTrackStartPacket::decode, UploadTrackStartPacket::handle);
        CHANNEL.registerMessage(packetId++, UploadTrackChunkPacket.class, UploadTrackChunkPacket::encode, UploadTrackChunkPacket::decode, UploadTrackChunkPacket::handle);
        CHANNEL.registerMessage(packetId++, UploadTrackFinishPacket.class, UploadTrackFinishPacket::encode, UploadTrackFinishPacket::decode, UploadTrackFinishPacket::handle);
        CHANNEL.registerMessage(packetId++, RequestSpeakerLibraryPacket.class, RequestSpeakerLibraryPacket::encode, RequestSpeakerLibraryPacket::decode, RequestSpeakerLibraryPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncSpeakerLibraryPacket.class, SyncSpeakerLibraryPacket::encode, SyncSpeakerLibraryPacket::decode, SyncSpeakerLibraryPacket::handle);
    }

    public static <MSG> void sendToServer(MSG message) {
        register();
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        register();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToTrackingAndSelf(Entity entity, MSG message) {
        register();
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }
}
