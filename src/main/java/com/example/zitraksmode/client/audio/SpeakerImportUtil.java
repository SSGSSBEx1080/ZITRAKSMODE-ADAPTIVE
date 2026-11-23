package com.example.zitraksmode.client.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class SpeakerImportUtil {

    private SpeakerImportUtil() {
    }

    public static PreparedTrack prepareTrack(Path inputFile) throws IOException {
        if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile)) {
            throw new IOException("Файл не найден: " + inputFile);
        }

        String fileName = inputFile.getFileName().toString();
        String lower = fileName.toLowerCase(Locale.ROOT);

        if (!lower.endsWith(".ogg") && !lower.endsWith(".mp3")) {
            throw new IOException("Поддерживаются только .ogg и .mp3: " + fileName);
        }

        Path oggFile = inputFile;
        boolean deleteTemp = false;
        try {
            if (lower.endsWith(".mp3")) {
                oggFile = SpeakerFfmpegUtil.convertMp3ToOgg(inputFile);
                deleteTemp = true;
            }

            long size = Files.size(oggFile);
            if (size <= 0L) {
                throw new IOException("Пустой аудиофайл: " + fileName);
            }
            if (size > 32L * 1024L * 1024L) {
                throw new IOException("Файл слишком большой. Максимум 32 MB: " + fileName);
            }

            byte[] oggBytes = Files.readAllBytes(oggFile);
            long durationMs = measureOggDurationMillis(oggBytes);

            String baseName = stripExtension(fileName);
            ParsedName parsedName = parseArtistAndTitle(baseName);
            String uploadFileName = sanitizeFileName(parsedName.title) + ".ogg";

            return new PreparedTrack(uploadFileName, parsedName.title, parsedName.author, oggBytes, durationMs);
        } finally {
            if (deleteTemp) {
                try {
                    Files.deleteIfExists(oggFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static boolean isSupportedAudioFile(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return lower.endsWith(".ogg") || lower.endsWith(".mp3");
    }

    public static long measureOggDurationMillis(byte[] oggBytes) throws IOException {
        ByteBuffer fileBuffer = BufferUtils.createByteBuffer(oggBytes.length);
        fileBuffer.put(oggBytes);
        fileBuffer.flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null);
            if (decoder == MemoryUtil.NULL) {
                throw new IOException("Не удалось прочитать OGG, код ошибки stb_vorbis=" + error.get(0));
            }

            try {
                float seconds = STBVorbis.stb_vorbis_stream_length_in_seconds(decoder);
                if (seconds <= 0.0F) {
                    return 0L;
                }
                return (long) (seconds * 1000.0F);
            } finally {
                STBVorbis.stb_vorbis_close(decoder);
            }
        }
    }

    private static ParsedName parseArtistAndTitle(String baseName) {
        String cleaned = baseName.trim().replaceAll("\\s+", " ");
        String[] split = cleaned.split(" - ", 2);
        if (split.length == 2) {
            return new ParsedName(
                    sanitizeText(split[0], 128),
                    sanitizeText(split[1], 128)
            );
        }
        return new ParsedName("Unknown", sanitizeText(cleaned, 128));
    }

    private static String sanitizeFileName(String value) {
        String cleaned = sanitizeText(value, 128);
        cleaned = cleaned.replaceAll("[\\/:*?\"<>|]", "_");
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return "track";
        }
        return cleaned;
    }

    private static String sanitizeText(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private static class ParsedName {
        private final String author;
        private final String title;

        private ParsedName(String author, String title) {
            this.author = author;
            this.title = title;
        }
    }

    public static class PreparedTrack {
        private final String fileName;
        private final String title;
        private final String author;
        private final byte[] oggBytes;
        private final long durationMs;

        public PreparedTrack(String fileName, String title, String author, byte[] oggBytes, long durationMs) {
            this.fileName = fileName;
            this.title = title;
            this.author = author;
            this.oggBytes = oggBytes;
            this.durationMs = durationMs;
        }

        public String getFileName() {
            return fileName;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public byte[] getOggBytes() {
            return oggBytes;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }
}
