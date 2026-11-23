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

public class SelectTrackPacket {

    private final InteractionHand hand;
    private final String trackId;

    public SelectTrackPacket(InteractionHand hand, String trackId) {
        this.hand = hand;
        this.trackId = trackId;
    }

    public static void encode(SelectTrackPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeUtf(msg.trackId, 128);
    }

    public static SelectTrackPacket decode(FriendlyByteBuf buf) {
        return new SelectTrackPacket(buf.readEnum(InteractionHand.class), buf.readUtf(128));
    }

    public static void handle(SelectTrackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(msg.hand);
            if (!stack.is(ModItems.JBL_SPEAKER.get())) {
                return;
            }

            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                SpeakerTrackMeta meta = storage.getTrackMeta(player.getUUID(), msg.trackId);
                if (meta == null) {
                    return;
                }

                float volume = JBLSpeakerItem.getVolume(stack);
                long startGameTime = player.level.getGameTime();

                JBLSpeakerItem.setCurrentTrackId(stack, meta.getTrackId());
                JBLSpeakerItem.setCurrentTrackTitle(stack, meta.getTitle());
                JBLSpeakerItem.setCurrentTrackAuthor(stack, meta.getAuthor());
                JBLSpeakerItem.setPlaybackDurationMs(stack, meta.getDurationMs());
                JBLSpeakerItem.setPlaybackStartGameTime(stack, startGameTime);
                JBLSpeakerItem.setPlaybackPositionMs(stack, 0L);
                JBLSpeakerItem.setPaused(stack, false);
                JBLSpeakerItem.setPlaying(stack, true);

                PacketHandler.sendToTrackingAndSelf(
                        new SpeakerStatePacket(player.getId(), player.getUUID(), true, false, meta, volume, startGameTime, 0L),
                        player
                );
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
