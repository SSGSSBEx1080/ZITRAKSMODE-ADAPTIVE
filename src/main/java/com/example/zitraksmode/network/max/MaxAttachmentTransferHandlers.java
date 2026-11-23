package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxAttachmentStorage;
import com.example.zitraksmode.max.MaxClientTransferState;
import com.example.zitraksmode.max.MaxFileAttachment;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class MaxAttachmentTransferHandlers {

    private static final int CHUNK_SIZE = 30 * 1024;
    private static final Map<UUID, Path> CLIENT_DOWNLOAD_TARGETS = new HashMap<>();

    private MaxAttachmentTransferHandlers() {
    }

    public static void handleStartUpload(StartMaxAttachmentUploadPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }
            try {
                MaxAttachmentUploadManager.start(
                        sender.getServer(),
                        packet.attachmentId,
                        sender.getUUID(),
                        packet.recipientId,
                        packet.caption,
                        packet.replyToMessageId,
                        packet.fileName,
                        packet.mimeType,
                        packet.totalBytes,
                        packet.previewBytes
                );
            } catch (Exception ignored) {
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleUploadChunk(MaxAttachmentUploadChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }
            try {
                MaxAttachmentUploadManager.append(sender.getServer(), packet.attachmentId, packet.chunkData);
            } catch (Exception ignored) {
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleFinishUpload(FinishMaxAttachmentUploadPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }
            try {
                MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
                MaxAttachmentUploadManager.UploadResult result =
                        MaxAttachmentUploadManager.finish(sender.getServer(), data, sender, packet.attachmentId);
                if (result != null) {
                    MaxSyncHelper.syncPair(sender.getServer(), data, result.senderId(), result.recipientId());
                }
            } catch (Exception ignored) {
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleRequestDownload(RequestMaxAttachmentDownloadPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            try {
                MaxMessengerSavedData data = MaxMessengerSavedData.get(player.getServer());
                if (!data.canUserDownloadAttachment(player.getUUID(), packet.getAttachmentId())) {
                    return;
                }

                MaxFileAttachment attachment = data.getAttachment(packet.getAttachmentId());
                if (attachment == null) {
                    return;
                }

                MaxPackets.sendToPlayer(
                        new StartMaxAttachmentDownloadPacket(
                                packet.getAttachmentId(),
                                attachment.getFileName(),
                                attachment.getMimeType(),
                                attachment.getSizeBytesLong()
                        ),
                        player
                );

                try (InputStream input = MaxAttachmentStorage.openForRead(player.getServer(), packet.getAttachmentId())) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        byte[] chunk = new byte[read];
                        System.arraycopy(buffer, 0, chunk, 0, read);
                        MaxPackets.sendToPlayer(new MaxAttachmentDownloadChunkPacket(packet.getAttachmentId(), chunk), player);
                    }
                }

                MaxPackets.sendToPlayer(new FinishMaxAttachmentDownloadPacket(packet.getAttachmentId(), true), player);
            } catch (Exception ignored) {
                ServerPlayer player2 = ctx.get().getSender();
                if (player2 != null) {
                    MaxPackets.sendToPlayer(new FinishMaxAttachmentDownloadPacket(packet.getAttachmentId(), false), player2);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleStartDownloadClient(StartMaxAttachmentDownloadPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleStartDownloadClientInternal(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleStartDownloadClientInternal(StartMaxAttachmentDownloadPacket packet) {
        try {
            Path folder = preferredDownloadFolder();
            Files.createDirectories(folder);
            Path out = uniquePath(folder, sanitizeFileName(packet.fileName));
            Files.deleteIfExists(out);
            Files.createFile(out);
            CLIENT_DOWNLOAD_TARGETS.put(packet.attachmentId, out);
            MaxClientTransferState.startDownload(packet.attachmentId, packet.fileName, packet.totalBytes, out);
        } catch (Exception ignored) {
        }
    }

    public static void handleDownloadChunkClient(MaxAttachmentDownloadChunkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleDownloadChunkClientInternal(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleDownloadChunkClientInternal(MaxAttachmentDownloadChunkPacket packet) {
        try {
            Path out = CLIENT_DOWNLOAD_TARGETS.get(packet.attachmentId);
            if (out == null) {
                return;
            }
            try (OutputStream output = Files.newOutputStream(out, StandardOpenOption.APPEND)) {
                output.write(packet.chunkData);
            }
            MaxClientTransferState.advanceDownload(packet.attachmentId, packet.chunkData.length);
        } catch (Exception ignored) {
        }
    }

    public static void handleFinishDownloadClient(FinishMaxAttachmentDownloadPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleFinishDownloadClientInternal(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleFinishDownloadClientInternal(FinishMaxAttachmentDownloadPacket packet) {
        Path out = CLIENT_DOWNLOAD_TARGETS.remove(packet.attachmentId);
        MaxClientTransferState.finishDownload(
                packet.attachmentId,
                packet.success,
                packet.success ? "Скачано" : "Ошибка",
                out
        );

        if (Minecraft.getInstance().player != null && out != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(packet.success
                            ? "MAX: файл сохранён -> " + out.toAbsolutePath()
                            : "MAX: ошибка скачивания"),
                    false
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static Path preferredDownloadFolder() {
        try {
            Path downloads = Path.of(System.getProperty("user.home"), "Downloads", "MAX");
            return Files.exists(downloads.getParent())
                    ? downloads
                    : Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("zitraksmode")
                    .resolve("max_downloads");
        } catch (Exception ignored) {
            return Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("zitraksmode")
                    .resolve("max_downloads");
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static Path uniquePath(Path folder, String fileName) {
        String safe = fileName == null || fileName.isBlank() ? "file.bin" : fileName;
        Path out = folder.resolve(safe);
        if (!Files.exists(out)) {
            return out;
        }

        int dot = safe.lastIndexOf('.');
        String base = dot >= 0 ? safe.substring(0, dot) : safe;
        String ext = dot >= 0 ? safe.substring(dot) : "";
        int i = 1;
        while (Files.exists(out)) {
            out = folder.resolve(base + " (" + i + ")" + ext);
            i++;
        }
        return out;
    }

    @OnlyIn(Dist.CLIENT)
    private static String sanitizeFileName(String fileName) {
        return (fileName == null ? "file.bin" : fileName).replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
