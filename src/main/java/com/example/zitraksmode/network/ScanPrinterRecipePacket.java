package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import com.example.zitraksmode.printer.custom.PrinterRecipeLimits;
import com.example.zitraksmode.printer.custom.PrinterRecipeScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScanPrinterRecipePacket {

    private final BlockPos printerPos;
    private final String displayName;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public ScanPrinterRecipePacket(BlockPos printerPos, String displayName, int sizeX, int sizeY, int sizeZ) {
        this.printerPos = printerPos;
        this.displayName = displayName;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static void encode(ScanPrinterRecipePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.printerPos);
        buf.writeUtf(packet.displayName);
        buf.writeVarInt(packet.sizeX);
        buf.writeVarInt(packet.sizeY);
        buf.writeVarInt(packet.sizeZ);
    }

    public static ScanPrinterRecipePacket decode(FriendlyByteBuf buf) {
        return new ScanPrinterRecipePacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(ScanPrinterRecipePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            var be = player.level.getBlockEntity(packet.printerPos);
            if (!(be instanceof Printer3DBlockEntity printer)) {
                player.sendSystemMessage(Component.literal("§cПринтер не найден"));
                return;
            }

            PlayerPrinterRecipeLibrary library = PrinterRecipeCapability.getLibrary(player);
            if (library == null) {
                player.sendSystemMessage(Component.literal("§cНе удалось открыть библиотеку рецептов"));
                return;
            }

            if (library.isFull()) {
                player.sendSystemMessage(Component.literal("§cЛимит рецептов достигнут: " + PrinterRecipeLimits.MAX_RECIPES_PER_PLAYER));
                return;
            }

            String normalizedName = PrinterRecipeLimits.normalizeName(packet.displayName);
            if (normalizedName.isBlank()) {
                player.sendSystemMessage(Component.literal("§cВведите название рецепта"));
                return;
            }

            String recipeId = library.createNextRecipeId();
            PrinterRecipeScanner.ScanResult result = PrinterRecipeScanner.scanFromPrinter(
                    printer,
                    normalizedName,
                    packet.sizeX,
                    packet.sizeY,
                    packet.sizeZ,
                    recipeId
            );

            if (!result.success() || result.recipe() == null) {
                player.sendSystemMessage(Component.literal("§c" + result.message()));
                return;
            }

            PlayerPrinterRecipeLibrary.AddResult addResult = library.addRecipe(result.recipe());
            if (addResult != PlayerPrinterRecipeLibrary.AddResult.SUCCESS) {
                switch (addResult) {
                    case LIMIT_REACHED -> player.sendSystemMessage(Component.literal("§cЛимит рецептов достигнут: " + PrinterRecipeLimits.MAX_RECIPES_PER_PLAYER));
                    case DUPLICATE_ID -> player.sendSystemMessage(Component.literal("§cКонфликт идентификатора рецепта"));
                    default -> player.sendSystemMessage(Component.literal("§cНе удалось сохранить рецепт"));
                }
                return;
            }

            ModPackets.sendToPlayer(SyncPrinterRecipeLibraryPacket.fromLibrary(library), player);
            player.sendSystemMessage(Component.literal("§aРецепт \"" + result.recipe().getDisplayName() + "\" сохранён. Блоков: " + result.blockCount()));
        });
        ctx.get().setPacketHandled(true);
    }
}
