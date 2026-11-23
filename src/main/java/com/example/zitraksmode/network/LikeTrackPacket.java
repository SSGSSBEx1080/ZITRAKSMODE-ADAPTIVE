package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LikeTrackPacket {

    private final String trackId;

    public LikeTrackPacket(String trackId) {
        this.trackId = trackId;
    }

    public static void encode(LikeTrackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.trackId, 128);
    }

    public static LikeTrackPacket decode(FriendlyByteBuf buf) {
        return new LikeTrackPacket(buf.readUtf(128));
    }

    public static void handle(LikeTrackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                storage.incrementLike(player.getUUID(), msg.trackId);
                JBLSpeakerNetwork.sendToPlayer(player, new SyncSpeakerLibraryPacket(storage.getLibrary(player.getUUID())));
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
