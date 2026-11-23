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

public class SeekSpeakerPacket {

    private final InteractionHand hand;
    private final long positionMs;

    public SeekSpeakerPacket(InteractionHand hand, long positionMs) {
        this.hand = hand;
        this.positionMs = Math.max(0L, positionMs);
    }

    public static void encode(SeekSpeakerPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeVarLong(msg.positionMs);
    }

    public static SeekSpeakerPacket decode(FriendlyByteBuf buf) {
        return new SeekSpeakerPacket(buf.readEnum(InteractionHand.class), buf.readVarLong());
    }

    public static void handle(SeekSpeakerPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
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

                long clampedPosition = meta.getDurationMs() > 0L ? Math.min(msg.positionMs, meta.getDurationMs()) : msg.positionMs;
                JBLSpeakerItem.setPlaybackPositionMs(stack, clampedPosition);
                if (!JBLSpeakerItem.isPaused(stack)) {
                    JBLSpeakerItem.setPlaybackStartGameTime(stack, player.level.getGameTime());
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
