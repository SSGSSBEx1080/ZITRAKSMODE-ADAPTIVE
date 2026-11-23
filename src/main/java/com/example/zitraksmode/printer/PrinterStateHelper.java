package com.example.zitraksmode.printer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PrinterStateHelper {

    public static BlockPlacement placement(BlockPos origin, int x, int y, int z, String stateString) {
        try {
            String sanitizedStateString = sanitizeStateString(stateString);
            BlockState state = BlockStateParser.parseForBlock(Registry.BLOCK, sanitizedStateString, true).blockState();
            Item requiredItem = resolveRequiredItem(sanitizedStateString, state);
            return new BlockPlacement(origin.offset(x, y, z), state, requiredItem);
        } catch (CommandSyntaxException e) {
            return new BlockPlacement(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), Items.AIR);
        }
    }

    /**
     * Санитайз blockstate строки.
     * ВАЖНО: НЕ трогаем open/powered/waterlogged/facing — они несут смысл (открытые люки,
     * повёрнутые воронки, водонаполненные лестницы).
     * Убираем только вредные для скана свойства.
     */
    public static String sanitizeStateString(String stateString) {
        if (stateString == null || stateString.isBlank()) {
            return "minecraft:air";
        }

        String sanitized = stateString;
        // Единственное что реально нужно чистить — occupied (кровать с игроком)
        sanitized = sanitized.replace("occupied=true", "occupied=false");
        return sanitized;
    }

    public static Item resolveRequiredItem(String stateString, BlockState state) {
        // ─── Специальные случаи для источников жидкости ──────────
        String blockIdRaw = stateString;
        int bracket = blockIdRaw.indexOf('[');
        String blockIdNoState = bracket >= 0 ? blockIdRaw.substring(0, bracket) : blockIdRaw;

        // Полный источник воды → ведро воды
        if (blockIdNoState.equals("minecraft:water") || blockIdNoState.equals("water")) {
            // Только уровень 0 = источник (level=0 или no level = default = source)
            if (!stateString.contains("level=") || stateString.contains("level=0")) {
                return Items.WATER_BUCKET;
            }
            // Течение воды — ничего не требуем (это следствие источника)
            return Items.AIR;
        }
        if (blockIdNoState.equals("minecraft:lava") || blockIdNoState.equals("lava")) {
            if (!stateString.contains("level=") || stateString.contains("level=0")) {
                return Items.LAVA_BUCKET;
            }
            return Items.AIR;
        }

        Item direct = state.getBlock().asItem();
        if (direct != Items.AIR) {
            return direct;
        }

        String blockId = blockIdNoState;
        if (blockId.startsWith("minecraft:")) {
            blockId = blockId.substring("minecraft:".length());
        }

        if (blockId.startsWith("potted_")) {
            String inner = blockId.substring("potted_".length());
            Item innerItem = itemByName(inner);
            return innerItem != Items.AIR ? innerItem : Items.FLOWER_POT;
        }

        if (blockId.endsWith("_wall_sign")) {
            return itemByName(blockId.replace("_wall_sign", "_sign"));
        }

        if (blockId.endsWith("_wall_banner")) {
            return itemByName(blockId.replace("_wall_banner", "_banner"));
        }

        if (blockId.endsWith("_wall_skull")) {
            return itemByName(blockId.replace("_wall_skull", "_skull"));
        }

        if (blockId.endsWith("_wall_head")) {
            return itemByName(blockId.replace("_wall_head", "_head"));
        }

        if (blockId.equals("water_cauldron") || blockId.equals("lava_cauldron") || blockId.equals("powder_snow_cauldron")) {
            return Items.CAULDRON;
        }

        if (blockId.equals("tripwire")) {
            return Items.STRING;
        }

        if (blockId.equals("redstone_wire")) {
            return Items.REDSTONE;
        }

        return Items.AIR;
    }

    private static Item itemByName(String path) {
        Item item = Registry.ITEM.get(new ResourceLocation("minecraft", path));
        return item == null ? Items.AIR : item;
    }
}