package com.example.zitraksmode.printer.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class PlayerPrinterRecipeLibrary {

    private final List<PlayerStoredPrinterRecipe> recipes = new ArrayList<>();
    private int nextRecipeNumber = 1;

    public List<PlayerStoredPrinterRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public int getRecipeCount() {
        return recipes.size();
    }

    public boolean isFull() {
        return recipes.size() >= PrinterRecipeLimits.MAX_RECIPES_PER_PLAYER;
    }

    public Optional<PlayerStoredPrinterRecipe> getRecipe(String recipeId) {
        return recipes.stream()
                .filter(recipe -> recipe.getRecipeId().equals(recipeId))
                .findFirst();
    }

    public String createNextRecipeId() {
        String id;
        do {
            id = "player_scan_" + nextRecipeNumber++;
        } while (containsRecipeId(id));
        return id;
    }

    public AddResult addRecipe(PlayerStoredPrinterRecipe recipe) {
        if (recipe == null) {
            return AddResult.INVALID_RECIPE;
        }

        if (!recipe.isValid()) {
            return AddResult.INVALID_RECIPE;
        }

        if (isFull()) {
            return AddResult.LIMIT_REACHED;
        }

        if (containsRecipeId(recipe.getRecipeId())) {
            return AddResult.DUPLICATE_ID;
        }

        recipes.add(recipe.copy());
        sortRecipes();
        return AddResult.SUCCESS;
    }

    public RenameResult renameRecipe(String recipeId, String newName) {
        Optional<PlayerStoredPrinterRecipe> optional = getRecipe(recipeId);
        if (optional.isEmpty()) {
            return RenameResult.NOT_FOUND;
        }

        String normalized = PrinterRecipeLimits.normalizeName(newName);
        if (normalized.isBlank()) {
            return RenameResult.EMPTY_NAME;
        }

        optional.get().setDisplayName(normalized);
        sortRecipes();
        return RenameResult.SUCCESS;
    }

    public boolean removeRecipe(String recipeId) {
        return recipes.removeIf(recipe -> recipe.getRecipeId().equals(recipeId));
    }

    public void clear() {
        recipes.clear();
        nextRecipeNumber = 1;
    }

    public void copyFrom(PlayerPrinterRecipeLibrary other) {
        clear();
        this.nextRecipeNumber = other.nextRecipeNumber;
        for (PlayerStoredPrinterRecipe recipe : other.recipes) {
            this.recipes.add(recipe.copy());
        }
        sortRecipes();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("nextRecipeNumber", nextRecipeNumber);

        ListTag listTag = new ListTag();
        for (PlayerStoredPrinterRecipe recipe : recipes) {
            listTag.add(recipe.save());
        }
        tag.put("recipes", listTag);
        return tag;
    }

    public void load(CompoundTag tag) {
        clear();
        nextRecipeNumber = Math.max(1, tag.getInt("nextRecipeNumber"));

        ListTag listTag = tag.getList("recipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            PlayerStoredPrinterRecipe recipe = PlayerStoredPrinterRecipe.load(listTag.getCompound(i));
            if (recipe.isValid() && !containsRecipeId(recipe.getRecipeId()) && recipes.size() < PrinterRecipeLimits.MAX_RECIPES_PER_PLAYER) {
                recipes.add(recipe);
            }
        }

        sortRecipes();
    }

    private boolean containsRecipeId(String recipeId) {
        return recipes.stream().anyMatch(recipe -> Objects.equals(recipe.getRecipeId(), recipeId));
    }

    private void sortRecipes() {
        recipes.sort(Comparator.comparing(recipe -> recipe.getDisplayName().toLowerCase(Locale.ROOT)));
    }

    public enum AddResult {
        SUCCESS,
        LIMIT_REACHED,
        DUPLICATE_ID,
        INVALID_RECIPE
    }

    public enum RenameResult {
        SUCCESS,
        NOT_FOUND,
        EMPTY_NAME
    }
}