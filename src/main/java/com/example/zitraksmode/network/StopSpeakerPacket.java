package com.example.zitraksmode.network;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.items.JBLSpeakerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopSpeakerPacket {

    private final InteractionHand hand;

    public StopSpeakerPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public static void encode(StopSpeakerPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
    }

    public static StopSpeakerPacket decode(FriendlyByteBuf buf) {
        return new StopSpeakerPacket(buf.readEnum(InteractionHand.class));
    }

    public static void handle(StopSpeakerPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(msg.hand);
            if (!stack.is(ModItems.JBL_SPEAKER.get())) {
                return;
            }

            JBLSpeakerItem.setPlaying(stack, false);
            JBLSpeakerItem.setPaused(stack, false);
            JBLSpeakerItem.setPlaybackPositionMs(stack, 0L);

            PacketHandler.sendToTrackingAndSelf(
                    new SpeakerStatePacket(player.getId(), player.getUUID(), false, false, null, JBLSpeakerItem.getVolume(stack), player.level.getGameTime(), 0L),
                    player
            );
        });
        ctx.setPacketHandled(true);
    }
}
