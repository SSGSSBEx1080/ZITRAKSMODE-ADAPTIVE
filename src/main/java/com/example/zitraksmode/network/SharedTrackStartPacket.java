package com.example.zitraksmode.network;

import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SharedTrackStartPacket {

    private final SpeakerTrackMeta meta;
    private final int totalSize;
    private final int totalChunks;

    public SharedTrackStartPacket(SpeakerTrackMeta meta, int totalSize, int totalChunks) {
        this.meta = meta;
        this.totalSize = totalSize;
        this.totalChunks = totalChunks;
    }

    public static void encode(SharedTrackStartPacket msg, FriendlyByteBuf buf) {
        msg.meta.write(buf);
        buf.writeVarInt(msg.totalSize);
        buf.writeVarInt(msg.totalChunks);
    }

    public static SharedTrackStartPacket decode(FriendlyByteBuf buf) {
        return new SharedTrackStartPacket(
                SpeakerTrackMeta.read(buf),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(SharedTrackStartPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SpeakerAudioManager.beginTrackDownload(msg.meta, msg.totalChunks, msg.totalSize)
        ));
        ctx.setPacketHandled(true);
    }
}
