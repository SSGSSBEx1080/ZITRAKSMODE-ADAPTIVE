package com.example.zitraksmode.network;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.items.JBLSpeakerItem;
import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PauseSpeakerPacket {

    private final InteractionHand hand;
    private final boolean paused;

    public PauseSpeakerPacket(InteractionHand hand, boolean paused) {
        this.hand = hand;
        this.paused = paused;
    }

    public static void encode(PauseSpeakerPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeBoolean(msg.paused);
    }

    public static PauseSpeakerPacket decode(FriendlyByteBuf buf) {
        return new PauseSpeakerPacket(buf.readEnum(InteractionHand.class), buf.readBoolean());
    }

    public static void handle(PauseSpeakerPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(msg.hand);
            if (!stack.is(ModItems.JBL_SPEAKER.get()) || !JBLSpeakerItem.isPlaying(stack)) {
                return;
            }

            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                SpeakerTrackMeta meta = storage.getTrackMeta(player.getUUID(), JBLSpeakerItem.getCurrentTrackId(stack));
                if (meta == null) {
                    return;
                }

                long positionMs = JBLSpeakerItem.getPlaybackPositionMs(stack);
                long startGameTime = JBLSpeakerItem.getPlaybackStartGameTime(stack);

                if (msg.paused && !JBLSpeakerItem.isPaused(stack)) {
                    positionMs += Math.max(0L, (player.level.getGameTime() - startGameTime) * 50L);
                    JBLSpeakerItem.setPlaybackPositionMs(stack, positionMs);
                    JBLSpeakerItem.setPaused(stack, true);
                } else if (!msg.paused && JBLSpeakerItem.isPaused(stack)) {
                    JBLSpeakerItem.setPaused(stack, false);
                    JBLSpeakerItem.setPlaybackStartGameTime(stack, player.level.getGameTime());
                    startGameTime = player.level.getGameTime();
                }

                PacketHandler.sendToTrackingAndSelf(
                        new SpeakerStatePacket(
                                player.getId(),
                                player.getUUID(),
                                true,
                                JBLSpeakerItem.isPaused(stack),
                                meta,
                                JBLSpeakerItem.getVolume(stack),
                                JBLSpeakerItem.getPlaybackStartGameTime(stack),
                                JBLSpeakerItem.getPlaybackPositionMs(stack)
                        ),
                        player
                );
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
