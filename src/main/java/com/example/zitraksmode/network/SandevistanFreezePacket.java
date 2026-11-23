package com.example.zitraksmode.network;

import com.example.zitraksmode.events.SandevistanClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SandevistanFreezePacket {
    private final boolean freeze;

    public SandevistanFreezePacket(boolean freeze) {
        this.freeze = freeze;
    }

    public SandevistanFreezePacket(FriendlyByteBuf buf) {
        this.freeze = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.freeze);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            SandevistanClientHandler.setTimeStopped(this.freeze);
        });
        context.setPacketHandled(true);
    }
}