package com.example.zitraksmode.client.printer;

import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class PrinterRecipeFileStorage {

    private static final String FILE_EXTENSION = ".zlp";
    private static final int FORMAT_VERSION = 1;

    private PrinterRecipeFileStorage() {
    }

    public static Path getExportDirectory() {
        return FMLPaths.GAMEDIR.get()
                .resolve("zitraksmode")
                .resolve("printer_exports");
    }

    public static String getRelativeFolderDisplay() {
        return "zitraksmode/printer_exports";
    }

    public static ExportResult exportRecipe(PlayerStoredPrinterRecipe recipe) throws IOException {
        if (recipe == null || !recipe.isValid()) {
            throw new IOException("Рецепт пустой или повреждён");
        }

        Files.createDirectories(getExportDirectory());

        Path file = createUniqueFilePath(recipe);

        CompoundTag root = new CompoundTag();
        root.putString("fileType", "zitraksmode_printer_recipe");
        root.putInt("formatVersion", FORMAT_VERSION);
        root.putLong("exportedAt", System.currentTimeMillis());
        root.put("recipe", recipe.save());

        try (OutputStream outputStream = Files.newOutputStream(file)) {
            NbtIo.writeCompressed(root, outputStream);
        }

        return new ExportResult(file.getFileName().toString(), file);
    }

    public static List<ImportEntry> listImportableFiles() throws IOException {
        Path directory = getExportDirectory();
        if (!Files.exists(directory)) {
            return List.of();
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(directory)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FILE_EXTENSION))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        List<ImportEntry> result = new ArrayList<>();
        for (Path file : files) {
            try {
                PlayerStoredPrinterRecipe recipe = readRecipe(file);
                result.add(new ImportEntry(
                        file.getFileName().toString(),
                        recipe.getDisplayName(),
                        recipe.getSizeX(),
                        recipe.getSizeY(),
                        recipe.getSizeZ(),
                        recipe.getStoredBlockCount()
                ));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public static PlayerStoredPrinterRecipe readRecipeFromFile(String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new IOException("Пустое имя файла");
        }

        Path file = getExportDirectory().resolve(fileName).normalize();
        Path normalizedDirectory = getExportDirectory().normalize();

        if (!file.startsWith(normalizedDirectory)) {
            throw new IOException("Недопустимый путь файла");
        }

        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("Файл не найден");
        }

        return readRecipe(file);
    }

    private static PlayerStoredPrinterRecipe readRecipe(Path file) throws IOException {
        CompoundTag root;
        try (InputStream inputStream = Files.newInputStream(file)) {
            root = NbtIo.readCompressed(inputStream);
        }

        if (root == null) {
            throw new IOException("Файл пустой");
        }

        CompoundTag recipeTag;
        if (root.contains("recipe", Tag.TAG_COMPOUND)) {
            recipeTag = root.getCompound("recipe");
        } else {
            recipeTag = root;
        }

        PlayerStoredPrinterRecipe recipe = PlayerStoredPrinterRecipe.load(recipeTag);
        if (recipe == null || !recipe.isValid()) {
            throw new IOException("Файл рецепта повреждён");
        }

        return recipe.copy();
    }

    private static Path createUniqueFilePath(PlayerStoredPrinterRecipe recipe) {
        String displayName = sanitizeFileName(recipe.getDisplayName());
        if (displayName.isBlank()) {
            displayName = sanitizeFileName(recipe.getRecipeId());
        }
        if (displayName.isBlank()) {
            displayName = "printer_recipe";
        }

        String recipeId = sanitizeFileName(recipe.getRecipeId());
        String baseName = recipeId.isBlank() ? displayName : displayName + "_" + recipeId;

        Path file = getExportDirectory().resolve(baseName + FILE_EXTENSION);
        int index = 2;

        while (Files.exists(file)) {
            file = getExportDirectory().resolve(baseName + "_" + index + FILE_EXTENSION);
            index++;
        }

        return file;
    }

    private static String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }

        return name.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^\\p{L}\\p{Nd}_\\-]+", "");
    }

    public record ExportResult(String fileName, Path path) {
    }

    public record ImportEntry(String fileName, String displayName, int sizeX, int sizeY, int sizeZ, int blockCount) {
    }
}