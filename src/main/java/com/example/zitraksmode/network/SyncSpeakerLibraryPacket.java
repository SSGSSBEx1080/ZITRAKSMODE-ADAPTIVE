package com.example.zitraksmode.network;

import com.example.zitraksmode.client.audio.MusicTrackManager;
import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncSpeakerLibraryPacket {

    private final List<SpeakerTrackMeta> tracks;

    public SyncSpeakerLibraryPacket(List<SpeakerTrackMeta> tracks) {
        this.tracks = tracks;
    }

    public static void encode(SyncSpeakerLibraryPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.tracks.size());
        for (SpeakerTrackMeta track : msg.tracks) {
            track.write(buf);
        }
    }

    public static SyncSpeakerLibraryPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SpeakerTrackMeta> tracks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tracks.add(SpeakerTrackMeta.read(buf));
        }
        return new SyncSpeakerLibraryPacket(tracks);
    }

    public static void handle(SyncSpeakerLibraryPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                MusicTrackManager.getInstance().replaceLibrary(msg.tracks)
        ));
        ctx.setPacketHandled(true);
    }
}
