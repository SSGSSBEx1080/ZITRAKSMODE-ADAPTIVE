package com.example.zitraksmode.network;

import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SharedTrackFinishPacket {

    private final UUID ownerUuid;
    private final String trackId;

    public SharedTrackFinishPacket(UUID ownerUuid, String trackId) {
        this.ownerUuid = ownerUuid;
        this.trackId = trackId;
    }

    public static void encode(SharedTrackFinishPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.ownerUuid);
        buf.writeUtf(msg.trackId, 128);
    }

    public static SharedTrackFinishPacket decode(FriendlyByteBuf buf) {
        return new SharedTrackFinishPacket(buf.readUUID(), buf.readUtf(128));
    }

    public static void handle(SharedTrackFinishPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SpeakerAudioManager.finishTrackDownload(msg.ownerUuid, msg.trackId)
        ));
        ctx.setPacketHandled(true);
    }
}
