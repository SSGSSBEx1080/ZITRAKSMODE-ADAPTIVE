package com.example.zitraksmode.network.max;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.function.Supplier;

public class DownloadMaxAttachmentPacket {

    private final UUID attachmentId;
    private final String fileName;
    private final String mimeType;
    private final byte[] data;

    public DownloadMaxAttachmentPacket(UUID attachmentId, String fileName, String mimeType, byte[] data) {
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.data = data;
    }

    public static void encode(DownloadMaxAttachmentPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.attachmentId);
        buf.writeUtf(packet.fileName, 256);
        buf.writeUtf(packet.mimeType, 128);
        buf.writeByteArray(packet.data);
    }

    public static DownloadMaxAttachmentPacket decode(FriendlyByteBuf buf) {
        return new DownloadMaxAttachmentPacket(buf.readUUID(), buf.readUtf(256), buf.readUtf(128), buf.readByteArray());
    }

    public static void handle(DownloadMaxAttachmentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(DownloadMaxAttachmentPacket packet) {
        try {
            Path folder = preferredDownloadFolder();
            Files.createDirectories(folder);
            Path out = uniquePath(folder, sanitizeFileName(packet.fileName));
            Files.write(out, packet.data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("MAX: файл сохранён -> " + out.toAbsolutePath()), false);
            }
        } catch (Exception exception) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("MAX: ошибка скачивания -> " + exception.getClass().getSimpleName()), false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static Path preferredDownloadFolder() {
        try {
            Path downloads = Path.of(System.getProperty("user.home"), "Downloads", "MAX");
            return Files.exists(downloads.getParent()) ? downloads : Minecraft.getInstance().gameDirectory.toPath().resolve("zitraksmode").resolve("max_downloads");
        } catch (Exception ignored) {
            return Minecraft.getInstance().gameDirectory.toPath().resolve("zitraksmode").resolve("max_downloads");
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
