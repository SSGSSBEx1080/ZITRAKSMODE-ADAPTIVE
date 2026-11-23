package com.example.zitraksmode.client.audio;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.network.JBLSpeakerNetwork;
import com.example.zitraksmode.network.RequestSpeakerLibraryPacket;
import com.example.zitraksmode.network.UploadTrackChunkPacket;
import com.example.zitraksmode.network.UploadTrackFinishPacket;
import com.example.zitraksmode.network.UploadTrackStartPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class MusicTrackManager {

    public static final int UPLOAD_CHUNK_SIZE = 24 * 1024;

    private static final MusicTrackManager INSTANCE = new MusicTrackManager();

    private final List<SpeakerTrackMeta> library = new CopyOnWriteArrayList<>();

    public static MusicTrackManager getInstance() {
        return INSTANCE;
    }

    public static void init() {
        SpeakerFfmpegUtil.ensureBundledFfmpegInstalledAsync();
    }

    public static List<String> getAvailableTracks() {
        List<String> result = new ArrayList<>();
        for (SpeakerTrackMeta meta : INSTANCE.library) {
            result.add(meta.getTitle());
        }
        return result;
    }

    public List<SpeakerTrackMeta> getLibrary() {
        return List.copyOf(library);
    }

    public SpeakerTrackMeta getTrackById(String trackId) {
        for (SpeakerTrackMeta meta : library) {
            if (meta.getTrackId().equals(trackId)) {
                return meta;
            }
        }
        return null;
    }

    public void replaceLibrary(List<SpeakerTrackMeta> newTracks) {
        List<SpeakerTrackMeta> sorted = new ArrayList<>(newTracks);
        sorted.sort(Comparator.comparingLong(SpeakerTrackMeta::getCreatedAt).reversed());
        library.clear();
        library.addAll(sorted);
    }

    public void requestLibraryRefresh() {
        JBLSpeakerNetwork.sendToServer(new RequestSpeakerLibraryPacket());
    }

    public void uploadTracksAsync(List<Path> paths, Consumer<String> statusConsumer) {
        CompletableFuture.runAsync(() -> {
            for (Path path : paths) {
                if (!SpeakerImportUtil.isSupportedAudioFile(path)) {
                    pushStatus(statusConsumer, "Пропуск: " + path.getFileName() + " (только .ogg/.mp3)");
                    continue;
                }
                uploadSingleTrack(path, statusConsumer);
            }

            pushStatus(statusConsumer, "Библиотека обновляется...");
            JBLSpeakerNetwork.sendToServer(new RequestSpeakerLibraryPacket());
            pushStatus(statusConsumer, "Импорт завершён");
        }).exceptionally(throwable -> {
            pushStatus(statusConsumer, "Ошибка импорта: " + rootMessage(throwable));
            return null;
        });
    }

    private void uploadSingleTrack(Path path, Consumer<String> statusConsumer) {
        try {
            pushStatus(statusConsumer, "Подготовка: " + path.getFileName());
            SpeakerImportUtil.PreparedTrack preparedTrack = SpeakerImportUtil.prepareTrack(path);
            byte[] data = preparedTrack.getOggBytes();
            UUID sessionId = UUID.randomUUID();

            JBLSpeakerNetwork.sendToServer(new UploadTrackStartPacket(
                    sessionId,
                    preparedTrack.getFileName(),
                    preparedTrack.getTitle(),
                    preparedTrack.getAuthor(),
                    data.length,
                    preparedTrack.getDurationMs()
            ));

            int totalChunks = (data.length + UPLOAD_CHUNK_SIZE - 1) / UPLOAD_CHUNK_SIZE;
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int start = chunkIndex * UPLOAD_CHUNK_SIZE;
                int end = Math.min(data.length, start + UPLOAD_CHUNK_SIZE);
                byte[] chunk = Arrays.copyOfRange(data, start, end);
                JBLSpeakerNetwork.sendToServer(new UploadTrackChunkPacket(sessionId, chunkIndex, chunk));
                pushStatus(statusConsumer, "Загрузка: " + preparedTrack.getTitle() + " [" + (chunkIndex + 1) + "/" + totalChunks + "]");
            }

            JBLSpeakerNetwork.sendToServer(new UploadTrackFinishPacket(sessionId));
            pushStatus(statusConsumer, "Готово: " + preparedTrack.getTitle());
        } catch (Exception e) {
            pushStatus(statusConsumer, "Ошибка файла " + path.getFileName() + ": " + rootMessage(e));
        }
    }

    private void pushStatus(Consumer<String> statusConsumer, String text) {
        if (statusConsumer == null) {
            return;
        }
        Minecraft.getInstance().execute(() -> statusConsumer.accept(text));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
