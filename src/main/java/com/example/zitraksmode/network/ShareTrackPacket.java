package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ShareTrackPacket {

    private final String trackId;
    private final UUID targetUuid;

    public ShareTrackPacket(String trackId, UUID targetUuid) {
        this.trackId = trackId;
        this.targetUuid = targetUuid;
    }

    public static void encode(ShareTrackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.trackId, 128);
        buf.writeUUID(msg.targetUuid);
    }

    public static ShareTrackPacket decode(FriendlyByteBuf buf) {
        return new ShareTrackPacket(buf.readUtf(128), buf.readUUID());
    }

    public static void handle(ShareTrackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }

            ServerPlayer target = sender.getServer().getPlayerList().getPlayer(msg.targetUuid);
            if (target == null) {
                return;
            }

            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(sender.getServer());
                SpeakerTrackMeta meta = storage.getTrackMeta(sender.getUUID(), msg.trackId);
                if (meta == null) {
                    return;
                }

                byte[] bytes = storage.readTrackBytes(sender.getUUID(), msg.trackId);
                storage.saveTrackForPlayer(target.getUUID(), meta.getFileName(), meta.getTitle(), meta.getAuthor(), meta.getDurationMs(), bytes);
                JBLSpeakerNetwork.sendToPlayer(target, new SyncSpeakerLibraryPacket(storage.getLibrary(target.getUUID())));
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
