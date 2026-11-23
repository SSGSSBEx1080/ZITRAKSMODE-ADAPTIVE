package com.example.zitraksmode.printer.custom;

import com.example.zitraksmode.printer.BlockPlacement;
import com.example.zitraksmode.printer.PrinterStateHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerStoredPrinterRecipeRuntime {

    private PlayerStoredPrinterRecipeRuntime() {
    }

    public static List<BlockPlacement> toPlacements(BlockPos startPos, PlayerStoredPrinterRecipe recipe) {
        List<BlockPlacement> result = new ArrayList<>();
        List<String> palette = recipe.getPalette();

        for (PlayerStoredPrinterRecipe.Placement placement : recipe.getPlacements()) {
            if (placement.getPaletteIndex() < 0 || placement.getPaletteIndex() >= palette.size()) {
                continue;
            }

            String stateString = palette.get(placement.getPaletteIndex());
            result.add(PrinterStateHelper.placement(
                    startPos,
                    placement.getX(),
                    placement.getY(),
                    placement.getZ(),
                    stateString
            ));
        }

        return result;
    }

    public static Map<Item, Integer> toMaterialMap(PlayerStoredPrinterRecipe recipe) {
        Map<Item, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : recipe.getMaterials().entrySet()) {
            Item item = Registry.ITEM.get(new ResourceLocation(entry.getKey()));
            if (item != null && item != Items.AIR && entry.getValue() > 0) {
                result.put(item, entry.getValue());
            }
        }

        return result;
    }
}