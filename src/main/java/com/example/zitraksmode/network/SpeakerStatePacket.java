package com.example.zitraksmode.network;

import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SpeakerStatePacket {

    private final int ownerEntityId;
    private final UUID ownerUuid;
    private final boolean active;
    private final boolean paused;
    private final SpeakerTrackMeta trackMeta;
    private final float volume;
    private final long startGameTime;
    private final long positionMs;

    public SpeakerStatePacket(int ownerEntityId,
                              UUID ownerUuid,
                              boolean active,
                              boolean paused,
                              SpeakerTrackMeta trackMeta,
                              float volume,
                              long startGameTime,
                              long positionMs) {
        this.ownerEntityId = ownerEntityId;
        this.ownerUuid = ownerUuid;
        this.active = active;
        this.paused = paused;
        this.trackMeta = trackMeta;
        this.volume = volume;
        this.startGameTime = startGameTime;
        this.positionMs = positionMs;
    }

    public static void encode(SpeakerStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ownerEntityId);
        buf.writeUUID(msg.ownerUuid);
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.paused);
        buf.writeBoolean(msg.trackMeta != null);
        if (msg.trackMeta != null) {
            msg.trackMeta.write(buf);
        }
        buf.writeFloat(msg.volume);
        buf.writeVarLong(msg.startGameTime);
        buf.writeVarLong(msg.positionMs);
    }

    public static SpeakerStatePacket decode(FriendlyByteBuf buf) {
        int ownerEntityId = buf.readInt();
        UUID ownerUuid = buf.readUUID();
        boolean active = buf.readBoolean();
        boolean paused = buf.readBoolean();
        SpeakerTrackMeta meta = buf.readBoolean() ? SpeakerTrackMeta.read(buf) : null;
        float volume = buf.readFloat();
        long startGameTime = buf.readVarLong();
        long positionMs = buf.readVarLong();
        return new SpeakerStatePacket(ownerEntityId, ownerUuid, active, paused, meta, volume, startGameTime, positionMs);
    }

    public static void handle(SpeakerStatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SpeakerAudioManager.handleSpeakerState(
                        msg.ownerEntityId,
                        msg.ownerUuid,
                        msg.active,
                        msg.paused,
                        msg.trackMeta,
                        msg.volume,
                        msg.startGameTime,
                        msg.positionMs
                )
        ));
        ctx.setPacketHandled(true);
    }
}
