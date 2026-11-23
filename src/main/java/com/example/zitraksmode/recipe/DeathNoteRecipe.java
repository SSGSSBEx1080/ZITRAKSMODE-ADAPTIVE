package com.example.zitraksmode.recipe;

import com.example.zitraksmode.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class DeathNoteRecipe extends ShapedRecipe {

    public static final RecipeSerializer<DeathNoteRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public DeathNoteRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe base = (ShapedRecipe) RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            return new DeathNoteRecipe(base.getId(), base.getGroup(),
                    base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem());
        }

        @Override
        public DeathNoteRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe base = (ShapedRecipe) RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return new DeathNoteRecipe(base.getId(), base.getGroup(),
                    base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, DeathNoteRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    };

    public DeathNoteRecipe(ResourceLocation id, String group, int width, int height,
                           NonNullList<Ingredient> inputs, ItemStack output) {
        super(id, group, width, height, inputs, output);
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        Item blackBlock = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.WRITABLE_BOOK)) {
                // книга найдена
            } else if (blackBlock == null && isBlackBlock(stack)) {
                blackBlock = stack.getItem();
            }
        }
        if (blackBlock == null) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.DEATH_NOTE.get());
        int maxUses = getMaxUsesForBlock(blackBlock);
        result.getOrCreateTag().putInt("maxUses", maxUses);
        return result;
    }

    private int getMaxUsesForBlock(Item block) {
        if (block == Items.OBSIDIAN) return 10;
        if (block == Items.NETHERITE_BLOCK) return 10;
        if (block == Items.CRYING_OBSIDIAN) return 7;
        if (block == Items.BLACKSTONE) return 4;
        if (block == Items.BLACK_CONCRETE) return 3;
        if (block == Items.BLACK_WOOL) return 2;
        if (block == Items.BLACK_CARPET) return 1;
        if (block == Items.COAL_BLOCK) return 2;
        if (block == Items.BLACK_TERRACOTTA) return 4;
        if (block == Items.BLACK_GLAZED_TERRACOTTA) return 4;
        if (block == Items.BLACK_SHULKER_BOX) return 5;
        if (block == Items.BLACK_CONCRETE_POWDER) return 2;
        return 2;
    }

    private boolean isBlackBlock(ItemStack stack) {
        return stack.is(Items.OBSIDIAN) || stack.is(Items.NETHERITE_BLOCK) || stack.is(Items.CRYING_OBSIDIAN) ||
               stack.is(Items.BLACKSTONE) || stack.is(Items.BLACK_CONCRETE) || stack.is(Items.BLACK_WOOL) ||
               stack.is(Items.BLACK_CARPET) || stack.is(Items.COAL_BLOCK) || stack.is(Items.BLACK_TERRACOTTA) ||
               stack.is(Items.BLACK_GLAZED_TERRACOTTA) || stack.is(Items.BLACK_SHULKER_BOX) ||
               stack.is(Items.BLACK_CONCRETE_POWDER) || stack.is(Items.BLACK_CANDLE) || stack.is(Items.BLACK_BANNER) ||
               stack.is(Items.BLACK_BED);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}