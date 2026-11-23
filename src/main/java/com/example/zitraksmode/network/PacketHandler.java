package com.example.zitraksmode.network;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "4";
    private static SimpleChannel CHANNEL;
    private static int packetId = 0;

    public static void registerPackets() {
        packetId = 0;
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ZitraksMode.MODID, "jbl"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(packetId++, SelectTrackPacket.class, SelectTrackPacket::encode, SelectTrackPacket::decode, SelectTrackPacket::handle);
        CHANNEL.registerMessage(packetId++, SpeakerStatePacket.class, SpeakerStatePacket::encode, SpeakerStatePacket::decode, SpeakerStatePacket::handle);
        CHANNEL.registerMessage(packetId++, VolumeChangePacket.class, VolumeChangePacket::encode, VolumeChangePacket::decode, VolumeChangePacket::handle);
        CHANNEL.registerMessage(packetId++, StopSpeakerPacket.class, StopSpeakerPacket::encode, StopSpeakerPacket::decode, StopSpeakerPacket::handle);
        CHANNEL.registerMessage(packetId++, PauseSpeakerPacket.class, PauseSpeakerPacket::encode, PauseSpeakerPacket::decode, PauseSpeakerPacket::handle);
        CHANNEL.registerMessage(packetId++, SeekSpeakerPacket.class, SeekSpeakerPacket::encode, SeekSpeakerPacket::decode, SeekSpeakerPacket::handle);
        CHANNEL.registerMessage(packetId++, RequestSharedTrackPacket.class, RequestSharedTrackPacket::encode, RequestSharedTrackPacket::decode, RequestSharedTrackPacket::handle);
        CHANNEL.registerMessage(packetId++, SharedTrackStartPacket.class, SharedTrackStartPacket::encode, SharedTrackStartPacket::decode, SharedTrackStartPacket::handle);
        CHANNEL.registerMessage(packetId++, SharedTrackChunkPacket.class, SharedTrackChunkPacket::encode, SharedTrackChunkPacket::decode, SharedTrackChunkPacket::handle);
        CHANNEL.registerMessage(packetId++, SharedTrackFinishPacket.class, SharedTrackFinishPacket::encode, SharedTrackFinishPacket::decode, SharedTrackFinishPacket::handle);
        CHANNEL.registerMessage(packetId++, DeleteTrackPacket.class, DeleteTrackPacket::encode, DeleteTrackPacket::decode, DeleteTrackPacket::handle);
        CHANNEL.registerMessage(packetId++, LikeTrackPacket.class, LikeTrackPacket::encode, LikeTrackPacket::decode, LikeTrackPacket::handle);
        CHANNEL.registerMessage(packetId++, RenameTrackPacket.class, RenameTrackPacket::encode, RenameTrackPacket::decode, RenameTrackPacket::handle);
        CHANNEL.registerMessage(packetId++, ShareTrackPacket.class, ShareTrackPacket::encode, ShareTrackPacket::decode, ShareTrackPacket::handle);
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllTracking(MSG message, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    public static <MSG> void sendToTrackingAndSelf(MSG message, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}
