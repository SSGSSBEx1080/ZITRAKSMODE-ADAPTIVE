package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.client.gui.ChigurCinematicOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Starts the client-only black overlay used by the bed scene. */
public record ChigurCinematicPacket(int ticks) {
    static void encode(ChigurCinematicPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ticks);
    }

    static ChigurCinematicPacket decode(FriendlyByteBuf buffer) {
        return new ChigurCinematicPacket(buffer.readVarInt());
    }

    static void handle(ChigurCinematicPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ChigurCinematicOverlay.start(packet.ticks)));
        context.setPacketHandled(true);
    }
}
