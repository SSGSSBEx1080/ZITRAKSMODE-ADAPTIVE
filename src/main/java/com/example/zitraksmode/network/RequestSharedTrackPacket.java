package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestSharedTrackPacket {

    public static final int CHUNK_SIZE = 24 * 1024;

    private final UUID ownerUuid;
    private final String trackId;

    public RequestSharedTrackPacket(UUID ownerUuid, String trackId) {
        this.ownerUuid = ownerUuid;
        this.trackId = trackId;
    }

    public static void encode(RequestSharedTrackPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.ownerUuid);
        buf.writeUtf(msg.trackId, 128);
    }

    public static RequestSharedTrackPacket decode(FriendlyByteBuf buf) {
        return new RequestSharedTrackPacket(buf.readUUID(), buf.readUtf(128));
    }

    public static void handle(RequestSharedTrackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer requester = ctx.getSender();
            if (requester == null || requester.getServer() == null) {
                return;
            }

            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(requester.getServer());
                SpeakerTrackMeta meta = storage.getTrackMeta(msg.ownerUuid, msg.trackId);
                if (meta == null) {
                    return;
                }

                byte[] bytes = storage.readTrackBytes(msg.ownerUuid, msg.trackId);
                int totalChunks = (bytes.length + CHUNK_SIZE - 1) / CHUNK_SIZE;

                PacketHandler.sendToPlayer(new SharedTrackStartPacket(meta, bytes.length, totalChunks), requester);
                for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                    int start = chunkIndex * CHUNK_SIZE;
                    int end = Math.min(bytes.length, start + CHUNK_SIZE);
                    byte[] chunk = Arrays.copyOfRange(bytes, start, end);
                    PacketHandler.sendToPlayer(new SharedTrackChunkPacket(msg.ownerUuid, msg.trackId, chunkIndex, chunk), requester);
                }
                PacketHandler.sendToPlayer(new SharedTrackFinishPacket(msg.ownerUuid, msg.trackId), requester);
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
