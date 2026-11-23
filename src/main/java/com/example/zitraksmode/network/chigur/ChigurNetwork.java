package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Packets for Chigur dialogue, coin toss, witness question and bed overlay. */
public final class ChigurNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZitraksMode.MODID, "chigur"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextMessageId;
    private static boolean registered;

    private ChigurNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(ChigurDialogOpenPacket.class, nextMessageId++)
                .encoder(ChigurDialogOpenPacket::encode)
                .decoder(ChigurDialogOpenPacket::decode)
                .consumerMainThread(ChigurDialogOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChigurDialogChoicePacket.class, nextMessageId++)
                .encoder(ChigurDialogChoicePacket::encode)
                .decoder(ChigurDialogChoicePacket::decode)
                .consumerMainThread(ChigurDialogChoicePacket::handle)
                .add();
        CHANNEL.messageBuilder(ChigurCoinOpenPacket.class, nextMessageId++)
                .encoder(ChigurCoinOpenPacket::encode)
                .decoder(ChigurCoinOpenPacket::decode)
                .consumerMainThread(ChigurCoinOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChigurCoinChoicePacket.class, nextMessageId++)
                .encoder(ChigurCoinChoicePacket::encode)
                .decoder(ChigurCoinChoicePacket::decode)
                .consumerMainThread(ChigurCoinChoicePacket::handle)
                .add();
        CHANNEL.messageBuilder(ChigurWitnessOpenPacket.class, nextMessageId++)
                .encoder(ChigurWitnessOpenPacket::encode)
                .decoder(ChigurWitnessOpenPacket::decode)
                .consumerMainThread(ChigurWitnessOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChigurWitnessChoicePacket.class, nextMessageId++)
                .encoder(ChigurWitnessChoicePacket::encode)
                .decoder(ChigurWitnessChoicePacket::decode)
                .consumerMainThread(ChigurWitnessChoicePacket::handle)
                .add();
        CHANNEL.messageBuilder(ChigurCinematicPacket.class, nextMessageId++)
                .encoder(ChigurCinematicPacket::encode)
                .decoder(ChigurCinematicPacket::decode)
                .consumerMainThread(ChigurCinematicPacket::handle)
                .add();
    }

    public static void openDialog(ServerPlayer player, int entityId, int aggression) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ChigurDialogOpenPacket(entityId, aggression));
    }

    public static void openCoin(ServerPlayer player, int entityId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ChigurCoinOpenPacket(entityId));
    }

    public static void openWitnessQuestion(ServerPlayer player, int entityId, String huntedName, String refusalText) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ChigurWitnessOpenPacket(entityId, huntedName, refusalText));
    }

    public static void cinematic(ServerPlayer player, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ChigurCinematicPacket(ticks));
    }
}
