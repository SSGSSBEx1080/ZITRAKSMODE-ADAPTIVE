package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Small selection-state packet. The full recipe follows in safe chunk packets,
 * never in one gigantic writeNbt call.
 */
public final class SyncSelectedPrinterRecipePacket {
    private final BlockPos printerPos;
    private final boolean clear;
    private final String recipeId;

    public SyncSelectedPrinterRecipePacket(BlockPos printerPos) {
        this.printerPos = printerPos;
        this.clear = true;
        this.recipeId = "";
    }

    public SyncSelectedPrinterRecipePacket(BlockPos printerPos, PlayerStoredPrinterRecipe recipe) {
        this.printerPos = printerPos;
        this.clear = recipe == null;
        this.recipeId = recipe == null ? "" : recipe.getRecipeId();
    }

    public static void encode(SyncSelectedPrinterRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.printerPos);
        buffer.writeBoolean(packet.clear);
        if (!packet.clear) {
            buffer.writeUtf(packet.recipeId, 128);
        }
    }

    public static SyncSelectedPrinterRecipePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        boolean clear = buffer.readBoolean();

        if (clear) {
            return new SyncSelectedPrinterRecipePacket(pos);
        }

        return new SyncSelectedPrinterRecipePacket(
                pos,
                new PlayerStoredPrinterRecipe(
                        buffer.readUtf(128),
                        "pending",
                        1,
                        1,
                        1,
                        0L
                )
        );
    }

    public static void handle(
            SyncSelectedPrinterRecipePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncSelectedPrinterRecipePacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        if (!(Minecraft.getInstance().level.getBlockEntity(packet.printerPos)
                instanceof Printer3DBlockEntity printer)) {
            return;
        }

        if (packet.clear) {
            printer.clearSelectedCustomRecipeClientSide();
            return;
        }

        printer.setSelectedCustomRecipe(packet.recipeId);
        printer.setClientPreviewCustomRecipe(null);
    }
}
