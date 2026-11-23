package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.client.gui.ChigurCoinScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Opens the heads/tails screen for the player selected by the server. */
public record ChigurCoinOpenPacket(int entityId) {
    static void encode(ChigurCoinOpenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    static ChigurCoinOpenPacket decode(FriendlyByteBuf buffer) {
        return new ChigurCoinOpenPacket(buffer.readVarInt());
    }

    static void handle(ChigurCoinOpenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new ChigurCoinScreen(packet.entityId))));
        context.setPacketHandled(true);
    }
}
