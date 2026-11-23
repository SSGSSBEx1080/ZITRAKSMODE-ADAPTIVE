package com.example.zitraksmode.network;

import com.example.zitraksmode.client.PetrificationClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes the temporary stone texture state for any living entity. */
public record PetrificationStatePacket(int entityId, int remainingTicks) {
    static void encode(PetrificationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.remainingTicks);
    }

    static PetrificationStatePacket decode(FriendlyByteBuf buffer) {
        return new PetrificationStatePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    static void handle(PetrificationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PetrificationClientState.set(packet.entityId, packet.remainingTicks)));
        context.setPacketHandled(true);
    }
}
