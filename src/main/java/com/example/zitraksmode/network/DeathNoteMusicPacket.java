package com.example.zitraksmode.network;

import com.example.zitraksmode.client.audio.DeathNoteMusicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeathNoteMusicPacket {

    private final boolean play;

    public DeathNoteMusicPacket(boolean play) {
        this.play = play;
    }

    public static void encode(DeathNoteMusicPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.play);
    }

    public static DeathNoteMusicPacket decode(FriendlyByteBuf buf) {
        return new DeathNoteMusicPacket(buf.readBoolean());
    }

    public static void handle(DeathNoteMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(DeathNoteMusicPacket msg) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        if (msg.play) {
            DeathNoteMusicManager.playDeathNoteMusic(Minecraft.getInstance().player.getUUID());
        } else {
            DeathNoteMusicManager.stopMusic(Minecraft.getInstance().player.getUUID());
        }
    }
}