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

public class VolumeChangePacket {

    private final InteractionHand hand;
    private final float volume;

    public VolumeChangePacket(InteractionHand hand, float volume) {
        this.hand = hand;
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));
    }

    public static void encode(VolumeChangePacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeFloat(msg.volume);
    }

    public static VolumeChangePacket decode(FriendlyByteBuf buf) {
        return new VolumeChangePacket(buf.readEnum(InteractionHand.class), buf.readFloat());
    }

    public static void handle(VolumeChangePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
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

            JBLSpeakerItem.setVolume(stack, msg.volume);

            if (!JBLSpeakerItem.isPlaying(stack) || player.getServer() == null) {
                return;
            }

            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                SpeakerTrackMeta meta = storage.getTrackMeta(player.getUUID(), JBLSpeakerItem.getCurrentTrackId(stack));
                if (meta != null) {
                    PacketHandler.sendToTrackingAndSelf(
                            new SpeakerStatePacket(
                                    player.getId(),
                                    player.getUUID(),
                                    true,
                                    JBLSpeakerItem.isPaused(stack),
                                    meta,
                                    msg.volume,
                                    JBLSpeakerItem.getPlaybackStartGameTime(stack),
                                    JBLSpeakerItem.getPlaybackPositionMs(stack)
                            ),
                            player
                    );
                }
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
