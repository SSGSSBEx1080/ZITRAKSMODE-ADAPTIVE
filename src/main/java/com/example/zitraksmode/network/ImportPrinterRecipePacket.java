package com.example.zitraksmode.network;

import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Compatibility wrapper for screen code. ModPackets.sendToServer intercepts it
 * and sends PrinterRecipeImportChunkPacket packets instead of this large NBT.
 */
public final class ImportPrinterRecipePacket {
    private final BlockPos printerPos;
    private final PlayerStoredPrinterRecipe recipe;

    public ImportPrinterRecipePacket(BlockPos printerPos, PlayerStoredPrinterRecipe recipe) {
        this.printerPos = printerPos;
        this.recipe = recipe == null ? null : recipe.copy();
    }

    public BlockPos getPrinterPos() {
        return printerPos;
    }

    public PlayerStoredPrinterRecipe getRecipe() {
        return recipe == null ? null : recipe.copy();
    }

    /* Never used by the normal client path after chunking was added. */
    public static void encode(ImportPrinterRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.printerPos);
        buffer.writeNbt(packet.recipe == null ? new CompoundTag() : packet.recipe.save());
    }

    public static ImportPrinterRecipePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag tag = buffer.readNbt();
        return new ImportPrinterRecipePacket(pos, PlayerStoredPrinterRecipe.load(tag));
    }

    public static void handle(
            ImportPrinterRecipePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        // Legacy fallback intentionally does nothing. Large imports are handled
        // only by PrinterRecipeImportChunkPacket.
        contextSupplier.get().setPacketHandled(true);
    }
}
