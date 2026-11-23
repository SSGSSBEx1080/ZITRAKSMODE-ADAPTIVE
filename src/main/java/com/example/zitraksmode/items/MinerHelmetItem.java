package com.example.zitraksmode.items;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import javax.annotation.Nullable;

public class MinerHelmetItem extends ArmorItem {

    private static final String TAG_LIGHT_PLACED =
            "MinerLightPlaced";

    private static final String TAG_LIGHT_X =
            "MinerLightX";

    private static final String TAG_LIGHT_Y =
            "MinerLightY";

    private static final String TAG_LIGHT_Z =
            "MinerLightZ";

    private static final String TAG_DRAIN_TIMER =
            "MinerDrainTimer";

    public MinerHelmetItem(
            ArmorMaterial material,
            Properties properties
    ) {
        super(
                material,
                EquipmentSlot.HEAD,
                properties
        );
    }

    @Override
    public void onArmorTick(
            ItemStack stack,
            Level level,
            Player player
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (stack.getDamageValue()
                >= stack.getMaxDamage()) {

            removeStoredLight(
                    level,
                    stack
            );

            removeHelmetEffects(
                    player
            );

            return;
        }

        int peresvetLevel =
                getPeresvetLevel(stack);

        /*
         * Ночное зрение.
         */
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        10,
                        0,
                        false,
                        false,
                        true
                )
        );

        /*
         * Спешка:
         *
         * Без Пересвета — Спешка I.
         * Пересвет I — Спешка I.
         * Пересвет II — Спешка II.
         * Пересвет III — Спешка III.
         */
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.DIG_SPEED,
                        10,
                        getHasteAmplifier(peresvetLevel),
                        false,
                        false,
                        true
                )
        );

        tickDurabilityDrain(
                stack,
                player
        );

        if (stack.getDamageValue()
                >= stack.getMaxDamage()) {

            removeStoredLight(
                    level,
                    stack
            );

            removeHelmetEffects(
                    player
            );

            return;
        }

        /*
         * Ищем место для источника света около глаз игрока.
         */
        BlockPos eyePos =
                new BlockPos(
                        (int) Math.floor(player.getX()),
                        (int) Math.floor(player.getEyeY()),
                        (int) Math.floor(player.getZ())
                );

        BlockPos fallbackPos =
                player.blockPosition().above();

        BlockPos targetPos =
                null;

        BlockState eyeState =
                level.getBlockState(eyePos);

        if (eyeState.isAir()
                || eyeState.is(Blocks.LIGHT)) {
            targetPos =
                    eyePos;
        } else {
            BlockState fallbackState =
                    level.getBlockState(fallbackPos);

            if (fallbackState.isAir()
                    || fallbackState.is(Blocks.LIGHT)) {
                targetPos =
                        fallbackPos;
            }
        }

        if (targetPos == null) {
            removeStoredLight(
                    level,
                    stack
            );

            return;
        }

        BlockPos oldPos =
                getStoredLightPos(stack);

        if (targetPos.equals(oldPos)) {
            return;
        }

        removeStoredLight(
                level,
                stack
        );

        BlockState targetState =
                level.getBlockState(targetPos);

        if (targetState.isAir()
                || targetState.is(Blocks.LIGHT)) {

            level.setBlock(
                    targetPos,
                    Blocks.LIGHT.defaultBlockState()
                            .setValue(
                                    LightBlock.LEVEL,
                                    15
                            ),
                    16 | 2
            );

            storeLightPos(
                    stack,
                    targetPos
            );
        }
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected
    ) {
        super.inventoryTick(
                stack,
                level,
                entity,
                slotId,
                isSelected
        );

        if (level.isClientSide()) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        boolean wearingThisStack =
                player.getItemBySlot(
                        EquipmentSlot.HEAD
                ) == stack;

        if (!wearingThisStack) {
            removeStoredLight(
                    level,
                    stack
            );

            ItemStack currentHelmet =
                    player.getItemBySlot(
                            EquipmentSlot.HEAD
                    );

            if (!(currentHelmet.getItem()
                    instanceof MinerHelmetItem)) {

                removeHelmetEffects(
                        player
                );
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );

        int peresvetLevel =
                getPeresvetLevel(stack);

        int secondsLeft =
                getDisplayedSecondsLeft(stack);

        int minutes =
                secondsLeft / 60;

        int seconds =
                secondsLeft % 60;

        if (secondsLeft > 0) {
            tooltip.add(
                    Component.literal(
                            "Осталось: "
                                    + minutes
                                    + " мин "
                                    + seconds
                                    + " сек"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );
        } else {
            tooltip.add(
                    Component.literal(
                            "Разряжен"
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );
        }

        tooltip.add(
                Component.literal(
                        "Эффекты: Ночное зрение, "
                                + getHasteText(peresvetLevel)
                ).withStyle(
                        ChatFormatting.YELLOW
                )
        );

        switch (peresvetLevel) {
            case 1 -> tooltip.add(
                    Component.literal(
                            "Пересвет I: 45 минут работы"
                    ).withStyle(
                            ChatFormatting.AQUA
                    )
            );

            case 2 -> tooltip.add(
                    Component.literal(
                            "Пересвет II: 60 минут работы, Спешка II"
                    ).withStyle(
                            ChatFormatting.AQUA
                    )
            );

            case 3 -> tooltip.add(
                    Component.literal(
                            "Пересвет III: 120 минут работы, Спешка III"
                    ).withStyle(
                            ChatFormatting.AQUA
                    )
            );

            default -> tooltip.add(
                    Component.literal(
                            "Без Пересвета: 30 минут работы"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );
        }
    }

    private void tickDurabilityDrain(
            ItemStack stack,
            Player player
    ) {
        CompoundTag tag =
                stack.getOrCreateTag();

        int timer =
                tag.getInt(
                        TAG_DRAIN_TIMER
                ) + 1;

        int interval =
                getDrainIntervalTicks(stack);

        if (timer >= interval) {
            timer = 0;

            int oldDamage =
                    stack.getDamageValue();

            int maxDamage =
                    stack.getMaxDamage();

            int newDamage =
                    Math.min(
                            maxDamage,
                            oldDamage + 1
                    );

            stack.setDamageValue(
                    newDamage
            );

            if (oldDamage < maxDamage
                    && newDamage >= maxDamage
                    && player instanceof ServerPlayer serverPlayer) {

                ZitraksMode.MINER_DRAINED.trigger(
                        serverPlayer
                );
            }
        }

        tag.putInt(
                TAG_DRAIN_TIMER,
                timer
        );
    }

    private int getPeresvetLevel(
            ItemStack stack
    ) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.PERESVET.get(),
                stack
        );
    }

    /**
     * При прочности 900:
     *
     * 900 * 40 / 20 = 1800 секунд = 30 минут.
     * 900 * 60 / 20 = 2700 секунд = 45 минут.
     * 900 * 80 / 20 = 3600 секунд = 60 минут.
     * 900 * 160 / 20 = 7200 секунд = 120 минут.
     */
    private int getDrainIntervalTicks(
            ItemStack stack
    ) {
        int level =
                getPeresvetLevel(stack);

        return switch (level) {
            case 1 -> 60;
            case 2 -> 80;
            case 3 -> 160;
            default -> 40;
        };
    }

    /**
     * Уровень эффекта:
     *
     * 0 = Спешка I.
     * 1 = Спешка II.
     * 2 = Спешка III.
     */
    private int getHasteAmplifier(
            int peresvetLevel
    ) {
        if (peresvetLevel >= 3) {
            return 2;
        }

        if (peresvetLevel >= 2) {
            return 1;
        }

        return 0;
    }

    private String getHasteText(
            int peresvetLevel
    ) {
        if (peresvetLevel >= 3) {
            return "Спешка III";
        }

        if (peresvetLevel >= 2) {
            return "Спешка II";
        }

        return "Спешка I";
    }

    private int getDisplayedSecondsLeft(
            ItemStack stack
    ) {
        int durabilityLeft =
                Math.max(
                        0,
                        stack.getMaxDamage()
                                - stack.getDamageValue()
                );

        return durabilityLeft
                * getDrainIntervalTicks(stack)
                / 20;
    }

    private void removeHelmetEffects(
            Player player
    ) {
        player.removeEffect(
                MobEffects.NIGHT_VISION
        );

        player.removeEffect(
                MobEffects.DIG_SPEED
        );
    }

    private void storeLightPos(
            ItemStack stack,
            BlockPos pos
    ) {
        CompoundTag tag =
                stack.getOrCreateTag();

        tag.putBoolean(
                TAG_LIGHT_PLACED,
                true
        );

        tag.putInt(
                TAG_LIGHT_X,
                pos.getX()
        );

        tag.putInt(
                TAG_LIGHT_Y,
                pos.getY()
        );

        tag.putInt(
                TAG_LIGHT_Z,
                pos.getZ()
        );
    }

    @Nullable
    private BlockPos getStoredLightPos(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getTag();

        if (tag == null
                || !tag.getBoolean(
                        TAG_LIGHT_PLACED
                )) {
            return null;
        }

        return new BlockPos(
                tag.getInt(TAG_LIGHT_X),
                tag.getInt(TAG_LIGHT_Y),
                tag.getInt(TAG_LIGHT_Z)
        );
    }

    private void clearStoredLight(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(
                TAG_LIGHT_PLACED
        );

        tag.remove(
                TAG_LIGHT_X
        );

        tag.remove(
                TAG_LIGHT_Y
        );

        tag.remove(
                TAG_LIGHT_Z
        );

        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private void removeStoredLight(
            Level level,
            ItemStack stack
    ) {
        BlockPos pos =
                getStoredLightPos(stack);

        if (pos == null) {
            return;
        }

        if (level.getBlockState(pos)
                .is(Blocks.LIGHT)) {

            level.removeBlock(
                    pos,
                    false
            );
        }

        clearStoredLight(
                stack
        );
    }
}