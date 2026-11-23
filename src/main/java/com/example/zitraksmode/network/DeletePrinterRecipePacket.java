package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeletePrinterRecipePacket {

    private final BlockPos printerPos;
    private final String recipeId;

    public DeletePrinterRecipePacket(BlockPos printerPos, String recipeId) {
        this.printerPos = printerPos;
        this.recipeId = recipeId;
    }

    public static void encode(DeletePrinterRecipePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.printerPos);
        buf.writeUtf(packet.recipeId);
    }

    public static DeletePrinterRecipePacket decode(FriendlyByteBuf buf) {
        return new DeletePrinterRecipePacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(DeletePrinterRecipePacket packet, Supplier<NetworkEvent.Context> ctx) {
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

            boolean removed = library.removeRecipe(packet.recipeId);
            if (!removed) {
                player.sendSystemMessage(Component.literal("§cРецепт не найден"));
                return;
            }

            var be = player.level.getBlockEntity(packet.printerPos);
            if (be instanceof Printer3DBlockEntity printer) {
                if (packet.recipeId.equals(printer.getSelectedCustomRecipeId())) {
                    printer.clearSelectedCustomRecipeSelection();
                    ModPackets.sendToPlayer(new SyncSelectedPrinterRecipePacket(packet.printerPos), player);
                }
            }

            ModPackets.sendToPlayer(SyncPrinterRecipeLibraryPacket.fromLibrary(library), player);
            player.sendSystemMessage(Component.literal("§eРецепт удалён"));
        });

        ctx.get().setPacketHandled(true);
    }
}
