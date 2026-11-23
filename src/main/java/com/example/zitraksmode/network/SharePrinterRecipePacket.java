package com.example.zitraksmode.network;

import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SharePrinterRecipePacket {

    private final String recipeId;
    private final java.util.UUID targetPlayerId;

    public SharePrinterRecipePacket(String recipeId, java.util.UUID targetPlayerId) {
        this.recipeId = recipeId;
        this.targetPlayerId = targetPlayerId;
    }

    public static void encode(SharePrinterRecipePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.recipeId);
        buf.writeUUID(packet.targetPlayerId);
    }

    public static SharePrinterRecipePacket decode(FriendlyByteBuf buf) {
        return new SharePrinterRecipePacket(buf.readUtf(), buf.readUUID());
    }

    public static void handle(SharePrinterRecipePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }

            PlayerPrinterRecipeLibrary senderLibrary = PrinterRecipeCapability.getLibrary(sender);
            if (senderLibrary == null) {
                sender.sendSystemMessage(Component.literal("§cНе удалось открыть твою библиотеку рецептов"));
                return;
            }

            PlayerStoredPrinterRecipe sourceRecipe = senderLibrary.getRecipe(packet.recipeId).orElse(null);
            if (sourceRecipe == null) {
                sender.sendSystemMessage(Component.literal("§cРецепт не найден"));
                return;
            }

            ServerPlayer target = sender.getServer().getPlayerList().getPlayer(packet.targetPlayerId);
            if (target == null) {
                sender.sendSystemMessage(Component.literal("§cИгрок не найден или вышел"));
                return;
            }

            PlayerPrinterRecipeLibrary targetLibrary = PrinterRecipeCapability.getLibrary(target);
            if (targetLibrary == null) {
                sender.sendSystemMessage(Component.literal("§cНе удалось открыть библиотеку игрока"));
                return;
            }

            if (targetLibrary.isFull()) {
                sender.sendSystemMessage(Component.literal("§cУ игрока уже максимум рецептов"));
                if (!target.getUUID().equals(sender.getUUID())) {
                    target.sendSystemMessage(Component.literal("§cТебе пытались отправить рецепт \"" + sourceRecipe.getDisplayName() + "\", но у тебя заполнен лимит рецептов"));
                }
                return;
            }

            PlayerStoredPrinterRecipe sharedCopy = sourceRecipe.copy();
            sharedCopy.setRecipeId(targetLibrary.createNextRecipeId());

            PlayerPrinterRecipeLibrary.AddResult addResult = targetLibrary.addRecipe(sharedCopy);
            if (addResult != PlayerPrinterRecipeLibrary.AddResult.SUCCESS) {
                sender.sendSystemMessage(Component.literal("§cНе удалось передать рецепт"));
                return;
            }

            ModPackets.sendToPlayer(SyncPrinterRecipeLibraryPacket.fromLibrary(targetLibrary), target);

            sender.sendSystemMessage(Component.literal("§aТы поделился рецептом \"" + sourceRecipe.getDisplayName() + "\" с игроком " + target.getGameProfile().getName()));
            target.sendSystemMessage(Component.literal("§bИгрок " + sender.getGameProfile().getName() + " поделился с вами рецептом \"" + sourceRecipe.getDisplayName() + "\"!"));
        });
        ctx.setPacketHandled(true);
    }
}
