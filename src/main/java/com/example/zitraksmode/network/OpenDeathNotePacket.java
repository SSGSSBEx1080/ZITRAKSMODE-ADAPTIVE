package com.example.zitraksmode.network;

import com.example.zitraksmode.client.screen.DeathNoteScreen;
import com.example.zitraksmode.client.screen.DeathNoteRequiemScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDeathNotePacket {
    private final ItemStack stack;
    private final boolean isRequiem;

    public OpenDeathNotePacket(ItemStack stack, boolean isRequiem) {
        this.stack = stack;
        this.isRequiem = isRequiem;
    }

    public static void encode(OpenDeathNotePacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
        buf.writeBoolean(msg.isRequiem);
    }

    public static OpenDeathNotePacket decode(FriendlyByteBuf buf) {
        return new OpenDeathNotePacket(buf.readItem(), buf.readBoolean());
    }

    public static void handle(OpenDeathNotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            System.out.println("[DEBUG] Клиент получил пакет OpenDeathNotePacket! isRequiem=" + msg.isRequiem);
            if (msg.isRequiem) {
                Minecraft.getInstance().setScreen(new DeathNoteRequiemScreen(msg.stack));
            } else {
                Minecraft.getInstance().setScreen(new DeathNoteScreen(msg.stack));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}