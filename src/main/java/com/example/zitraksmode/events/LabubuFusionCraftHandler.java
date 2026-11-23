package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LabubuFigurineItem;
import com.example.zitraksmode.items.LabubuRarity;
import com.example.zitraksmode.recipe.LabubuFusionRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class LabubuFusionCraftHandler {

    private static final int REQUIRED_FIGURINES = 9;

    private LabubuFusionCraftHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(
            PlayerEvent.ItemCraftedEvent event
    ) {
        if (event.getEntity().level.isClientSide) {
            return;
        }

        ItemStack result =
                event.getCrafting();

        if (!(result.getItem()
                instanceof LabubuFigurineItem resultFigurine)) {
            return;
        }

        LabubuRarity sourceRarity =
                getPreviousRarity(
                        resultFigurine.type.rarity
                );

        if (sourceRarity == null) {
            return;
        }

        Container inventory =
                event.getInventory();

        if (!(inventory instanceof net.minecraft.world.inventory.CraftingContainer craftingContainer)) {
            return;
        }

        int occupiedBeforeCraft =
                LabubuFusionRecipe
                        .getOccupiedSlotsBeforeCraft(
                                craftingContainer
                        );

        /*
         * Minecraft уже убрал по одному предмету
         * из каждой занятой ячейки.
         *
         * Например:
         *
         * 9 фигурок в одной ячейке:
         * Minecraft убрал 1,
         * дополнительно убираем 8.
         *
         * 5 + 4 в двух ячейках:
         * Minecraft убрал 2,
         * дополнительно убираем 7.
         */
        int extraToRemove =
                Math.max(
                        0,
                        REQUIRED_FIGURINES
                                - occupiedBeforeCraft
                );

        for (int i = 0;
             i < craftingContainer.getContainerSize()
                     && extraToRemove > 0;
             i++) {

            ItemStack stack =
                    craftingContainer.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem()
                    instanceof LabubuFigurineItem figurine)) {
                continue;
            }

            if (figurine.type.rarity
                    != sourceRarity) {
                continue;
            }

            int remove =
                    Math.min(
                            extraToRemove,
                            stack.getCount()
                    );

            stack.shrink(remove);
            extraToRemove -= remove;

            craftingContainer.setItem(
                    i,
                    stack
            );
        }

        craftingContainer.setChanged();
    }

    private static LabubuRarity getPreviousRarity(
            LabubuRarity targetRarity
    ) {
        return switch (targetRarity) {
            case UNCOMMON ->
                    LabubuRarity.COMMON;

            case RARE ->
                    LabubuRarity.UNCOMMON;

            case EPIC ->
                    LabubuRarity.RARE;

            case LEGENDARY ->
                    LabubuRarity.EPIC;

            case COMMON ->
                    null;
        };
    }
}