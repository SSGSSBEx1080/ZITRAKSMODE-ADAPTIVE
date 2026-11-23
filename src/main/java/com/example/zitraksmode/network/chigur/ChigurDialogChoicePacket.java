package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.entities.AntonChigurEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends a selected normal dialogue option to the server. */
public record ChigurDialogChoicePacket(int entityId, int choice) {
    static void encode(ChigurDialogChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.choice);
    }

    static ChigurDialogChoicePacket decode(FriendlyByteBuf buffer) {
        return new ChigurDialogChoicePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    static void handle(ChigurDialogChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;
            if (context.getSender().level.getEntity(packet.entityId) instanceof AntonChigurEntity chigur) {
                chigur.handleDialogChoice(context.getSender(), packet.choice);
            }
        });
        context.setPacketHandled(true);
    }
}
