package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RenameTrackPacket {

    private final String trackId;
    private final String newTitle;

    public RenameTrackPacket(String trackId, String newTitle) {
        this.trackId = trackId;
        this.newTitle = newTitle == null ? "" : newTitle.trim();
    }

    public static void encode(RenameTrackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.trackId, 128);
        buf.writeUtf(msg.newTitle, 128);
    }

    public static RenameTrackPacket decode(FriendlyByteBuf buf) {
        return new RenameTrackPacket(buf.readUtf(128), buf.readUtf(128));
    }

    public static void handle(RenameTrackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.getServer() == null || msg.newTitle.isBlank()) {
                return;
            }
            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                storage.renameTrack(player.getUUID(), msg.trackId, msg.newTitle);
                JBLSpeakerNetwork.sendToPlayer(player, new SyncSpeakerLibraryPacket(storage.getLibrary(player.getUUID())));
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
