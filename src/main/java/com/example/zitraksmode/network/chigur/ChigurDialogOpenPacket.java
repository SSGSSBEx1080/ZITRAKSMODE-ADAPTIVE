package com.example.zitraksmode.network.chigur;

import com.example.zitraksmode.client.gui.ChigurDialogScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Opens the normal two-line dialogue on the client. */
public record ChigurDialogOpenPacket(int entityId, int aggression) {
    static void encode(ChigurDialogOpenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.aggression);
    }

    static ChigurDialogOpenPacket decode(FriendlyByteBuf buffer) {
        return new ChigurDialogOpenPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    static void handle(ChigurDialogOpenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new ChigurDialogScreen(packet.entityId))));
        context.setPacketHandled(true);
    }
}
