package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Client-to-server chunk stream for importing a large recipe file. */
public final class PrinterRecipeImportChunkPacket {
    private static final Map<UUID, PendingImport> PENDING_IMPORTS = new HashMap<>();

    private final BlockPos printerPos;
    private final UUID transferId;
    private final int chunkIndex;
    private final int totalChunks;
    private final CompoundTag chunk;

    public PrinterRecipeImportChunkPacket(
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

    public static void sendRecipe(BlockPos printerPos, PlayerStoredPrinterRecipe recipe) {
        UUID transferId = UUID.randomUUID();
        int totalChunks = PrinterRecipeChunkData.totalChunks(recipe);

        for (int index = 0; index < totalChunks; index++) {
            ModPackets.INSTANCE.sendToServer(
                    new PrinterRecipeImportChunkPacket(
                            printerPos,
                            transferId,
                            index,
                            totalChunks,
                            PrinterRecipeChunkData.createChunk(recipe, index)
                    )
            );
        }
    }

    public static void encode(PrinterRecipeImportChunkPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.printerPos);
        buffer.writeUUID(packet.transferId);
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeVarInt(packet.totalChunks);
        buffer.writeNbt(packet.chunk);
    }

    public static PrinterRecipeImportChunkPacket decode(FriendlyByteBuf buffer) {
        return new PrinterRecipeImportChunkPacket(
                buffer.readBlockPos(),
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readNbt()
        );
    }

    public static void handle(
            PrinterRecipeImportChunkPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            PendingImport pending = PENDING_IMPORTS.get(player.getUUID());

            if (pending == null || !pending.transferId.equals(packet.transferId)) {
                if (packet.chunkIndex != 0 || !packet.chunk.getBoolean("Meta")) {
                    return;
                }

                pending = new PendingImport(
                        packet.printerPos,
                        packet.transferId,
                        packet.totalChunks,
                        PrinterRecipeChunkData.createRecipe(packet.chunk)
                );
                PENDING_IMPORTS.put(player.getUUID(), pending);
            }

            if (pending.receivedChunks.containsKey(packet.chunkIndex)) {
                return;
            }

            PrinterRecipeChunkData.appendChunk(pending.recipe, packet.chunk);
            pending.receivedChunks.put(packet.chunkIndex, Boolean.TRUE);

            if (pending.receivedChunks.size() < pending.totalChunks) {
                return;
            }

            PENDING_IMPORTS.remove(player.getUUID());
            finishImport(player, pending);
        });

        context.setPacketHandled(true);
    }

    private static void finishImport(ServerPlayer player, PendingImport pending) {
        PlayerPrinterRecipeLibrary library = PrinterRecipeCapability.getLibrary(player);
        if (library == null) {
            player.sendSystemMessage(Component.literal("§cНе удалось открыть библиотеку рецептов"));
            return;
        }

        if (library.isFull()) {
            player.sendSystemMessage(Component.literal("§cЛимит кастомных рецептов уже достигнут"));
            return;
        }

        if (!pending.recipe.isValid()) {
            player.sendSystemMessage(Component.literal("§cФайл рецепта повреждён или превышает лимит"));
            return;
        }

        PlayerStoredPrinterRecipe imported = pending.recipe.copy();
        imported.setRecipeId(library.createNextRecipeId());

        if (library.addRecipe(imported) != PlayerPrinterRecipeLibrary.AddResult.SUCCESS) {
            player.sendSystemMessage(Component.literal("§cНе удалось импортировать рецепт"));
            return;
        }

        if (player.level.getBlockEntity(pending.printerPos) instanceof Printer3DBlockEntity printer) {
            printer.setSelectedCustomRecipe(imported.getRecipeId(), player);
            ModPackets.sendToPlayer(new SyncSelectedPrinterRecipePacket(pending.printerPos, imported), player);
            PrinterRecipeSyncChunkPacket.sendRecipe(player, pending.printerPos, imported);
        }

        ModPackets.sendToPlayer(SyncPrinterRecipeLibraryPacket.fromLibrary(library), player);
        player.sendSystemMessage(Component.literal("§aРецепт импортирован: \""
                + imported.getDisplayName() + "\""));
    }

    private static final class PendingImport {
        private final BlockPos printerPos;
        private final UUID transferId;
        private final int totalChunks;
        private final PlayerStoredPrinterRecipe recipe;
        private final Map<Integer, Boolean> receivedChunks = new HashMap<>();

        private PendingImport(
                BlockPos printerPos,
                UUID transferId,
                int totalChunks,
                PlayerStoredPrinterRecipe recipe
        ) {
            this.printerPos = printerPos;
            this.transferId = transferId;
            this.totalChunks = totalChunks;
            this.recipe = recipe;
        }
    }
}
