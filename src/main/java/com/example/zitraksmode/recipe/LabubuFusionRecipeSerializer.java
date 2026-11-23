package com.example.zitraksmode.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class LabubuFusionRecipeSerializer
        implements RecipeSerializer<LabubuFusionRecipe> {

    @Override
    public LabubuFusionRecipe fromJson(
            ResourceLocation recipeId,
            JsonObject json
    ) {
        /*
         * Рецепт кастомный, поэтому JSON может быть пустым
         * кроме поля type.
         */
        return new LabubuFusionRecipe(
                recipeId
        );
    }

    @Override
    public LabubuFusionRecipe fromNetwork(
            ResourceLocation recipeId,
            FriendlyByteBuf buffer
    ) {
        return new LabubuFusionRecipe(
                recipeId
        );
    }

    @Override
    public void toNetwork(
            FriendlyByteBuf buffer,
            LabubuFusionRecipe recipe
    ) {
        /*
         * Данных для передачи нет.
         */
    }
}