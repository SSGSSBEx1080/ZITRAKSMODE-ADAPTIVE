package com.example.zitraksmode.network;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.printer.Printer3DMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPrinterScrollPacket {

    private final BlockPos pos;
    private final int scrollRow;

    public SyncPrinterScrollPacket(BlockPos pos, int scrollRow) {
        this.pos = pos;
        this.scrollRow = scrollRow;
    }

    public static void encode(SyncPrinterScrollPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.scrollRow);
    }

    public static SyncPrinterScrollPacket decode(FriendlyByteBuf buf) {
        return new SyncPrinterScrollPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(SyncPrinterScrollPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var be = player.level.getBlockEntity(packet.pos);
            if (be instanceof Printer3DBlockEntity) {
                if (player.containerMenu instanceof Printer3DMenu menu) {
                    menu.setPrinterScrollRowServer(packet.scrollRow);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}