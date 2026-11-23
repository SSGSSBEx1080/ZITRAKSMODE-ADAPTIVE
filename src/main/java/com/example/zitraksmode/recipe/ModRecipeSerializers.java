package com.example.zitraksmode.recipe;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>>
            SERIALIZERS =
            DeferredRegister.create(
                    ForgeRegistries.RECIPE_SERIALIZERS,
                    ZitraksMode.MODID
            );

    public static final RegistryObject<
            RecipeSerializer<LabubuFusionRecipe>
            > LABUBU_FUSION =
            SERIALIZERS.register(
                    "labubu_fusion",
                    LabubuFusionRecipeSerializer::new
            );

    private ModRecipeSerializers() {
    }
}