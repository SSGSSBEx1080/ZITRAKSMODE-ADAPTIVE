package com.example.zitraksmode.printer.custom;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlayerStoredPrinterRecipe {

    private String recipeId;
    private String displayName;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private long createdAt;

    private final List<String> palette = new ArrayList<>();
    private final List<Placement> placements = new ArrayList<>();
    private final Map<String, Integer> materials = new LinkedHashMap<>();

    public PlayerStoredPrinterRecipe(String recipeId, String displayName, int sizeX, int sizeY, int sizeZ, long createdAt) {
        this.recipeId = recipeId;
        this.displayName = PrinterRecipeLimits.normalizeName(displayName);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.createdAt = createdAt;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = PrinterRecipeLimits.normalizeName(displayName);
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public List<String> getPalette() {
        return Collections.unmodifiableList(palette);
    }

    public List<Placement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }

    public Map<String, Integer> getMaterials() {
        return Collections.unmodifiableMap(materials);
    }

    public int getStoredBlockCount() {
        return placements.size();
    }

    public long getScanVolume() {
        return PrinterRecipeLimits.volume(sizeX, sizeY, sizeZ);
    }

    public boolean isValid() {
        if (recipeId == null || recipeId.isBlank()) {
            return false;
        }
        if (displayName == null || displayName.isBlank()) {
            return false;
        }
        if (!PrinterRecipeLimits.isVolumeAllowed(sizeX, sizeY, sizeZ)) {
            return false;
        }
        if (!PrinterRecipeLimits.isStoredBlockCountAllowed(placements.size())) {
            return false;
        }
        if (palette.isEmpty() && !placements.isEmpty()) {
            return false;
        }
        for (Placement placement : placements) {
            if (placement.paletteIndex < 0 || placement.paletteIndex >= palette.size()) {
                return false;
            }
        }
        return true;
    }

    public void addPaletteState(String stateString) {
        if (stateString == null || stateString.isBlank()) {
            return;
        }
        palette.add(stateString);
    }

    public void addPlacement(int x, int y, int z, int paletteIndex) {
        placements.add(new Placement(x, y, z, paletteIndex));
    }

    public void putMaterial(String itemId, int count) {
        if (itemId == null || itemId.isBlank() || count <= 0) {
            return;
        }
        materials.put(itemId, count);
    }

    public void putMaterial(Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) {
            return;
        }
        ResourceLocation key = Registry.ITEM.getKey(item);
        if (key != null) {
            putMaterial(key.toString(), count);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("recipeId", recipeId);
        tag.putString("displayName", displayName);
        tag.putInt("sizeX", sizeX);
        tag.putInt("sizeY", sizeY);
        tag.putInt("sizeZ", sizeZ);
        tag.putLong("createdAt", createdAt);

        ListTag paletteTag = new ListTag();
        for (String stateString : palette) {
            paletteTag.add(StringTag.valueOf(stateString));
        }
        tag.put("palette", paletteTag);

        ListTag placementsTag = new ListTag();
        for (Placement placement : placements) {
            placementsTag.add(placement.save());
        }
        tag.put("placements", placementsTag);

        ListTag materialsTag = new ListTag();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            CompoundTag materialTag = new CompoundTag();
            materialTag.putString("item", entry.getKey());
            materialTag.putInt("count", entry.getValue());
            materialsTag.add(materialTag);
        }
        tag.put("materials", materialsTag);

        return tag;
    }

    public static PlayerStoredPrinterRecipe load(CompoundTag tag) {
        PlayerStoredPrinterRecipe recipe = new PlayerStoredPrinterRecipe(
                tag.getString("recipeId"),
                tag.getString("displayName"),
                tag.getInt("sizeX"),
                tag.getInt("sizeY"),
                tag.getInt("sizeZ"),
                tag.getLong("createdAt")
        );

        ListTag paletteTag = tag.getList("palette", Tag.TAG_STRING);
        for (int i = 0; i < paletteTag.size(); i++) {
            recipe.palette.add(paletteTag.getString(i));
        }

        ListTag placementsTag = tag.getList("placements", Tag.TAG_COMPOUND);
        for (int i = 0; i < placementsTag.size(); i++) {
            recipe.placements.add(Placement.load(placementsTag.getCompound(i)));
        }

        ListTag materialsTag = tag.getList("materials", Tag.TAG_COMPOUND);
        for (int i = 0; i < materialsTag.size(); i++) {
            CompoundTag materialTag = materialsTag.getCompound(i);
            String itemId = materialTag.getString("item");
            int count = materialTag.getInt("count");
            if (!itemId.isBlank() && count > 0) {
                recipe.materials.put(itemId, count);
            }
        }

        return recipe;
    }

    public PlayerStoredPrinterRecipe copy() {
        PlayerStoredPrinterRecipe copy = new PlayerStoredPrinterRecipe(recipeId, displayName, sizeX, sizeY, sizeZ, createdAt);
        copy.palette.addAll(this.palette);
        for (Placement placement : this.placements) {
            copy.placements.add(placement.copy());
        }
        copy.materials.putAll(this.materials);
        return copy;
    }

    public static class Placement {
        private final int x;
        private final int y;
        private final int z;
        private final int paletteIndex;

        public Placement(int x, int y, int z, int paletteIndex) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.paletteIndex = paletteIndex;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getPaletteIndex() {
            return paletteIndex;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            tag.putInt("paletteIndex", paletteIndex);
            return tag;
        }

        public static Placement load(CompoundTag tag) {
            return new Placement(
                    tag.getInt("x"),
                    tag.getInt("y"),
                    tag.getInt("z"),
                    tag.getInt("paletteIndex")
            );
        }

        public Placement copy() {
            return new Placement(x, y, z, paletteIndex);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Placement placement)) return false;
            return x == placement.x && y == placement.y && z == placement.z && paletteIndex == placement.paletteIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, paletteIndex);
        }
    }
}
