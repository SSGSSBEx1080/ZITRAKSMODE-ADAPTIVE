package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import com.example.zitraksmode.printer.custom.PrinterRecipeLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RenamePrinterRecipePacket {

    private final BlockPos printerPos;
    private final String recipeId;
    private final String newName;

    public RenamePrinterRecipePacket(BlockPos printerPos, String recipeId, String newName) {
        this.printerPos = printerPos;
        this.recipeId = recipeId;
        this.newName = newName;
    }

    public static void encode(RenamePrinterRecipePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.printerPos);
        buf.writeUtf(packet.recipeId);
        buf.writeUtf(packet.newName);
    }

    public static RenamePrinterRecipePacket decode(FriendlyByteBuf buf) {
        return new RenamePrinterRecipePacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(RenamePrinterRecipePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            PlayerPrinterRecipeLibrary library = PrinterRecipeCapability.getLibrary(player);
            if (library == null) {
                player.sendSystemMessage(Component.literal("§cНе удалось открыть библиотеку рецептов"));
                return;
            }

            PlayerPrinterRecipeLibrary.RenameResult result = library.renameRecipe(packet.recipeId, packet.newName);
            switch (result) {
                case NOT_FOUND -> {
                    player.sendSystemMessage(Component.literal("§cРецепт не найден"));
                    return;
                }
                case EMPTY_NAME -> {
                    player.sendSystemMessage(Component.literal("§cВведите название рецепта"));
                    return;
                }
                case SUCCESS -> {
                }
            }

            ModPackets.sendToPlayer(SyncPrinterRecipeLibraryPacket.fromLibrary(library), player);

            var be = player.level.getBlockEntity(packet.printerPos);
            if (be instanceof Printer3DBlockEntity printer) {
                if (packet.recipeId.equals(printer.getSelectedCustomRecipeId())) {
                    PlayerStoredPrinterRecipe recipe = library.getRecipe(packet.recipeId).orElse(null);
                    if (recipe != null) {
                        ModPackets.sendToPlayer(new SyncSelectedPrinterRecipePacket(packet.printerPos, recipe), player);
                    }
                }
            }

            String normalized = PrinterRecipeLimits.normalizeName(packet.newName);
            player.sendSystemMessage(Component.literal("§aРецепт переименован в \"" + normalized + "\""));
        });

        ctx.get().setPacketHandled(true);
    }
}
