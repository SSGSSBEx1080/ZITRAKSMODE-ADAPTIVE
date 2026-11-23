package com.example.zitraksmode.printer.custom;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.blocks.Printer3DBlock;
import com.example.zitraksmode.printer.PrinterStateHelper;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PrinterRecipeScanner {

    private PrinterRecipeScanner() {
    }

    public static ScanResult scanFromPrinter(Printer3DBlockEntity printer, String displayName, int sizeX, int sizeY, int sizeZ, String recipeId) {
        if (printer == null || printer.getLevel() == null) {
            return ScanResult.error("Принтер не найден");
        }

        String normalizedName = PrinterRecipeLimits.normalizeName(displayName);
        if (normalizedName.isBlank()) {
            return ScanResult.error("Название рецепта пустое");
        }

        if (!PrinterRecipeLimits.isDimensionInputValid(sizeX, sizeY, sizeZ)) {
            return ScanResult.error("Размеры должны быть больше нуля");
        }

        if (!PrinterRecipeLimits.isVolumeAllowed(sizeX, sizeY, sizeZ)) {
            return ScanResult.error("Слишком большой объём скана: максимум " + PrinterRecipeLimits.MAX_SCAN_VOLUME + " клеток");
        }

        Level level = printer.getLevel();
        BlockPos printerPos = printer.getBlockPos();
        Rotation forwardRotation = getPrinterRotation(printer);
        // Обратная ротация — нужна чтобы "нормализовать" состояние блока
        // из мировой ориентации в локальную (как будто принтер смотрит NORTH)
        Rotation inverseRotation = inverseOf(forwardRotation);

        PlayerStoredPrinterRecipe recipe = new PlayerStoredPrinterRecipe(
                recipeId,
                normalizedName,
                sizeX,
                sizeY,
                sizeZ,
                System.currentTimeMillis()
        );

        Map<String, Integer> paletteIndexMap = new LinkedHashMap<>();
        Map<Item, Integer> materialCounter = new LinkedHashMap<>();
        int storedBlocks = 0;

        for (int y = 0; y < sizeY; y++) {
            for (int x = 1; x <= sizeX; x++) {
                for (int z = 1; z <= sizeZ; z++) {
                    // Локальную позицию превращаем в мировую с учётом FACING принтера
                    BlockPos targetPos = rotateLocalToWorld(printerPos, forwardRotation, x, y, z);
                    BlockState worldState = level.getBlockState(targetPos);

                    if (worldState.isAir()) {
                        continue;
                    }

                    // Состояние из мира → нормализуем к локальной системе (принтер смотрит NORTH)
                    BlockState localState = rotateStateSmart(worldState, inverseRotation);

                    String stateString = PrinterStateHelper.sanitizeStateString(BlockStateParser.serialize(localState));

                    int paletteIndex = paletteIndexMap.computeIfAbsent(stateString, key -> {
                        recipe.addPaletteState(key);
                        return recipe.getPalette().size() - 1;
                    });

                    recipe.addPlacement(x, y, z, paletteIndex);
                    storedBlocks++;

                    if (!PrinterRecipeLimits.isStoredBlockCountAllowed(storedBlocks)) {
                        return ScanResult.error("Слишком много блоков в рецепте: максимум " + PrinterRecipeLimits.MAX_STORED_BLOCKS);
                    }

                    Item requiredItem = PrinterStateHelper.resolveRequiredItem(stateString, localState);
                    if (requiredItem != Items.AIR) {
                        materialCounter.merge(requiredItem, 1, Integer::sum);
                    }
                }
            }
        }

        for (Map.Entry<Item, Integer> entry : materialCounter.entrySet()) {
            recipe.putMaterial(entry.getKey(), entry.getValue());
        }

        if (storedBlocks <= 0) {
            return ScanResult.error("В зоне скана нет блоков");
        }

        if (!recipe.isValid()) {
            return ScanResult.error("Рецепт получился невалидным");
        }

        return ScanResult.success(recipe, storedBlocks);
    }

    private static Rotation getPrinterRotation(Printer3DBlockEntity printer) {
        Direction facing = printer.getBlockState().hasProperty(Printer3DBlock.FACING)
                ? printer.getBlockState().getValue(Printer3DBlock.FACING)
                : Direction.NORTH;

        return switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static Rotation inverseOf(Rotation rotation) {
        return switch (rotation) {
            case NONE -> Rotation.NONE;
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
        };
    }

    private static BlockPos rotateLocalToWorld(BlockPos printerPos, Rotation rotation, int relX, int relY, int relZ) {
        int rx = relX;
        int rz = relZ;

        switch (rotation) {
            case NONE -> {
                rx = relX;
                rz = relZ;
            }
            case CLOCKWISE_90 -> {
                rx = -relZ;
                rz = relX;
            }
            case CLOCKWISE_180 -> {
                rx = -relX;
                rz = -relZ;
            }
            case COUNTERCLOCKWISE_90 -> {
                rx = relZ;
                rz = -relX;
            }
        }

        return new BlockPos(printerPos.getX() + rx, printerPos.getY() + relY, printerPos.getZ() + rz);
    }

    /**
     * Универсальная ротация BlockState. Работает для всех блоков включая
     * hopper/chest/dispenser/dropper/barrel где ванильный state.rotate() no-op.
     */
    private static BlockState rotateStateSmart(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return state;
        }

        BlockState result;
        try {
            result = state.rotate(rotation);
        } catch (Exception ignored) {
            result = state;
        }

        // Дополнительно ротируем HORIZONTAL_FACING если ваниль не тронула
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction orig = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction curr = result.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? result.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : orig;
            if (curr == orig) {
                try {
                    result = result.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(orig));
                } catch (Exception ignored) {
                }
            }
        }

        // То же самое для 6-направленного FACING (hopper/dispenser/dropper/observer/piston/barrel)
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction orig = state.getValue(BlockStateProperties.FACING);
            Direction curr = result.hasProperty(BlockStateProperties.FACING)
                    ? result.getValue(BlockStateProperties.FACING)
                    : orig;
            if (curr == orig && orig.getAxis().isHorizontal()) {
                try {
                    result = result.setValue(BlockStateProperties.FACING, rotation.rotate(orig));
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    public record ScanResult(boolean success, String message, PlayerStoredPrinterRecipe recipe, int blockCount) {
        public static ScanResult success(PlayerStoredPrinterRecipe recipe, int blockCount) {
            return new ScanResult(true, "OK", recipe, blockCount);
        }

        public static ScanResult error(String message) {
            return new ScanResult(false, message, null, 0);
        }
    }
}
