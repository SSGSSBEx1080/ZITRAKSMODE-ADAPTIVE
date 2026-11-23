package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-to-client chunk stream for a selected large printer recipe. */
public final class PrinterRecipeSyncChunkPacket {
    private static final Map<String, PendingRecipe> CLIENT_PENDING = new HashMap<>();

    private final BlockPos printerPos;
    private final UUID transferId;
    private final int chunkIndex;
    private final int totalChunks;
    private final CompoundTag chunk;

    public PrinterRecipeSyncChunkPacket(
            BlockPos printerPos,
            UUID transferId,
            int chunkIndex,
            int totalChunks,
            CompoundTag chunk
    ) {
        this.printerPos = printerPos;
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.chunk = chunk;
    }

    public static void sendRecipe(
            ServerPlayer player,
            BlockPos printerPos,
            PlayerStoredPrinterRecipe recipe
    ) {
        UUID transferId = UUID.randomUUID();
        int totalChunks = PrinterRecipeChunkData.totalChunks(recipe);

        for (int index = 0; index < totalChunks; index++) {
            ModPackets.sendToPlayer(
                    new PrinterRecipeSyncChunkPacket(
                            printerPos,
                            transferId,
                            index,
                            totalChunks,
                            PrinterRecipeChunkData.createChunk(recipe, index)
                    ),
                    player
            );
        }
    }

    public static void encode(PrinterRecipeSyncChunkPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.printerPos);
        buffer.writeUUID(packet.transferId);
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeVarInt(packet.totalChunks);
        buffer.writeNbt(packet.chunk);
    }

    public static PrinterRecipeSyncChunkPacket decode(FriendlyByteBuf buffer) {
        return new PrinterRecipeSyncChunkPacket(
                buffer.readBlockPos(),
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readNbt()
        );
    }

    public static void handle(
            PrinterRecipeSyncChunkPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PrinterRecipeSyncChunkPacket packet) {
        String key = packet.printerPos.asLong() + ":" + packet.transferId;
        PendingRecipe pending = CLIENT_PENDING.get(key);

        if (pending == null) {
            if (packet.chunkIndex != 0 || !packet.chunk.getBoolean("Meta")) {
                return;
            }

            pending = new PendingRecipe(
                    packet.totalChunks,
                    PrinterRecipeChunkData.createRecipe(packet.chunk)
            );
            CLIENT_PENDING.put(key, pending);
        }

        if (pending.receivedChunks.containsKey(packet.chunkIndex)) {
            return;
        }

        PrinterRecipeChunkData.appendChunk(pending.recipe, packet.chunk);
        pending.receivedChunks.put(packet.chunkIndex, Boolean.TRUE);

        if (pending.receivedChunks.size() < pending.totalChunks) {
            return;
        }

        CLIENT_PENDING.remove(key);

        if (Minecraft.getInstance().level == null) {
            return;
        }

        if (!(Minecraft.getInstance().level.getBlockEntity(packet.printerPos)
                instanceof Printer3DBlockEntity printer)) {
            return;
        }

        printer.setSelectedCustomRecipe(pending.recipe.getRecipeId());
        printer.setClientPreviewCustomRecipe(pending.recipe);
    }

    private static final class PendingRecipe {
        private final int totalChunks;
        private final PlayerStoredPrinterRecipe recipe;
        private final Map<Integer, Boolean> receivedChunks = new HashMap<>();

        private PendingRecipe(int totalChunks, PlayerStoredPrinterRecipe recipe) {
            this.totalChunks = totalChunks;
            this.recipe = recipe;
        }
    }
}
