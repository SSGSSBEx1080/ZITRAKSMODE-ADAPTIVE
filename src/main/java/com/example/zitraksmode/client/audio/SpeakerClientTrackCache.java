package com.example.zitraksmode.client.audio;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class SpeakerClientTrackCache {

    private SpeakerClientTrackCache() {
    }

    public static Path getCacheRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("zitraksmode")
                .resolve("cache_audio");
    }

    public static Path getOwnerDir(UUID ownerId) {
        return getCacheRoot().resolve(ownerId.toString());
    }

    public static File getTrackFile(SpeakerTrackMeta meta) {
        return getOwnerDir(meta.getOwnerId()).resolve(meta.getTrackId() + ".ogg").toFile();
    }

    public static boolean hasTrack(SpeakerTrackMeta meta) {
        return getTrackFile(meta).isFile();
    }

    public static File storeTrack(SpeakerTrackMeta meta, byte[] oggBytes) throws IOException {
        Path path = getOwnerDir(meta.getOwnerId()).resolve(meta.getTrackId() + ".ogg");
        Files.createDirectories(path.getParent());
        Files.write(path, oggBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return path.toFile();
    }

    public static Path getImportTempDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("zitraksmode")
                .resolve("temp_audio_imports");
    }

    public static Path createTempOggFile(String baseName) throws IOException {
        Files.createDirectories(getImportTempDir());
        String safe = baseName == null || baseName.isBlank()
                ? "track"
                : baseName.replaceAll("[^a-zA-Z0-9а-яА-ЯёЁ_-]", "_");
        return Files.createTempFile(getImportTempDir(), safe + "_", ".ogg");
    }
}