package com.example.zitraksmode.events;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.blockentities.HentaiBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent.PlayerTickEvent; // ✅ ПРАВИЛЬНЫЙ ИМПОРТ ДЛЯ 1.19.2
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HentaiStareHandler {
    
    private static final Map<UUID, Integer> stareTicks = new HashMap<>();
    private static final int REQUIRED_TICKS = 600; // 30 секунд

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) { // ✅ Без .Pre, напрямую PlayerTickEvent
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        HitResult hit = player.pick(5.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            resetStare(player);
            return;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockEntity be = player.level.getBlockEntity(pos);

        if (be instanceof HentaiBlockEntity && player.level.getBlockState(pos).is(ModBlocks.HENTAI_BLOCK.get())) {
            int ticks = stareTicks.getOrDefault(player.getUUID(), 0) + 1;
            stareTicks.put(player.getUUID(), ticks);

            if (ticks >= REQUIRED_TICKS) {
                ZitraksMode.HENTAI_LONG_STARE.trigger(player);
                resetStare(player);
            }
        } else {
            resetStare(player);
        }
    }

    private static void resetStare(ServerPlayer player) {
        stareTicks.remove(player.getUUID());
    }
}