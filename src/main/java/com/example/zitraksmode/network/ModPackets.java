package com.example.zitraksmode.network;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModPackets {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZitraksMode.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId;

    private ModPackets() {
    }

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(SelectRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectRecipePacket::decode)
                .encoder(SelectRecipePacket::encode)
                .consumerMainThread(SelectRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(StartPrintingPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(StartPrintingPacket::decode)
                .encoder(StartPrintingPacket::encode)
                .consumerMainThread(StartPrintingPacket::handle)
                .add();

        INSTANCE.messageBuilder(AutofillPrinterPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AutofillPrinterPacket::decode)
                .encoder(AutofillPrinterPacket::encode)
                .consumerMainThread(AutofillPrinterPacket::handle)
                .add();

        INSTANCE.messageBuilder(ScanPrinterRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ScanPrinterRecipePacket::decode)
                .encoder(ScanPrinterRecipePacket::encode)
                .consumerMainThread(ScanPrinterRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(DeletePrinterRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeletePrinterRecipePacket::decode)
                .encoder(DeletePrinterRecipePacket::encode)
                .consumerMainThread(DeletePrinterRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(RenamePrinterRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RenamePrinterRecipePacket::decode)
                .encoder(RenamePrinterRecipePacket::encode)
                .consumerMainThread(RenamePrinterRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncPrinterScrollPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SyncPrinterScrollPacket::decode)
                .encoder(SyncPrinterScrollPacket::encode)
                .consumerMainThread(SyncPrinterScrollPacket::handle)
                .add();

        INSTANCE.messageBuilder(SharePrinterRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SharePrinterRecipePacket::decode)
                .encoder(SharePrinterRecipePacket::encode)
                .consumerMainThread(SharePrinterRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(ImportPrinterRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ImportPrinterRecipePacket::decode)
                .encoder(ImportPrinterRecipePacket::encode)
                .consumerMainThread(ImportPrinterRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(PrinterRecipeImportChunkPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PrinterRecipeImportChunkPacket::decode)
                .encoder(PrinterRecipeImportChunkPacket::encode)
                .consumerMainThread(PrinterRecipeImportChunkPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncPrinterRecipeLibraryPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncPrinterRecipeLibraryPacket::decode)
                .encoder(SyncPrinterRecipeLibraryPacket::encode)
                .consumerMainThread(SyncPrinterRecipeLibraryPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncSelectedPrinterRecipePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncSelectedPrinterRecipePacket::decode)
                .encoder(SyncSelectedPrinterRecipePacket::encode)
                .consumerMainThread(SyncSelectedPrinterRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(PrinterRecipeSyncChunkPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PrinterRecipeSyncChunkPacket::decode)
                .encoder(PrinterRecipeSyncChunkPacket::encode)
                .consumerMainThread(PrinterRecipeSyncChunkPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        if (message instanceof ImportPrinterRecipePacket importPacket) {
            if (importPacket.getRecipe() != null) {
                PrinterRecipeImportChunkPacket.sendRecipe(
                        importPacket.getPrinterPos(),
                        importPacket.getRecipe()
                );
            }
            return;
        }

        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
