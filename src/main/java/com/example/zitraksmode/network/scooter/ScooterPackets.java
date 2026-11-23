package com.example.zitraksmode.network.scooter;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ScooterPackets {

    private static final String PROTOCOL = "1";
    private static int id = 0;
    private static SimpleChannel channel;

    public static void register() {
        if (channel != null) {
            return;
        }

        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ZitraksMode.MODID, "scooter"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        channel.messageBuilder(ScooterInputPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ScooterInputPacket::decode)
                .encoder(ScooterInputPacket::encode)
                .consumerMainThread(ScooterInputPacket::handle)
                .add();

        channel.messageBuilder(ScooterActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ScooterActionPacket::decode)
                .encoder(ScooterActionPacket::encode)
                .consumerMainThread(ScooterActionPacket::handle)
                .add();
    }

    private static int nextId() {
        return id++;
    }

    public static void sendToServer(Object msg) {
        if (channel != null) {
            channel.sendToServer(msg);
        }
    }
}
