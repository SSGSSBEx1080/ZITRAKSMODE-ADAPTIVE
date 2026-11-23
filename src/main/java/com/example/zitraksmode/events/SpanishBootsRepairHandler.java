package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.SpanishBootsItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ремонт испанских сапог медной шерстью.
 *
 * Один предмет Copper Wool восстанавливает 25% максимальной прочности.
 * Стоимость операции — 1 уровень опыта.
 */
@Mod.EventBusSubscriber(
        modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SpanishBootsRepairHandler {

    private SpanishBootsRepairHandler() {
    }

    @SubscribeEvent
    public static void repairSpanishBoots(
            AnvilUpdateEvent event
    ) {
        ItemStack left =
                event.getLeft();

        ItemStack right =
                event.getRight();

        /*
         * Слева должны быть именно испанские сапоги.
         */
        if (!(left.getItem()
                instanceof SpanishBootsItem)) {
            return;
        }

        /*
         * Справа должна быть медная шерсть.
         */
        if (!right.is(
                ModItems.COPPER_WOOL.get()
        )) {
            return;
        }

        /*
         * Целые сапоги чинить не нужно.
         */
        if (left.getDamageValue() <= 0) {
            return;
        }

        ItemStack output =
                left.copy();

        int repairAmount =
                Math.max(
                        1,
                        left.getMaxDamage() / 4
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
         * Расходуется одна медная шерсть.
         */
        event.setMaterialCost(
                1
        );

        /*
         * Стоимость в уровнях опыта.
         */
        event.setCost(
                1
        );
    }
}