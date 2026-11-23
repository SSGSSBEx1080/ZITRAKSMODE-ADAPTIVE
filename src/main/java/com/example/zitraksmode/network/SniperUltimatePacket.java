package com.example.zitraksmode.network;

import com.example.zitraksmode.items.SniperItem;
import com.example.zitraksmode.weapons.ModToolTiers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SniperUltimatePacket {

    public SniperUltimatePacket() {
    }

    public SniperUltimatePacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (tryActivate(player, InteractionHand.MAIN_HAND)) {
                return;
            }

            tryActivate(player, InteractionHand.OFF_HAND);
        });

        context.setPacketHandled(true);
        return true;
    }

    private static boolean tryActivate(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SniperItem sniperItem)) {
            return false;
        }

        if (sniperItem.getTier() != ModToolTiers.SNIPER_NETHERITE) {
            return false;
        }

        sniperItem.activateUltimate(player, hand);
        return true;
    }
}