package com.example.zitraksmode.network.labubu;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.blocks.LabubuBoxBlock;
import com.example.zitraksmode.items.LabubuFigurineItem;
import com.example.zitraksmode.items.LabubuLootTable;
import com.example.zitraksmode.items.LabubuType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LabubuCaseManager {

    private static final long EXPIRE_MS = 30000L;

    private static final Map<UUID, PendingCase> PENDING_BY_PLAYER = new HashMap<>();

    // Увеличили длину ленты, чтобы старт был сразу заполнен и крутка была длиннее
    private static final int SEQUENCE_SIZE = 140;
    private static final int WINNER_INDEX = 95;

    public static boolean startCase(ServerPlayer player, ServerLevel level, BlockPos pos) {
        cleanupExpired();

        if (PENDING_BY_PLAYER.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cСначала дождись окончания текущей рулетки."));
            return false;
        }

        if (!(level.getBlockState(pos).getBlock() instanceof LabubuBoxBlock)) {
            return false;
        }

        // Сразу ломаем коробку, чтобы никто другой не успел нажать
        level.destroyBlock(pos, false);

        LabubuType reward = LabubuLootTable.getRandomType();
        List<LabubuType> sequence = LabubuLootTable.createRouletteSequence(reward, SEQUENCE_SIZE, WINNER_INDEX);

        PendingCase pending = new PendingCase(level.dimension().location().toString(), pos.immutable(), reward, System.currentTimeMillis());
        PENDING_BY_PLAYER.put(player.getUUID(), pending);

        LabubuCasePackets.sendToPlayer(new OpenLabubuCasePacket(sequence, WINNER_INDEX), player);
        return true;
    }

    public static void claimReward(ServerPlayer player) {
        cleanupExpired();

        PendingCase pending = PENDING_BY_PLAYER.remove(player.getUUID());
        if (pending == null) {
            return;
        }

        ServerLevel level = player.server.overworld();
        for (ServerLevel sl : player.server.getAllLevels()) {
            if (sl.dimension().location().toString().equals(pending.dimensionId)) {
                level = sl;
                break;
            }
        }

        ItemStack figurineStack = LabubuLootTable.getFigurineStack(pending.reward);

        if (!player.getInventory().add(figurineStack)) {
            player.drop(figurineStack, false);
        }

        if (figurineStack.getItem() instanceof LabubuFigurineItem figurineItem) {
            ZitraksMode.LABUBU_COLLECTOR.checkAndTrigger(player, figurineItem.type);
        }

        player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pending.pos.getX() + 0.5,
                pending.pos.getY() + 1.0,
                pending.pos.getZ() + 0.5,
                20,
                0.35,
                0.35,
                0.35,
                0.2
        );
    }

    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        PENDING_BY_PLAYER.entrySet().removeIf(entry -> now - entry.getValue().createdAtMs > EXPIRE_MS);
    }

    private static class PendingCase {
        private final String dimensionId;
        private final BlockPos pos;
        private final LabubuType reward;
        private final long createdAtMs;

        private PendingCase(String dimensionId, BlockPos pos, LabubuType reward, long createdAtMs) {
            this.dimensionId = dimensionId;
            this.pos = pos;
            this.reward = reward;
            this.createdAtMs = createdAtMs;
        }
    }
}