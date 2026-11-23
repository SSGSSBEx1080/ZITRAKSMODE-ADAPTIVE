package com.example.zitraksmode.network;

import com.example.zitraksmode.client.MedusaWaveClientRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Starts one client-only rendered shell at the exact server-side wave origin. */
public record MedusaWavePacket(double x, double y, double z, int radius) {
    static void encode(MedusaWavePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.radius);
    }

    static MedusaWavePacket decode(FriendlyByteBuf buffer) {
        return new MedusaWavePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt()
        );
    }

    static void handle(MedusaWavePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MedusaWaveClientRenderer.start(packet.x, packet.y, packet.z, packet.radius)));
        context.setPacketHandled(true);
    }
}
