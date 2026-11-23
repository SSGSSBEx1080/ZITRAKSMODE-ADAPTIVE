package com.example.zitraksmode.network.max;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class MaxPackets {

    private static final String PROTOCOL_VERSION = "10";

    private static SimpleChannel INSTANCE;
    private static boolean registered = false;
    private static int packetId = 0;

    private MaxPackets() {
    }

    private static int id() {
        return packetId++;
    }

    public static void register() {
        if (registered) {
            return;
        }

        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ZitraksMode.MODID, "max"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        INSTANCE.messageBuilder(RequestMaxSyncPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestMaxSyncPacket::decode)
                .encoder(RequestMaxSyncPacket::encode)
                .consumerMainThread(RequestMaxSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(RegisterMaxProfilePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RegisterMaxProfilePacket::decode)
                .encoder(RegisterMaxProfilePacket::encode)
                .consumerMainThread(RegisterMaxProfilePacket::handle)
                .add();

        INSTANCE.messageBuilder(SendMaxMessagePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SendMaxMessagePacket::decode)
                .encoder(SendMaxMessagePacket::encode)
                .consumerMainThread(SendMaxMessagePacket::handle)
                .add();

        INSTANCE.messageBuilder(EditMaxMessagePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(EditMaxMessagePacket::decode)
                .encoder(EditMaxMessagePacket::encode)
                .consumerMainThread(EditMaxMessagePacket::handle)
                .add();

        INSTANCE.messageBuilder(DeleteMaxMessagePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeleteMaxMessagePacket::decode)
                .encoder(DeleteMaxMessagePacket::encode)
                .consumerMainThread(DeleteMaxMessagePacket::handle)
                .add();

        INSTANCE.messageBuilder(ForwardMaxMessagePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ForwardMaxMessagePacket::decode)
                .encoder(ForwardMaxMessagePacket::encode)
                .consumerMainThread(ForwardMaxMessagePacket::handle)
                .add();

        INSTANCE.messageBuilder(UploadMaxAttachmentPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UploadMaxAttachmentPacket::decode)
                .encoder(UploadMaxAttachmentPacket::encode)
                .consumerMainThread(UploadMaxAttachmentPacket::handle)
                .add();

        INSTANCE.messageBuilder(StartMaxAttachmentUploadPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(StartMaxAttachmentUploadPacket::decode)
                .encoder(StartMaxAttachmentUploadPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleStartUpload)
                .add();

        INSTANCE.messageBuilder(MaxAttachmentUploadChunkPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MaxAttachmentUploadChunkPacket::decode)
                .encoder(MaxAttachmentUploadChunkPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleUploadChunk)
                .add();

        INSTANCE.messageBuilder(FinishMaxAttachmentUploadPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(FinishMaxAttachmentUploadPacket::decode)
                .encoder(FinishMaxAttachmentUploadPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleFinishUpload)
                .add();

        INSTANCE.messageBuilder(RequestMaxAttachmentDownloadPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestMaxAttachmentDownloadPacket::decode)
                .encoder(RequestMaxAttachmentDownloadPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleRequestDownload)
                .add();

        INSTANCE.messageBuilder(StartMaxAttachmentDownloadPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StartMaxAttachmentDownloadPacket::decode)
                .encoder(StartMaxAttachmentDownloadPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleStartDownloadClient)
                .add();

        INSTANCE.messageBuilder(MaxAttachmentDownloadChunkPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(MaxAttachmentDownloadChunkPacket::decode)
                .encoder(MaxAttachmentDownloadChunkPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleDownloadChunkClient)
                .add();

        INSTANCE.messageBuilder(FinishMaxAttachmentDownloadPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FinishMaxAttachmentDownloadPacket::decode)
                .encoder(FinishMaxAttachmentDownloadPacket::encode)
                .consumerMainThread(MaxAttachmentTransferHandlers::handleFinishDownloadClient)
                .add();

        INSTANCE.messageBuilder(SendMaxGiftPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SendMaxGiftPacket::decode)
                .encoder(SendMaxGiftPacket::encode)
                .consumerMainThread(SendMaxGiftPacket::handle)
                .add();

        INSTANCE.messageBuilder(RespondMaxGiftPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RespondMaxGiftPacket::decode)
                .encoder(RespondMaxGiftPacket::encode)
                .consumerMainThread(RespondMaxGiftPacket::handle)
                .add();

        INSTANCE.messageBuilder(MarkMaxConversationReadPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MarkMaxConversationReadPacket::decode)
                .encoder(MarkMaxConversationReadPacket::encode)
                .consumerMainThread(MarkMaxConversationReadPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncMaxStatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncMaxStatePacket::decode)
                .encoder(SyncMaxStatePacket::encode)
                .consumerMainThread(SyncMaxStatePacket::handle)
                .add();

        registered = true;
    }

    public static <MSG> void sendToServer(MSG message) {
        if (INSTANCE == null) {
            throw new IllegalStateException("MAX packets are not registered yet");
        }
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (INSTANCE == null) {
            throw new IllegalStateException("MAX packets are not registered yet");
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
