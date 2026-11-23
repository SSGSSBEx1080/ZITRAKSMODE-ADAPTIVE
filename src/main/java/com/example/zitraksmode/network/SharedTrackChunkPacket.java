package com.example.zitraksmode.network;

import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SharedTrackChunkPacket {

    public static final int MAX_CHUNK_SIZE = 24 * 1024;

    private final UUID ownerUuid;
    private final String trackId;
    private final int chunkIndex;
    private final byte[] data;

    public SharedTrackChunkPacket(UUID ownerUuid, String trackId, int chunkIndex, byte[] data) {
        this.ownerUuid = ownerUuid;
        this.trackId = trackId;
        this.chunkIndex = chunkIndex;
        this.data = data;
    }

    public static void encode(SharedTrackChunkPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.ownerUuid);
        buf.writeUtf(msg.trackId, 128);
        buf.writeVarInt(msg.chunkIndex);
        buf.writeVarInt(msg.data.length);
        buf.writeBytes(msg.data);
    }

    public static SharedTrackChunkPacket decode(FriendlyByteBuf buf) {
        UUID ownerUuid = buf.readUUID();
        String trackId = buf.readUtf(128);
        int chunkIndex = buf.readVarInt();
        int length = buf.readVarInt();
        if (length < 0 || length > MAX_CHUNK_SIZE + 1024) {
            throw new IllegalArgumentException("Invalid shared JBL chunk length: " + length);
        }
        byte[] data = new byte[length];
        buf.readBytes(data);
        return new SharedTrackChunkPacket(ownerUuid, trackId, chunkIndex, data);
    }

    public static void handle(SharedTrackChunkPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SpeakerAudioManager.acceptTrackChunk(msg.ownerUuid, msg.trackId, msg.chunkIndex, msg.data)
        ));
        ctx.setPacketHandled(true);
    }
}
