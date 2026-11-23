package com.example.zitraksmode.network;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Network channel for Medusa settings and the client petrification renderer. */
public final class MedusaNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZitraksMode.MODID, "medusa"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextMessageId;
    private static boolean registered;

    private MedusaNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(MedusaSettingsPacket.class, nextMessageId++)
                .encoder(MedusaSettingsPacket::encode)
                .decoder(MedusaSettingsPacket::decode)
                .consumerMainThread(MedusaSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(PetrificationStatePacket.class, nextMessageId++)
                .encoder(PetrificationStatePacket::encode)
                .decoder(PetrificationStatePacket::decode)
                .consumerMainThread(PetrificationStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(MedusaWavePacket.class, nextMessageId++)
                .encoder(MedusaWavePacket::encode)
                .decoder(MedusaWavePacket::decode)
                .consumerMainThread(MedusaWavePacket::handle)
                .add();
    }
}
