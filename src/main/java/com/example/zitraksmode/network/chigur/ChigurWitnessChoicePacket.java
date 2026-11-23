package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.entities.AntonChigurEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server receives the witness answer; entity validates both player and active scene. */
public record ChigurWitnessChoicePacket(int entityId, int choice) {
    static void encode(ChigurWitnessChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.choice);
    }

    static ChigurWitnessChoicePacket decode(FriendlyByteBuf buffer) {
        return new ChigurWitnessChoicePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    static void handle(ChigurWitnessChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;
            if (context.getSender().level.getEntity(packet.entityId) instanceof AntonChigurEntity chigur) {
                chigur.handleWitnessChoice(context.getSender(), packet.choice);
            }
        });
        context.setPacketHandled(true);
    }
}
