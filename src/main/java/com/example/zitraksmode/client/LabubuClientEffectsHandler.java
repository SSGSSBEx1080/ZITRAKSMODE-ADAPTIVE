package com.example.zitraksmode.client;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LabubuType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LabubuClientEffectsHandler {

    private static final int DEFAULT_AURA_RADIUS = 6;
    private static final int ANONYMOUS_AURA_RADIUS = 3;

    // === ПЬЯНАЯ КАМЕРА (Русский Лабубу) ===
    private static final float RUSSIAN_ROLL_AMPLITUDE = 6.0F;
    private static final float RUSSIAN_YAW_AMPLITUDE = 3.0F;
    private static final float RUSSIAN_PITCH_AMPLITUDE = 2.0F;
    private static final float RUSSIAN_FOV_AMPLITUDE = 0.08F;

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || player.level == null) {
            return;
        }

        int japaneseCount = countNearbyLabubu(player, LabubuType.JAPANESE, DEFAULT_AURA_RADIUS);
        if (japaneseCount > 0) {
            event.setFOV(event.getFOV() * 0.5D);
        }

        // Пульсация FOV от русского лабубу
        int russianCount = countNearbyLabubu(player, LabubuType.RUSSIAN, DEFAULT_AURA_RADIUS);
        if (russianCount > 0) {
            float intensity = Math.min(1.0F, russianCount * 0.35F);
            float t = (player.tickCount + (float) event.getPartialTick()) * 0.13F;
            double pulse = Math.sin(t) * RUSSIAN_FOV_AMPLITUDE * intensity;
            event.setFOV(event.getFOV() * (1.0D + pulse));
        }
    }

    // === Наклон камеры (roll/yaw/pitch) от русского лабубу ===
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || player.level == null) {
            return;
        }

        int russianCount = countNearbyLabubu(player, LabubuType.RUSSIAN, DEFAULT_AURA_RADIUS);
        if (russianCount <= 0) {
            return;
        }

        float intensity = Math.min(1.0F, russianCount * 0.35F);
        float t = (player.tickCount + (float) event.getPartialTick()) * 0.1F;

        // Три несинхронных синуса дают "пьяное" плавание
        float rollOffset = (float) Math.sin(t * 0.9F) * RUSSIAN_ROLL_AMPLITUDE * intensity
                         + (float) Math.sin(t * 0.35F + 1.7F) * RUSSIAN_ROLL_AMPLITUDE * 0.5F * intensity;

        float yawOffset = (float) Math.sin(t * 0.7F + 0.5F) * RUSSIAN_YAW_AMPLITUDE * intensity;

        float pitchOffset = (float) Math.sin(t * 0.6F + 2.3F) * RUSSIAN_PITCH_AMPLITUDE * intensity;

        event.setRoll(event.getRoll() + rollOffset);
        event.setYaw(event.getYaw() + yawOffset);
        event.setPitch(event.getPitch() + pitchOffset);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player targetPlayer)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (hasNearbyLabubu(targetPlayer.blockPosition(), LabubuType.ANONYMOUS, ANONYMOUS_AURA_RADIUS)) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static int countNearbyLabubu(LocalPlayer player, LabubuType wantedType, int radius) {
        int count = 0;
        BlockPos center = player.blockPosition();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    Block block = player.level.getBlockState(pos).getBlock();

                    for (Map.Entry<LabubuType, net.minecraftforge.registries.RegistryObject<Block>> entry
                            : ModBlocks.LABUBU_FIGURINE_BLOCKS.entrySet()) {
                        if (entry.getKey() == wantedType && entry.getValue().get() == block) {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    private static boolean hasNearbyLabubu(BlockPos center, LabubuType wantedType, int radius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    Block block = minecraft.level.getBlockState(pos).getBlock();

                    for (Map.Entry<LabubuType, net.minecraftforge.registries.RegistryObject<Block>> entry
                            : ModBlocks.LABUBU_FIGURINE_BLOCKS.entrySet()) {
                        if (entry.getKey() == wantedType && entry.getValue().get() == block) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}