package com.example.zitraksmode.network;

import com.example.zitraksmode.client.printer.ClientPrinterRecipeLibrary;
import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncPrinterRecipeLibraryPacket {

    private final List<ClientPrinterRecipeLibrary.RecipeSummary> recipes;

    public SyncPrinterRecipeLibraryPacket(List<ClientPrinterRecipeLibrary.RecipeSummary> recipes) {
        this.recipes = recipes;
    }

    public static SyncPrinterRecipeLibraryPacket fromLibrary(PlayerPrinterRecipeLibrary library) {
        List<ClientPrinterRecipeLibrary.RecipeSummary> summaries = new ArrayList<>();
        for (PlayerStoredPrinterRecipe recipe : library.getRecipes()) {
            summaries.add(new ClientPrinterRecipeLibrary.RecipeSummary(
                    recipe.getRecipeId(),
                    recipe.getDisplayName(),
                    recipe.getSizeX(),
                    recipe.getSizeY(),
                    recipe.getSizeZ(),
                    recipe.getStoredBlockCount()
            ));
        }
        return new SyncPrinterRecipeLibraryPacket(summaries);
    }

    public static void encode(SyncPrinterRecipeLibraryPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.recipes.size());
        for (ClientPrinterRecipeLibrary.RecipeSummary recipe : packet.recipes) {
            buf.writeUtf(recipe.recipeId());
            buf.writeUtf(recipe.displayName());
            buf.writeVarInt(recipe.sizeX());
            buf.writeVarInt(recipe.sizeY());
            buf.writeVarInt(recipe.sizeZ());
            buf.writeVarInt(recipe.blockCount());
        }
    }

    public static SyncPrinterRecipeLibraryPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ClientPrinterRecipeLibrary.RecipeSummary> recipes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            recipes.add(new ClientPrinterRecipeLibrary.RecipeSummary(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
        }
        return new SyncPrinterRecipeLibraryPacket(recipes);
    }

    public static void handle(SyncPrinterRecipeLibraryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncPrinterRecipeLibraryPacket packet) {
        ClientPrinterRecipeLibrary.setRecipes(packet.recipes);
    }
}