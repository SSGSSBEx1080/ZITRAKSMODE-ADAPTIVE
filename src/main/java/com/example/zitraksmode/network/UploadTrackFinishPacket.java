package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public class UploadTrackFinishPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final UUID sessionId;

    public UploadTrackFinishPacket(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public static void encode(UploadTrackFinishPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.sessionId);
    }

    public static UploadTrackFinishPacket decode(FriendlyByteBuf buf) {
        return new UploadTrackFinishPacket(buf.readUUID());
    }

    public static void handle(UploadTrackFinishPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
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
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                storage.finishUpload(player, msg.sessionId);
                JBLSpeakerNetwork.sendToPlayer(player, new SyncSpeakerLibraryPacket(storage.getLibrary(player.getUUID())));
            } catch (Exception e) {
                LOGGER.error("JBL: failed to finish upload for {}", player.getGameProfile().getName(), e);
            }
        });
        ctx.setPacketHandled(true);
    }
}
