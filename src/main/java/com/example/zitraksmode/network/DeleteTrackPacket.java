package com.example.zitraksmode.network;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.items.JBLSpeakerItem;
import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteTrackPacket {

    private final String trackId;

    public DeleteTrackPacket(String trackId) {
        this.trackId = trackId;
    }

    public static void encode(DeleteTrackPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.trackId, 128);
    }

    public static DeleteTrackPacket decode(FriendlyByteBuf buf) {
        return new DeleteTrackPacket(buf.readUtf(128));
    }

    public static void handle(DeleteTrackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            try {
                SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
                storage.deleteTrack(player.getUUID(), msg.trackId);

                for (ItemStack stack : player.getInventory().items) {
                    stopIfCurrentTrack(stack, player, msg.trackId);
                }
                for (ItemStack stack : player.getInventory().offhand) {
                    stopIfCurrentTrack(stack, player, msg.trackId);
                }
                for (ItemStack stack : player.getInventory().armor) {
                    stopIfCurrentTrack(stack, player, msg.trackId);
                }

                JBLSpeakerNetwork.sendToPlayer(player, new SyncSpeakerLibraryPacket(storage.getLibrary(player.getUUID())));
            } catch (Exception ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void stopIfCurrentTrack(ItemStack stack, ServerPlayer player, String deletedTrackId) {
        if (!stack.is(ModItems.JBL_SPEAKER.get())) {
            return;
        }
        if (!JBLSpeakerItem.isPlaying(stack)) {
            return;
        }
        if (!deletedTrackId.equals(JBLSpeakerItem.getCurrentTrackId(stack))) {
            return;
        }

        JBLSpeakerItem.setPlaying(stack, false);
        JBLSpeakerItem.setPaused(stack, false);
        JBLSpeakerItem.setPlaybackPositionMs(stack, 0L);
        PacketHandler.sendToTrackingAndSelf(
                new SpeakerStatePacket(player.getId(), player.getUUID(), false, false, null, JBLSpeakerItem.getVolume(stack), player.level.getGameTime(), 0L),
                player
        );
    }
}
