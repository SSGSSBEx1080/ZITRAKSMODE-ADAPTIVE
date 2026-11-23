package com.example.zitraksmode.recipe;

import com.example.zitraksmode.items.LabubuFigurineItem;
import com.example.zitraksmode.items.LabubuLootTable;
import com.example.zitraksmode.items.LabubuRarity;
import com.example.zitraksmode.items.LabubuType;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class LabubuFusionRecipe extends CustomRecipe {

    private static final int REQUIRED_FIGURINES = 9;

    /*
     * Запоминаем, сколько ячеек было занято до крафта.
     * Это нужно, потому что Minecraft сам убирает по одному
     * предмету из каждой занятой ячейки.
     */
    private static final Map<CraftingContainer, Integer>
            OCCUPIED_SLOTS_BEFORE_CRAFT =
            new WeakHashMap<>();

    public LabubuFusionRecipe(
            ResourceLocation id
    ) {
        super(id);
    }

    @Override
    public boolean matches(
            CraftingContainer container,
            Level level
    ) {
        int totalCount = 0;
        int occupiedSlots = 0;

        LabubuRarity sourceRarity =
                null;

        for (int i = 0;
             i < container.getContainerSize();
             i++) {

            ItemStack stack =
                    container.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem()
                    instanceof LabubuFigurineItem figurine)) {
                return false;
            }

            occupiedSlots++;
            totalCount += stack.getCount();

            if (sourceRarity == null) {
                sourceRarity =
                        figurine.type.rarity;
            } else if (figurine.type.rarity
                    != sourceRarity) {
                /*
                 * Нельзя смешивать COMMON и RARE,
                 * например.
                 */
                return false;
            }
        }

        if (sourceRarity == null
                || totalCount < REQUIRED_FIGURINES) {
            return false;
        }

        if (getNextRarity(sourceRarity) == null) {
            return false;
        }

        /*
         * Запоминаем данные именно подходящей сетки.
         */
        OCCUPIED_SLOTS_BEFORE_CRAFT.put(
                container,
                occupiedSlots
        );

        return true;
    }

    public static int getOccupiedSlotsBeforeCraft(
            CraftingContainer container
    ) {
        return OCCUPIED_SLOTS_BEFORE_CRAFT.getOrDefault(
                container,
                1
        );
    }

    @Override
    public ItemStack assemble(
            CraftingContainer container
    ) {
        List<LabubuType> figurines =
                getFigurineTypes(container);

        if (figurines.isEmpty()) {
            return ItemStack.EMPTY;
        }

        LabubuRarity sourceRarity =
                figurines.get(0).rarity;

        LabubuRarity targetRarity =
                getNextRarity(sourceRarity);

        if (targetRarity == null) {
            return ItemStack.EMPTY;
        }

        LabubuType resultType =
                getRandomTypeOfRarity(
                        targetRarity
                );

        return LabubuLootTable.getFigurineStack(
                resultType
        );
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width >= 1
                && height >= 1;
    }

    @Override
    public ItemStack getResultItem() {
        return LabubuLootTable.getFigurineStack(
                LabubuType.PURPLE
        );
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(
            CraftingContainer container
    ) {
        return NonNullList.withSize(
                container.getContainerSize(),
                ItemStack.EMPTY
        );
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.LABUBU_FUSION.get();
    }

    private static List<LabubuType> getFigurineTypes(
            CraftingContainer container
    ) {
        List<LabubuType> result =
                new ArrayList<>();

        for (int i = 0;
             i < container.getContainerSize();
             i++) {

            ItemStack stack =
                    container.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem()
                    instanceof LabubuFigurineItem figurine) {

                result.add(
                        figurine.type
                );
            }
        }

        return result;
    }

    private static LabubuRarity getNextRarity(
            LabubuRarity rarity
    ) {
        return switch (rarity) {
            case COMMON ->
                    LabubuRarity.UNCOMMON;

            case UNCOMMON ->
                    LabubuRarity.RARE;

            case RARE ->
                    LabubuRarity.EPIC;

            case EPIC ->
                    LabubuRarity.LEGENDARY;

            case LEGENDARY ->
                    null;
        };
    }

    private static LabubuType getRandomTypeOfRarity(
            LabubuRarity rarity
    ) {
        List<LabubuType> candidates =
                new ArrayList<>();

        for (LabubuType type : LabubuType.values()) {
            if (type.rarity == rarity) {
                candidates.add(type);
            }
        }

        if (candidates.isEmpty()) {
            return LabubuType.PURPLE;
        }

        RandomSource random =
                RandomSource.create();

        return candidates.get(
                random.nextInt(
                        candidates.size()
                )
        );
    }
}