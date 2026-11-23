package com.example.zitraksmode.network.labubu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClaimLabubuCaseRewardPacket {

    public ClaimLabubuCaseRewardPacket() {
    }

    public static void encode(ClaimLabubuCaseRewardPacket msg, FriendlyByteBuf buf) {
    }

    public static ClaimLabubuCaseRewardPacket decode(FriendlyByteBuf buf) {
        return new ClaimLabubuCaseRewardPacket();
    }

    public static void handle(ClaimLabubuCaseRewardPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                LabubuCaseManager.claimReward(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}