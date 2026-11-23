package com.example.zitraksmode.recipe;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.util.DeathNoteHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.network.FriendlyByteBuf;
import com.google.gson.JsonObject;

public class DeathNoteRequiemRecipe extends ShapedRecipe {

    public static final RecipeSerializer<DeathNoteRequiemRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public DeathNoteRequiemRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe base = (ShapedRecipe) RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            return new DeathNoteRequiemRecipe(base.getId(), base.getGroup(),
                    base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem());
        }

        @Override
        public DeathNoteRequiemRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe base = (ShapedRecipe) RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return new DeathNoteRequiemRecipe(base.getId(), base.getGroup(),
                    base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DeathNoteRequiemRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    };

    public DeathNoteRequiemRecipe(ResourceLocation id, String group, int width, int height,
                                   NonNullList<Ingredient> ingredients, ItemStack result) {
        super(id, group, width, height, ingredients, result);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        // Проверяем форму
        if (!super.matches(inv, level)) return false;

        // Ищем обычную тетрадь с числом убийств >=5
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == ModItems.DEATH_NOTE.get()
                    && stack.getOrCreateTag().getInt(DeathNoteHelper.TAG_KILLS) >= 5) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        return getResultItem().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}