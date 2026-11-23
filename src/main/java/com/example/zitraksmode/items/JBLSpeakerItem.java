package com.example.zitraksmode.items;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import com.example.zitraksmode.client.gui.JBLSpeakerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JBLSpeakerItem extends Item {

    private static final Map<UUID, Integer> listeningProgress = new HashMap<>();

    public JBLSpeakerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new JBLSpeakerScreen(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean active = isPlaying(stack) || SpeakerAudioManager.isHudVisible();
        boolean paused = isPaused(stack);

        String track = getCurrentTrackTitle(stack);
        if (track == null || track.isEmpty()) {
            track = getCurrentTrackId(stack);
        }
        if ((track == null || track.isEmpty()) && SpeakerAudioManager.isHudVisible()) {
            track = SpeakerAudioManager.getHudTrackName();
        }

        if (active) {
            tooltip.add(Component.literal(paused ? "§e⏸ На паузе" : "§a▶ Играет"));
            if (track != null && !track.isEmpty()) {
                tooltip.add(Component.literal("§7♫ " + track));
            }
        } else {
            tooltip.add(Component.literal("§7⏹ Остановлено"));
            tooltip.add(Component.literal("§eПКМ → Открыть плеер"));
        }

        float volumeValue = SpeakerAudioManager.isHudVisible() ? SpeakerAudioManager.getHudVolume() : getVolume(stack);
        tooltip.add(Component.literal("§7Громкость: " + (int) (volumeValue * 100.0F) + "%"));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    public static boolean isPlaying(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean("Playing");
    }

    public static void setPlaying(ItemStack stack, boolean playing) {
        stack.getOrCreateTag().putBoolean("Playing", playing);
    }

    public static boolean isPaused(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean("Paused");
    }

    public static void setPaused(ItemStack stack, boolean paused) {
        stack.getOrCreateTag().putBoolean("Paused", paused);
    }

    public static String getCurrentTrackId(ItemStack stack) {
        return stack.getOrCreateTag().getString("CurrentTrackId");
    }

    public static String getCurrentTrack(ItemStack stack) {
        return getCurrentTrackId(stack);
    }

    public static void setCurrentTrackId(ItemStack stack, String trackId) {
        stack.getOrCreateTag().putString("CurrentTrackId", trackId == null ? "" : trackId);
    }

    public static void setCurrentTrack(ItemStack stack, String trackId) {
        setCurrentTrackId(stack, trackId);
    }

    public static String getCurrentTrackTitle(ItemStack stack) {
        return stack.getOrCreateTag().getString("CurrentTrackTitle");
    }

    public static void setCurrentTrackTitle(ItemStack stack, String title) {
        stack.getOrCreateTag().putString("CurrentTrackTitle", title == null ? "" : title);
    }

    public static String getCurrentTrackAuthor(ItemStack stack) {
        return stack.getOrCreateTag().getString("CurrentTrackAuthor");
    }

    public static void setCurrentTrackAuthor(ItemStack stack, String author) {
        stack.getOrCreateTag().putString("CurrentTrackAuthor", author == null ? "" : author);
    }

    public static float getVolume(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains("Volume")) {
            nbt.putFloat("Volume", 1.0F);
        }
        return nbt.getFloat("Volume");
    }

    public static void setVolume(ItemStack stack, float volume) {
        stack.getOrCreateTag().putFloat("Volume", Math.max(0.0F, Math.min(1.0F, volume)));
    }

    public static long getPlaybackStartGameTime(ItemStack stack) {
        return stack.getOrCreateTag().getLong("PlaybackStartGameTime");
    }

    public static void setPlaybackStartGameTime(ItemStack stack, long gameTime) {
        stack.getOrCreateTag().putLong("PlaybackStartGameTime", Math.max(0L, gameTime));
    }

    public static long getPlaybackDurationMs(ItemStack stack) {
        return stack.getOrCreateTag().getLong("PlaybackDurationMs");
    }

    public static void setPlaybackDurationMs(ItemStack stack, long durationMs) {
        stack.getOrCreateTag().putLong("PlaybackDurationMs", Math.max(0L, durationMs));
    }

    public static long getPlaybackPositionMs(ItemStack stack) {
        return stack.getOrCreateTag().getLong("PlaybackPositionMs");
    }

    public static void setPlaybackPositionMs(ItemStack stack, long positionMs) {
        stack.getOrCreateTag().putLong("PlaybackPositionMs", Math.max(0L, positionMs));
    }

    private static void checkAchievementProgress(Level level, Player player, ItemStack stack) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean active = isPlaying(stack) && !isPaused(stack);
        float volume = getVolume(stack);

        if (active && volume >= 1.0F) {
            UUID playerId = player.getUUID();
            int time = listeningProgress.getOrDefault(playerId, 0) + 1;
            listeningProgress.put(playerId, time);

            if (time >= 300) {
                ZitraksMode.JBL_FULL_VOLUME.trigger(serverPlayer);
                listeningProgress.remove(playerId);
            }
        } else {
            listeningProgress.remove(player.getUUID());
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player) {
            checkAchievementProgress(level, player, stack);
        }
    }

    public static void clearProgress(Player player) {
        if (player != null) {
            listeningProgress.remove(player.getUUID());
        }
    }
}
