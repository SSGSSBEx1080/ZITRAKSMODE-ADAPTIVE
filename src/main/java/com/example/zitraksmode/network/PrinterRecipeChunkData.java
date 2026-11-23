package com.example.zitraksmode.network;

import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Map;

/** Shared compact chunk codec for large custom printer recipes. */
final class PrinterRecipeChunkData {
    static final int ENTRIES_PER_CHUNK = 512;

    private PrinterRecipeChunkData() {
    }

    static int totalChunks(PlayerStoredPrinterRecipe recipe) {
        int placementChunks = chunksFor(recipe.getPlacements().size());
        int paletteChunks = chunksFor(recipe.getPalette().size());
        int materialChunks = chunksFor(recipe.getMaterials().size());
        return Math.max(1, Math.max(placementChunks, Math.max(paletteChunks, materialChunks)));
    }

    static CompoundTag createChunk(PlayerStoredPrinterRecipe recipe, int chunkIndex) {
        CompoundTag chunk = new CompoundTag();

        if (chunkIndex == 0) {
            chunk.putBoolean("Meta", true);
            chunk.putString("recipeId", recipe.getRecipeId());
            chunk.putString("displayName", recipe.getDisplayName());
            chunk.putInt("sizeX", recipe.getSizeX());
            chunk.putInt("sizeY", recipe.getSizeY());
            chunk.putInt("sizeZ", recipe.getSizeZ());
            chunk.putLong("createdAt", recipe.getCreatedAt());
        }

        int from = chunkIndex * ENTRIES_PER_CHUNK;
        int paletteTo = Math.min(recipe.getPalette().size(), from + ENTRIES_PER_CHUNK);
        int placementTo = Math.min(recipe.getPlacements().size(), from + ENTRIES_PER_CHUNK);

        ListTag paletteTag = new ListTag();
        for (int index = from; index < paletteTo; index++) {
            paletteTag.add(StringTag.valueOf(recipe.getPalette().get(index)));
        }
        chunk.put("palette", paletteTag);

        ListTag materialsTag = new ListTag();
        int materialIndex = 0;
        for (Map.Entry<String, Integer> entry : recipe.getMaterials().entrySet()) {
            if (materialIndex >= from && materialIndex < from + ENTRIES_PER_CHUNK) {
                CompoundTag material = new CompoundTag();
                material.putString("item", entry.getKey());
                material.putInt("count", entry.getValue());
                materialsTag.add(material);
            }
            materialIndex++;
        }
        chunk.put("materials", materialsTag);

        ListTag placementsTag = new ListTag();
        for (int index = from; index < placementTo; index++) {
            placementsTag.add(recipe.getPlacements().get(index).save());
        }
        chunk.put("placements", placementsTag);

        return chunk;
    }

    static PlayerStoredPrinterRecipe createRecipe(CompoundTag metadata) {
        return new PlayerStoredPrinterRecipe(
                metadata.getString("recipeId"),
                metadata.getString("displayName"),
                metadata.getInt("sizeX"),
                metadata.getInt("sizeY"),
                metadata.getInt("sizeZ"),
                metadata.getLong("createdAt")
        );
    }

    static void appendChunk(PlayerStoredPrinterRecipe recipe, CompoundTag chunk) {
        ListTag paletteTag = chunk.getList("palette", Tag.TAG_STRING);
        for (int index = 0; index < paletteTag.size(); index++) {
            recipe.addPaletteState(paletteTag.getString(index));
        }

        ListTag materialsTag = chunk.getList("materials", Tag.TAG_COMPOUND);
        for (int index = 0; index < materialsTag.size(); index++) {
            CompoundTag material = materialsTag.getCompound(index);
            recipe.putMaterial(material.getString("item"), material.getInt("count"));
        }

        ListTag placementsTag = chunk.getList("placements", Tag.TAG_COMPOUND);
        for (int index = 0; index < placementsTag.size(); index++) {
            PlayerStoredPrinterRecipe.Placement placement =
                    PlayerStoredPrinterRecipe.Placement.load(placementsTag.getCompound(index));

            recipe.addPlacement(
                    placement.getX(),
                    placement.getY(),
                    placement.getZ(),
                    placement.getPaletteIndex()
            );
        }
    }

    private static int chunksFor(int amount) {
        return (amount + ENTRIES_PER_CHUNK - 1) / ENTRIES_PER_CHUNK;
    }
}
