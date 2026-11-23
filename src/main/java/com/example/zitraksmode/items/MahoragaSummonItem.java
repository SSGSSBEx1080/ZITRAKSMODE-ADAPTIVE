package com.example.zitraksmode.items;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.entities.MahoragaCinematicEntity;
import com.example.zitraksmode.entities.MahoragaEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MahoragaSummonItem extends Item {

    public static final int COOLDOWN_SEAL_TICKS = 12000;   // 10 минут
    public static final int COOLDOWN_CANCEL_TICKS = 6000;  // 5 минут

    private static final String TAG_ACTIVE = "MahoragaScrollActive";
    private static final String TAG_OWNER = "MahoragaScrollOwner";
    private static final String TAG_TOKEN = "MahoragaScrollToken";
    private static final String TAG_COOLDOWN_END = "MahoragaScrollCooldownEnd";

    private static final AABB GLOBAL_SEARCH_BOX = new AABB(
            -30000000.0D, -2048.0D, -30000000.0D,
             30000000.0D,  4096.0D,  30000000.0D
    );

    public MahoragaSummonItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFireResistant() {
        return true;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public @Nullable Entity createEntity(Level level, Entity location, ItemStack itemstack) {
        ResistantMahoragaScrollEntity entity = new ResistantMahoragaScrollEntity(
                level,
                location.getX(),
                location.getY(),
                location.getZ(),
                itemstack.copy()
        );
        entity.setDeltaMovement(location.getDeltaMovement());
        entity.setPickUpDelay(10);
        return entity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (isStackOnCooldown(stack, level)) {
            long remaining = getRemainingCooldownTicks(stack, level);
            player.displayClientMessage(
                    Component.literal("§cСвиток ещё восстанавливается: §e" + formatTicks(remaining)),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        UUID playerUUID = player.getUUID();

        UUID stackOwner = getStackOwner(stack);
        UUID stackToken = getStackToken(stack);

        if (hasActiveData(stack) && stackOwner != null && !playerUUID.equals(stackOwner)) {
            if (hasAnyActiveForToken(level, stackOwner, stackToken)) {
                player.displayClientMessage(
                        Component.literal("§cТолько хозяин этого свитка может управлять Махорагой."),
                        true
                );
                return InteractionResultHolder.fail(stack);
            } else {
                clearActiveData(stack);
                stackOwner = null;
                stackToken = null;
            }
        }

        MahoragaCinematicEntity ownCinematic = findMatchingCinematic(level, playerUUID, stackToken);
        if (ownCinematic != null) {
            ownCinematic.cancelSummon();
            clearActiveData(stack);
            setStackCooldown(stack, level, COOLDOWN_CANCEL_TICKS);
            player.displayClientMessage(
                    Component.literal("§6Ритуал призыва прерван. §7(КД 5 минут)"),
                    true
            );
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        MahoragaEntity ownMahoraga = findMatchingMahoraga(level, playerUUID, stackToken);
        if (ownMahoraga != null) {
            ownMahoraga.discard();
            clearActiveData(stack);
            setStackCooldown(stack, level, COOLDOWN_SEAL_TICKS);
            player.displayClientMessage(
                    Component.literal("§5Махорага изгнан обратно в свиток. §7(КД 10 минут)"),
                    true
            );
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (hasActiveData(stack)) {
            clearActiveData(stack);
        }

        if (hasAnyOwnerCinematic(level, playerUUID) || hasAnyOwnerMahoraga(level, playerUUID)) {
            player.displayClientMessage(
                    Component.literal("§cМожешь отменить или изгнать только тем же свитком, которым призвал."),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        EntityType<MahoragaCinematicEntity> type = ModEntities.MAHORAGA_CINEMATIC.get();
        if (type == null) {
            player.displayClientMessage(
                    Component.literal("§cОшибка: сущность синематика не найдена"),
                    false
            );
            return InteractionResultHolder.fail(stack);
        }

        MahoragaCinematicEntity cinematic = type.create(level);
        if (cinematic == null) {
            player.displayClientMessage(
                    Component.literal("§cОшибка: не удалось создать ритуал призыва"),
                    false
            );
            return InteractionResultHolder.fail(stack);
        }

        UUID summonToken = UUID.randomUUID();

        cinematic.setPos(player.getX(), player.getY(), player.getZ());
        cinematic.setOwnerUUID(playerUUID);
        cinematic.setSummonToken(summonToken);
        level.addFreshEntity(cinematic);

        setActiveData(stack, playerUUID, summonToken);

        player.displayClientMessage(
                Component.literal("§5Ритуал призыва начат..."),
                true
        );

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (level != null && isStackOnCooldown(stack, level)) {
            tooltip.add(Component.literal("§cКД: §e" + formatTicks(getRemainingCooldownTicks(stack, level))));
        } else if (stack.hasTag() && stack.getTag().contains(TAG_COOLDOWN_END)) {
            tooltip.add(Component.literal("§cКД активен"));
        }

        if (hasActiveData(stack)) {
            tooltip.add(Component.literal("§5Привязан к активному ритуалу / Махораге"));
        } else {
            tooltip.add(Component.literal("§7ПКМ — начать ритуал"));
            tooltip.add(Component.literal("§7ПКМ ещё раз — прервать / изгнать"));
        }

        tooltip.add(Component.literal("§6Не сгорает в огне, лаве и кактусе"));
    }

    public static boolean isStackOnCooldown(ItemStack stack, Level level) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_COOLDOWN_END)) {
            return false;
        }

        long end = stack.getTag().getLong(TAG_COOLDOWN_END);
        long now = level.getGameTime();

        if (now >= end) {
            stack.getTag().remove(TAG_COOLDOWN_END);
            return false;
        }

        return true;
    }

    public static long getRemainingCooldownTicks(ItemStack stack, Level level) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_COOLDOWN_END)) {
            return 0L;
        }
        return Math.max(0L, stack.getTag().getLong(TAG_COOLDOWN_END) - level.getGameTime());
    }

    public static void setStackCooldown(ItemStack stack, Level level, int ticks) {
        stack.getOrCreateTag().putLong(TAG_COOLDOWN_END, level.getGameTime() + ticks);
    }

    public static void clearStackCooldown(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove(TAG_COOLDOWN_END);
        }
    }

    public static void setActiveData(ItemStack stack, UUID ownerUUID, UUID token) {
        stack.getOrCreateTag().putBoolean(TAG_ACTIVE, true);
        stack.getOrCreateTag().putUUID(TAG_OWNER, ownerUUID);
        stack.getOrCreateTag().putUUID(TAG_TOKEN, token);
    }

    public static void clearActiveData(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }

        stack.getTag().remove(TAG_ACTIVE);
        stack.getTag().remove(TAG_OWNER);
        stack.getTag().remove(TAG_TOKEN);
    }

    public static boolean hasActiveData(ItemStack stack) {
        return stack.hasTag()
                && stack.getTag().getBoolean(TAG_ACTIVE)
                && stack.getTag().hasUUID(TAG_OWNER)
                && stack.getTag().hasUUID(TAG_TOKEN);
    }

    public static boolean matchesSummonScroll(ItemStack stack, UUID ownerUUID, UUID token) {
        if (!(stack.getItem() instanceof MahoragaSummonItem)) {
            return false;
        }
        if (!hasActiveData(stack)) {
            return false;
        }

        UUID stackOwner = getStackOwner(stack);
        UUID stackToken = getStackToken(stack);

        return ownerUUID.equals(stackOwner) && token.equals(stackToken);
    }

    @Nullable
    public static UUID getStackOwner(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().hasUUID(TAG_OWNER)) {
            return stack.getTag().getUUID(TAG_OWNER);
        }
        return null;
    }

    @Nullable
    public static UUID getStackToken(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().hasUUID(TAG_TOKEN)) {
            return stack.getTag().getUUID(TAG_TOKEN);
        }
        return null;
    }

    private static boolean hasAnyOwnerCinematic(Level level, UUID ownerUUID) {
        return !level.getEntitiesOfClass(
                MahoragaCinematicEntity.class,
                GLOBAL_SEARCH_BOX,
                entity -> ownerUUID.equals(entity.getOwnerUUID())
        ).isEmpty();
    }

    private static boolean hasAnyOwnerMahoraga(Level level, UUID ownerUUID) {
        return !level.getEntitiesOfClass(
                MahoragaEntity.class,
                GLOBAL_SEARCH_BOX,
                entity -> entity.getOwnerUUID() != null && ownerUUID.equals(entity.getOwnerUUID())
        ).isEmpty();
    }

    private static boolean hasAnyActiveForToken(Level level, UUID ownerUUID, @Nullable UUID token) {
        if (token == null) {
            return false;
        }
        return findMatchingCinematic(level, ownerUUID, token) != null
                || findMatchingMahoraga(level, ownerUUID, token) != null;
    }

    @Nullable
    private static MahoragaCinematicEntity findMatchingCinematic(Level level, UUID ownerUUID, @Nullable UUID token) {
        if (token == null) {
            return null;
        }

        return level.getEntitiesOfClass(
                MahoragaCinematicEntity.class,
                GLOBAL_SEARCH_BOX,
                entity -> ownerUUID.equals(entity.getOwnerUUID()) && token.equals(entity.getSummonToken())
        ).stream().findFirst().orElse(null);
    }

    @Nullable
    private static MahoragaEntity findMatchingMahoraga(Level level, UUID ownerUUID, @Nullable UUID token) {
        if (token == null) {
            return null;
        }

        return level.getEntitiesOfClass(
                MahoragaEntity.class,
                GLOBAL_SEARCH_BOX,
                entity -> entity.getOwnerUUID() != null
                        && ownerUUID.equals(entity.getOwnerUUID())
                        && token.equals(entity.getSummonToken())
        ).stream().findFirst().orElse(null);
    }

    private static String formatTicks(long ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        if (minutes > 0) {
            return minutes + "м " + seconds + "с";
        }
        return seconds + "с";
    }

    public static class ResistantMahoragaScrollEntity extends ItemEntity {
        public ResistantMahoragaScrollEntity(Level level, double x, double y, double z, ItemStack stack) {
            super(level, x, y, z, stack);
        }

        @Override
        public boolean fireImmune() {
            return true;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (source.isFire() || source == DamageSource.CACTUS) {
                return false;
            }
            return super.hurt(source, amount);
        }
    }
}