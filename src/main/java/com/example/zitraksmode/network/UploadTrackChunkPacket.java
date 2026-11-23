package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public class UploadTrackChunkPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int MAX_CHUNK_SIZE = 24 * 1024;

    private final UUID sessionId;
    private final int chunkIndex;
    private final byte[] data;

    public UploadTrackChunkPacket(UUID sessionId, int chunkIndex, byte[] data) {
        this.sessionId = sessionId;
        this.chunkIndex = chunkIndex;
        this.data = data;
    }

    public static void encode(UploadTrackChunkPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.sessionId);
        buf.writeVarInt(msg.chunkIndex);
        buf.writeVarInt(msg.data.length);
        buf.writeBytes(msg.data);
    }

    public static UploadTrackChunkPacket decode(FriendlyByteBuf buf) {
        UUID sessionId = buf.readUUID();
        int chunkIndex = buf.readVarInt();
        int length = buf.readVarInt();
        if (length < 0 || length > MAX_CHUNK_SIZE + 1024) {
            throw new IllegalArgumentException("Invalid JBL chunk length: " + length);
        }
        byte[] data = new byte[length];
        buf.readBytes(data);
        return new UploadTrackChunkPacket(sessionId, chunkIndex, data);
    }

    public static void handle(UploadTrackChunkPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
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
                SpeakerTrackStorage.forServer(player.getServer()).appendUploadChunk(player, msg.sessionId, msg.chunkIndex, msg.data);
            } catch (Exception e) {
                LOGGER.error("JBL: failed to append chunk {} for {}", msg.chunkIndex, player.getGameProfile().getName(), e);
            }
        });
        ctx.setPacketHandled(true);
    }
}
