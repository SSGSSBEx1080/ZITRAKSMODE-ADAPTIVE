package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.client.gui.ChigurWitnessScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Opens the client-only witness question after Chigur finishes speech 4. */
public record ChigurWitnessOpenPacket(int entityId, String huntedName, String refusalText) {
    static void encode(ChigurWitnessOpenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUtf(packet.huntedName, 128);
        buffer.writeUtf(packet.refusalText, 256);
    }

    static ChigurWitnessOpenPacket decode(FriendlyByteBuf buffer) {
        return new ChigurWitnessOpenPacket(
                buffer.readVarInt(),
                buffer.readUtf(128),
                buffer.readUtf(256)
        );
    }

    static void handle(ChigurWitnessOpenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(
                        new ChigurWitnessScreen(packet.entityId, packet.huntedName, packet.refusalText)
                )));
        context.setPacketHandled(true);
    }
}
