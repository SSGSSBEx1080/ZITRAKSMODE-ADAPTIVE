package com.example.zitraksmode.network.mih;

import com.example.zitraksmode.ZitraksMode;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopMadeInHeavenSoundPacket {

    public StopMadeInHeavenSoundPacket() {
    }

    public static void encode(StopMadeInHeavenSoundPacket msg, FriendlyByteBuf buf) {
    }

    public static StopMadeInHeavenSoundPacket decode(FriendlyByteBuf buf) {
        return new StopMadeInHeavenSoundPacket();
    }

    public static void handle(StopMadeInHeavenSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.getSoundManager().stop(new ResourceLocation(ZitraksMode.MODID, "made_in_heaven"), SoundSource.MASTER);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}