package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.JBLSpeakerItem;
import com.example.zitraksmode.jbl.SpeakerTrackMeta;
import com.example.zitraksmode.jbl.SpeakerTrackStorage;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.SpeakerStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpeakerEventHandler {

    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        ItemStack activeSpeaker = findActiveSpeaker(player.getInventory());
        if (activeSpeaker.isEmpty()) {
            if (ACTIVE_PLAYERS.remove(player.getUUID())) {
                PacketHandler.sendToTrackingAndSelf(
                        new SpeakerStatePacket(player.getId(), player.getUUID(), false, false, null, 1.0F, player.level.getGameTime(), 0L),
                        player
                );
            }
            return;
        }

        ACTIVE_PLAYERS.add(player.getUUID());

        try {
            SpeakerTrackStorage storage = SpeakerTrackStorage.forServer(player.getServer());
            SpeakerTrackMeta meta = storage.getTrackMeta(player.getUUID(), JBLSpeakerItem.getCurrentTrackId(activeSpeaker));
            if (meta == null) {
                JBLSpeakerItem.setPlaying(activeSpeaker, false);
                JBLSpeakerItem.setPaused(activeSpeaker, false);
                PacketHandler.sendToTrackingAndSelf(
                        new SpeakerStatePacket(player.getId(), player.getUUID(), false, false, null, JBLSpeakerItem.getVolume(activeSpeaker), player.level.getGameTime(), 0L),
                        player
                );
                ACTIVE_PLAYERS.remove(player.getUUID());
                return;
            }

            long positionMs = JBLSpeakerItem.getPlaybackPositionMs(activeSpeaker);
            boolean paused = JBLSpeakerItem.isPaused(activeSpeaker);

            if (!paused) {
                positionMs += Math.max(0L, (player.level.getGameTime() - JBLSpeakerItem.getPlaybackStartGameTime(activeSpeaker)) * 50L);
                if (meta.getDurationMs() > 0L && positionMs >= meta.getDurationMs()) {
                    JBLSpeakerItem.setPlaying(activeSpeaker, false);
                    JBLSpeakerItem.setPaused(activeSpeaker, false);
                    JBLSpeakerItem.setPlaybackPositionMs(activeSpeaker, 0L);
                    PacketHandler.sendToTrackingAndSelf(
                            new SpeakerStatePacket(player.getId(), player.getUUID(), false, false, null, JBLSpeakerItem.getVolume(activeSpeaker), player.level.getGameTime(), 0L),
                            player
                    );
                    ACTIVE_PLAYERS.remove(player.getUUID());
                    return;
                }
            }

            if (player.level.getGameTime() % 10L == 0L) {
                PacketHandler.sendToTrackingAndSelf(
                        new SpeakerStatePacket(
                                player.getId(),
                                player.getUUID(),
                                true,
                                paused,
                                meta,
                                JBLSpeakerItem.getVolume(activeSpeaker),
                                JBLSpeakerItem.getPlaybackStartGameTime(activeSpeaker),
                                JBLSpeakerItem.getPlaybackPositionMs(activeSpeaker)
                        ),
                        player
                );
            }
        } catch (Exception ignored) {
        }
    }

    private static ItemStack findActiveSpeaker(Inventory inventory) {
        for (ItemStack stack : inventory.items) {
            if (isActiveSpeaker(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (isActiveSpeaker(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.armor) {
            if (isActiveSpeaker(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isActiveSpeaker(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.JBL_SPEAKER.get()) && JBLSpeakerItem.isPlaying(stack);
    }
}
