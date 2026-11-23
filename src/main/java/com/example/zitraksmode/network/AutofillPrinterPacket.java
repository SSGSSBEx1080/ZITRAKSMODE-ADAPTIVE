package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AutofillPrinterPacket {

    private final BlockPos pos;

    public AutofillPrinterPacket(BlockPos pos) {
        this.pos = pos;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static AutofillPrinterPacket decode(FriendlyByteBuf buf) {
        return new AutofillPrinterPacket(buf.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            var be = player.level.getBlockEntity(pos);
            if (be instanceof Printer3DBlockEntity printer) {
                printer.autoFillFromPlayer(player);
            }
        });

        ctx.get().setPacketHandled(true);
    }
}