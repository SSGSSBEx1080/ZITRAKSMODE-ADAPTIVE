package com.example.zitraksmode.client.printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ClientPrinterRecipeLibrary {

    private static final List<RecipeSummary> RECIPES = new ArrayList<>();

    private ClientPrinterRecipeLibrary() {
    }

    public static void setRecipes(List<RecipeSummary> recipes) {
        RECIPES.clear();
        RECIPES.addAll(recipes);
    }

    public static List<RecipeSummary> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Optional<RecipeSummary> getById(String recipeId) {
        return RECIPES.stream().filter(recipe -> recipe.recipeId().equals(recipeId)).findFirst();
    }

    public record RecipeSummary(String recipeId, String displayName, int sizeX, int sizeY, int sizeZ, int blockCount) {
    }
}