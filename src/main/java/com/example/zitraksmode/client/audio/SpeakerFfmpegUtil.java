package com.example.zitraksmode.client.audio;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class SpeakerFfmpegUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String WINDOWS_RESOURCE_PATH = "/ffmpeg/windows/ffmpeg.exe";
    private static final Object INSTALL_LOCK = new Object();
    private static volatile boolean installAttempted = false;

    private SpeakerFfmpegUtil() {
    }

    public static void ensureBundledFfmpegInstalledAsync() {
        Thread thread = new Thread(() -> {
            try {
                ensureBundledFfmpegInstalled();
            } catch (Exception e) {
                LOGGER.warn("JBL: bundled ffmpeg auto-install skipped: {}", rootMessage(e));
            }
        }, "JBL-FFmpeg-Installer");
        thread.setDaemon(true);
        thread.start();
    }

    public static Path convertMp3ToOgg(Path mp3Path) throws IOException {
        String ffmpegBinary = resolveFfmpegBinary();
        String baseName = stripExtension(mp3Path.getFileName().toString());
        Path output = SpeakerClientTrackCache.createTempOggFile(baseName);

        List<String> command = new ArrayList<>();
        command.add(ffmpegBinary);
        command.add("-y");
        command.add("-i");
        command.add(mp3Path.toAbsolutePath().toString());
        command.add("-vn");
        command.add("-c:a");
        command.add("libvorbis");
        command.add("-q:a");
        command.add("5");
        command.add(output.toAbsolutePath().toString());

        LOGGER.info("JBL: starting MP3 -> OGG conversion: {}", mp3Path.getFileName());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String outputText;
        try (InputStream inputStream = process.getInputStream()) {
            outputText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        boolean finished;
        try {
            finished = process.waitFor(180, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ffmpeg conversion interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("ffmpeg timeout while converting " + mp3Path.getFileName());
        }

        if (process.exitValue() != 0 || !Files.exists(output) || Files.size(output) <= 0L) {
            throw new IOException("ffmpeg failed for " + mp3Path.getFileName() + "\n" + outputText);
        }

        LOGGER.info("JBL: converted MP3 to OGG using ffmpeg: {} -> {}", mp3Path, output);
        return output;
    }

    public static String resolveFfmpegBinary() throws IOException {
        ensureBundledFfmpegInstalled();

        Path gameDir = FMLPaths.GAMEDIR.get();

        List<Path> directCandidates = List.of(
                gameDir.resolve("zitraksmode").resolve("tools").resolve("ffmpeg.exe"),
                gameDir.resolve("zitraksmode").resolve("tools").resolve("ffmpeg"),
                gameDir.resolve("zitraksmode").resolve("tools").resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe"),
                gameDir.resolve("zitraksmode").resolve("tools").resolve("ffmpeg").resolve("ffmpeg.exe"),
                gameDir.resolve("ffmpeg.exe")
        );

        for (Path candidate : directCandidates) {
            if (Files.isRegularFile(candidate)) {
                LOGGER.info("JBL: ffmpeg found at {}", candidate.toAbsolutePath());
                return candidate.toAbsolutePath().toString();
            }
        }

        Path toolsDir = gameDir.resolve("zitraksmode").resolve("tools");
        Path discovered = findFfmpegRecursively(toolsDir, 8);
        if (discovered != null) {
            LOGGER.info("JBL: ffmpeg found recursively at {}", discovered.toAbsolutePath());
            return discovered.toAbsolutePath().toString();
        }

        if (isCommandAvailable("ffmpeg")) {
            LOGGER.info("JBL: using system ffmpeg from PATH");
            return "ffmpeg";
        }
        if (isCommandAvailable("ffmpeg.exe")) {
            LOGGER.info("JBL: using system ffmpeg.exe from PATH");
            return "ffmpeg.exe";
        }

        throw new IOException(
                "Не найден ffmpeg. Положи его сюда: " +
                        gameDir.resolve("zitraksmode").resolve("tools").resolve("ffmpeg.exe") +
                        " или зашей ffmpeg в resources по пути src/main/resources/ffmpeg/windows/ffmpeg.exe"
        );
    }

    public static Path ensureBundledFfmpegInstalled() throws IOException {
        synchronized (INSTALL_LOCK) {
            Path target = getEmbeddedInstallTarget();

            if (Files.isRegularFile(target) && Files.size(target) > 0L) {
                installAttempted = true;
                return target;
            }

            if (installAttempted) {
                return target;
            }
            installAttempted = true;

            if (!isWindows()) {
                LOGGER.info("JBL: bundled ffmpeg auto-install skipped, OS is not Windows");
                return target;
            }

            try (InputStream inputStream = SpeakerFfmpegUtil.class.getResourceAsStream(WINDOWS_RESOURCE_PATH)) {
                if (inputStream == null) {
                    LOGGER.warn("JBL: bundled ffmpeg resource not found at {}", WINDOWS_RESOURCE_PATH);
                    return target;
                }

                Files.createDirectories(target.getParent());
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                target.toFile().setExecutable(true, false);

                LOGGER.info("JBL: extracted bundled ffmpeg to {}", target.toAbsolutePath());
                return target;
            }
        }
    }

    public static Path getEmbeddedInstallTarget() {
        return FMLPaths.GAMEDIR.get().resolve("zitraksmode").resolve("tools").resolve("ffmpeg.exe");
    }

    private static Path findFfmpegRecursively(Path root, int maxDepth) {
        if (root == null || !Files.exists(root)) {
            return null;
        }

        try (Stream<Path> stream = Files.walk(root, maxDepth)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.equals("ffmpeg.exe") || name.equals("ffmpeg");
                    })
                    .sorted(Comparator.comparingInt(path -> path.toString().length()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command, "-version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
