package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LogiaCloakItem;
import com.example.zitraksmode.items.LogiaCloakType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class LogiaCloakRepairHandler {

    private LogiaCloakRepairHandler() {
    }

    @SubscribeEvent
    public static void repairCloak(
            AnvilUpdateEvent event
    ) {
        ItemStack left =
                event.getLeft();

        ItemStack right =
                event.getRight();

        LogiaCloakItem cloak =
                LogiaCloakItem.fromStack(left);

        if (cloak == null) {
            return;
        }

        if (right.isEmpty()) {
            return;
        }

        if (left.getDamageValue() <= 0) {
            return;
        }

        RepairData repairData =
                getRepairData(
                        cloak.getCloakType(),
                        right
                );

        if (repairData == null) {
            return;
        }

        ItemStack output =
                left.copy();

        int repairAmount =
                Math.max(
                        1,
                        (int) Math.round(
                                left.getMaxDamage()
                                        * repairData.repairPercent
                        )
                );

        int newDamage =
                Math.max(
                        0,
                        left.getDamageValue()
                                - repairAmount
                );

        output.setDamageValue(
                newDamage
        );

        event.setOutput(
                output
        );

        /*
         * Один материал за операцию.
         */
        event.setMaterialCost(
                1
        );

        /*
         * Все ремонты стоят только 1 уровень.
         * Это минимальная стоимость, при которой
         * наковальня нормально выдаёт результат.
         */
        event.setCost(
                1
        );
    }

    private static RepairData getRepairData(
            LogiaCloakType cloakType,
            ItemStack material
    ) {
        /*
         * Акаину.
         *
         * Сгусток магмы: 15%
         * Огненный заряд: 28%
         * Ведро лавы: 48%
         * Магма-блок: 70%
         */
        if (cloakType == LogiaCloakType.AKAINU) {
            if (material.is(Items.MAGMA_CREAM)) {
                return new RepairData(0.15D);
            }

            if (material.is(Items.FIRE_CHARGE)) {
                return new RepairData(0.28D);
            }

            if (material.is(Items.LAVA_BUCKET)) {
                return new RepairData(0.48D);
            }

            if (material.is(Items.MAGMA_BLOCK)) {
                return new RepairData(0.70D);
            }

            return null;
        }

        /*
         * Аокидзи.
         *
         * Снежок: 10%
         * Обычный лёд: 20%
         * Плотный лёд: 35%
         * Синий лёд: 50%
         * Алмаз: 75%
         */
        if (cloakType == LogiaCloakType.AOKIJI) {
            if (material.is(Items.SNOWBALL)) {
                return new RepairData(0.10D);
            }

            if (material.is(Items.ICE)) {
                return new RepairData(0.20D);
            }

            if (material.is(Items.PACKED_ICE)) {
                return new RepairData(0.35D);
            }

            if (material.is(Items.BLUE_ICE)) {
                return new RepairData(0.50D);
            }

            if (material.is(Items.DIAMOND)) {
                return new RepairData(0.75D);
            }

            return null;
        }

        /*
         * Кизару.
         *
         * Осколок аметиста: 20%
         * Светопыль: 42%
         * Светокамень: 70%
         */
        if (cloakType == LogiaCloakType.KIZARU) {
            if (material.is(Items.AMETHYST_SHARD)) {
                return new RepairData(0.20D);
            }

            if (material.is(Items.GLOWSTONE_DUST)) {
                return new RepairData(0.42D);
            }

            if (material.is(Items.GLOWSTONE)) {
                return new RepairData(0.70D);
            }

            return null;
        }

        /*
         * Белоус.
         *
         * Железный слиток: 32%
         * Сердце моря: 80%
         */
        if (cloakType == LogiaCloakType.WHITEBEARD) {
            if (material.is(Items.IRON_INGOT)) {
                return new RepairData(0.32D);
            }

            if (material.is(Items.HEART_OF_THE_SEA)) {
                return new RepairData(0.80D);
            }

            return null;
        }

        /*
         * Сэнгоку.
         *
         * Золотой слиток: 55%
         */
        if (cloakType == LogiaCloakType.SENGOKU) {
            if (material.is(Items.GOLD_INGOT)) {
                return new RepairData(0.55D);
            }

            return null;
        }

        /*
         * У Крокодайла специального ремонта нет.
         */
        return null;
    }

    private record RepairData(
            double repairPercent
    ) {
    }
}