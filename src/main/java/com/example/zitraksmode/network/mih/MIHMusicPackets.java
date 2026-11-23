package com.example.zitraksmode.network.mih;

import com.example.zitraksmode.ZitraksMode;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class MIHMusicPackets {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZitraksMode.MODID, "mih_music"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(StopMadeInHeavenSoundPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StopMadeInHeavenSoundPacket::encode)
                .decoder(StopMadeInHeavenSoundPacket::decode)
                .consumerMainThread(StopMadeInHeavenSoundPacket::handle)
                .add();
    }

    public static <MSG> void sendToAll(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}