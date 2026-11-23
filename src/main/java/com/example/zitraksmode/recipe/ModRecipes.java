package com.example.zitraksmode.recipe;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ZitraksMode.MODID);

    public static final RegistryObject<RecipeSerializer<DeathNoteRecipe>> DEATH_NOTE =
            SERIALIZERS.register("death_note_craft", () -> DeathNoteRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<DeathNoteRequiemRecipe>> DEATH_NOTE_REQUIEM =
            SERIALIZERS.register("death_note_requiem_craft", () -> DeathNoteRequiemRecipe.SERIALIZER);
}