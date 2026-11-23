package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public class UploadTrackStartPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final UUID sessionId;
    private final String fileName;
    private final String title;
    private final String author;
    private final long expectedSize;
    private final long durationMs;

    public UploadTrackStartPacket(UUID sessionId, String fileName, String title, String author, long expectedSize, long durationMs) {
        this.sessionId = sessionId;
        this.fileName = fileName;
        this.title = title;
        this.author = author;
        this.expectedSize = expectedSize;
        this.durationMs = durationMs;
    }

    public static void encode(UploadTrackStartPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.sessionId);
        buf.writeUtf(msg.fileName, 256);
        buf.writeUtf(msg.title, 256);
        buf.writeUtf(msg.author, 256);
        buf.writeVarLong(msg.expectedSize);
        buf.writeVarLong(msg.durationMs);
    }

    public static UploadTrackStartPacket decode(FriendlyByteBuf buf) {
        return new UploadTrackStartPacket(
                buf.readUUID(),
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readVarLong(),
                buf.readVarLong()
        );
    }

    public static void handle(UploadTrackStartPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            try {
                if (player.getServer() == null) {
                    return;
                }
                SpeakerTrackStorage.forServer(player.getServer()).beginUpload(
                        player,
                        msg.sessionId,
                        msg.fileName,
                        msg.title,
                        msg.author,
                        msg.expectedSize,
                        msg.durationMs
                );
            } catch (Exception e) {
                LOGGER.error("JBL: failed to start upload for {}", player.getGameProfile().getName(), e);
            }
        });
        ctx.setPacketHandled(true);
    }
}
    