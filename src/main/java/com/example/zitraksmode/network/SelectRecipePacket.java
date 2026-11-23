package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.PrinterRecipe;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SelectRecipePacket {
    private final BlockPos pos;
    private final String recipeId;

    public SelectRecipePacket(BlockPos pos, PrinterRecipe recipe) {
        this.pos = pos;
        this.recipeId = recipe.getId();
    }

    public SelectRecipePacket(BlockPos pos, String recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public static void encode(SelectRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.recipeId, 128);
    }

    public static SelectRecipePacket decode(FriendlyByteBuf buffer) {
        return new SelectRecipePacket(buffer.readBlockPos(), buffer.readUtf(128));
    }

    public static void handle(
            SelectRecipePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (!(player.level.getBlockEntity(packet.pos) instanceof Printer3DBlockEntity printer)) {
                return;
            }

            PrinterRecipe builtin = PrinterRecipe.getById(packet.recipeId);
            if (builtin != null) {
                printer.setSelectedRecipe(builtin);
                ModPackets.sendToPlayer(new SyncSelectedPrinterRecipePacket(packet.pos), player);
                return;
            }

            PlayerStoredPrinterRecipe customRecipe = null;
            var library = PrinterRecipeCapability.getLibrary(player);
            if (library != null) {
                customRecipe = library.getRecipe(packet.recipeId).orElse(null);
            }

            if (customRecipe != null) {
                printer.setSelectedCustomRecipe(packet.recipeId, player);
                ModPackets.sendToPlayer(
                        new SyncSelectedPrinterRecipePacket(packet.pos, customRecipe),
                        player
                );
                PrinterRecipeSyncChunkPacket.sendRecipe(player, packet.pos, customRecipe);
            }
        });

        context.setPacketHandled(true);
    }
}
