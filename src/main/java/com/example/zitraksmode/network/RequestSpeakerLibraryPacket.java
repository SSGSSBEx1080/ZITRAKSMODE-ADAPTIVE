package com.example.zitraksmode.network;

import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class RequestSpeakerLibraryPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(RequestSpeakerLibraryPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestSpeakerLibraryPacket decode(FriendlyByteBuf buf) {
        return new RequestSpeakerLibraryPacket();
    }

    public static void handle(RequestSpeakerLibraryPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
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
                JBLSpeakerNetwork.sendToPlayer(player, new SyncSpeakerLibraryPacket(storage.getLibrary(player.getUUID())));
            } catch (Exception e) {
                LOGGER.error("JBL: failed to sync library for {}", player.getGameProfile().getName(), e);
            }
        });
        ctx.setPacketHandled(true);
    }
}
