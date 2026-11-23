package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RegisterMaxProfilePacket {

    private final String displayName;
    private final String description;
    private final String avatarId;
    private final String showcaseItemId;

    public RegisterMaxProfilePacket(String displayName, String description, String avatarId, String showcaseItemId) {
        this.displayName = displayName;
        this.description = description;
        this.avatarId = avatarId;
        this.showcaseItemId = showcaseItemId;
    }

    public static void encode(RegisterMaxProfilePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.displayName, 64);
        buf.writeUtf(packet.description, 256);
        buf.writeUtf(packet.avatarId, 32767);
        buf.writeUtf(packet.showcaseItemId, 256);
    }

    public static RegisterMaxProfilePacket decode(FriendlyByteBuf buf) {
        return new RegisterMaxProfilePacket(
                buf.readUtf(64),
                buf.readUtf(256),
                buf.readUtf(32767),
                buf.readUtf(256)
        );
    }

    public static void handle(RegisterMaxProfilePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.getServer() == null) {
                return;
            }

            MaxMessengerSavedData data = MaxMessengerSavedData.get(player.getServer());
            data.ensureOnlineProfiles(player.getServer());
            data.updateProfile(player, packet.displayName, packet.description, packet.avatarId, packet.showcaseItemId);
            MaxPackets.sendToPlayer(SyncMaxStatePacket.fromServerState(player, data), player);
        });
        ctx.get().setPacketHandled(true);
    }
}