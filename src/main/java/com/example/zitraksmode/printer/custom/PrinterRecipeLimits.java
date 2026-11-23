package com.example.zitraksmode.printer.custom;

public final class PrinterRecipeLimits {
    public static final int MAX_RECIPES_PER_PLAYER = 20;
    public static final int MAX_NAME_LENGTH = 96;

    /** Large scan volume for Create-style schematics. */
    public static final int MAX_SCAN_VOLUME = 200_000;

    /** Maximum non-air blocks saved in one custom printer recipe. */
    public static final int MAX_STORED_BLOCKS = 50_000;

    private PrinterRecipeLimits() {
    }

    public static long volume(int sizeX, int sizeY, int sizeZ) {
        return (long) sizeX * (long) sizeY * (long) sizeZ;
    }

    public static boolean isDimensionInputValid(int sizeX, int sizeY, int sizeZ) {
        return sizeX > 0 && sizeY > 0 && sizeZ > 0;
    }

    public static boolean isVolumeAllowed(int sizeX, int sizeY, int sizeZ) {
        return isDimensionInputValid(sizeX, sizeY, sizeZ)
                && volume(sizeX, sizeY, sizeZ) <= MAX_SCAN_VOLUME;
    }

    public static boolean isStoredBlockCountAllowed(int blockCount) {
        return blockCount >= 0 && blockCount <= MAX_STORED_BLOCKS;
    }

    public static String normalizeName(String name) {
        if (name == null) return "";

        String cleaned = name.trim();
        if (cleaned.length() > MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_NAME_LENGTH).trim();
        }
        return cleaned;
    }
}
