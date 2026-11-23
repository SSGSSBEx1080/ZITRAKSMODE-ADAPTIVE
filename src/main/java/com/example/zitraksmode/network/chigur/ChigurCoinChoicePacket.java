package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.entities.AntonChigurEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends the selected side of the coin. The entity validates the active sequence. */
public record ChigurCoinChoicePacket(int entityId, boolean heads) {
    static void encode(ChigurCoinChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.heads);
    }

    static ChigurCoinChoicePacket decode(FriendlyByteBuf buffer) {
        return new ChigurCoinChoicePacket(buffer.readVarInt(), buffer.readBoolean());
    }

    static void handle(ChigurCoinChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;
            if (context.getSender().level.getEntity(packet.entityId) instanceof AntonChigurEntity chigur) {
                chigur.handleCoinChoice(context.getSender(), packet.heads);
            }
        });
        context.setPacketHandled(true);
    }
}
