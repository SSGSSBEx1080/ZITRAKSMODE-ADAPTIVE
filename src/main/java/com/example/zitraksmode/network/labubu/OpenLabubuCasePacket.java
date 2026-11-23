package com.example.zitraksmode.network.labubu;

import com.example.zitraksmode.client.gui.screens.LabubuCaseScreen;
import com.example.zitraksmode.items.LabubuType;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenLabubuCasePacket {

    private final List<LabubuType> sequence;
    private final int winnerIndex;

    public OpenLabubuCasePacket(List<LabubuType> sequence, int winnerIndex) {
        this.sequence = sequence;
        this.winnerIndex = winnerIndex;
    }

    public static void encode(OpenLabubuCasePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.sequence.size());
        for (LabubuType type : msg.sequence) {
            buf.writeVarInt(type.ordinal());
        }
        buf.writeVarInt(msg.winnerIndex);
    }

    public static OpenLabubuCasePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<LabubuType> sequence = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int ordinal = buf.readVarInt();
            sequence.add(LabubuType.values()[ordinal]);
        }

        int winnerIndex = buf.readVarInt();
        return new OpenLabubuCasePacket(sequence, winnerIndex);
    }

    public static void handle(OpenLabubuCasePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new LabubuCaseScreen(msg.sequence, msg.winnerIndex));
        });
        ctx.get().setPacketHandled(true);
    }
}