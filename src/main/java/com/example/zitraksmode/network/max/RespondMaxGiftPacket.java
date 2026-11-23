package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxGiftOffer;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class RespondMaxGiftPacket {

    private final UUID giftId;
    private final boolean accept;

    public RespondMaxGiftPacket(UUID giftId, boolean accept) {
        this.giftId = giftId;
        this.accept = accept;
    }

    public static void encode(RespondMaxGiftPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.giftId);
        buf.writeBoolean(packet.accept);
    }

    public static RespondMaxGiftPacket decode(FriendlyByteBuf buf) {
        return new RespondMaxGiftPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(RespondMaxGiftPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer actor = ctx.get().getSender();
            if (actor == null || actor.getServer() == null) {
                return;
            }
            MaxMessengerSavedData data = MaxMessengerSavedData.get(actor.getServer());
            MaxGiftOffer offer = data.getGiftsForUser(actor.getUUID()).stream()
                    .filter(g -> g.getGiftId().equals(packet.giftId))
                    .findFirst()
                    .orElse(null);
            if (offer == null) {
                return;
            }
            data.respondToGift(actor, packet.giftId, packet.accept);
            MaxSyncHelper.syncPair(actor.getServer(), data, offer.getSenderId(), offer.getRecipientId());
        });
        ctx.get().setPacketHandled(true);
    }
}
