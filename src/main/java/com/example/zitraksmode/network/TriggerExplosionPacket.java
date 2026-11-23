package com.example.zitraksmode.network;

import com.example.zitraksmode.abilities.AbilitySystem;
import com.example.zitraksmode.items.SniperItem;
import com.example.zitraksmode.weapons.ModToolTiers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class TriggerExplosionPacket {

    private static final Logger LOGGER = LogManager.getLogger();

    public TriggerExplosionPacket() {
    }

    public TriggerExplosionPacket(FriendlyByteBuf buf) {
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

            // Сначала пытаемся активировать ульту снайперки
            if (tryTriggerSniperUltimate(player)) {
                return;
            }

            // Если снайперка не подошла — обычная логика ТНТ-брони
            AbilitySystem.triggerExplosion(player);
        });

        context.setPacketHandled(true);
        return true;
    }

    private static boolean tryTriggerSniperUltimate(ServerPlayer player) {
        return tryActivateSniperInHand(player, InteractionHand.MAIN_HAND)
                || tryActivateSniperInHand(player, InteractionHand.OFF_HAND);
    }

    private static boolean tryActivateSniperInHand(ServerPlayer player, InteractionHand hand) {
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