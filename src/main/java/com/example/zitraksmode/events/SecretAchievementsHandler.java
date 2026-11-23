package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class SecretAchievementsHandler {

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        Block placedBlock = event.getPlacedBlock().getBlock();

        // Отладка
        System.out.println("[DEBUG] Block placed: " + ForgeRegistries.BLOCKS.getKey(placedBlock) + " at " + pos);

        if (placedBlock != ModBlocks.EIGHT.get()) return;

        BlockPos pos1 = pos.west(3);
        BlockPos pos2 = pos.west(2);
        BlockPos pos3 = pos.west(1);

        Block b1 = level.getBlockState(pos1).getBlock();
        Block b2 = level.getBlockState(pos2).getBlock();
        Block b3 = level.getBlockState(pos3).getBlock();

        boolean match = (b1 == ModBlocks.ONE.get()) &&
                        (b2 == ModBlocks.FOUR.get()) &&
                        (b3 == ModBlocks.EIGHT.get());

        if (match) {
            ZitraksMode.SECRET_1488.trigger(player);
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§5§l[СЕКРЕТ РАСКРЫТ]"),
                true
            );
            System.out.println("[DEBUG] Secret 1488 unlocked for " + player.getName().getString());
        }
    }
}