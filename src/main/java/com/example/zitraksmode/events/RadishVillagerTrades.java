package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Торговля редиской у фермера.
 *
 * Фермер первого уровня:
 * 1 изумруд -> 4 редиски.
 */
@Mod.EventBusSubscriber(
        modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadishVillagerTrades {

    private RadishVillagerTrades() {
    }

    @SubscribeEvent
    public static void addRadishTrade(VillagerTradesEvent event) {
        /*
         * Только фермер.
         * Уровень 1 — новичок.
         */
        if (event.getType() != VillagerProfession.FARMER) {
            return;
        }

        event.getTrades().get(1).add(
                new BasicItemListing(
                        1,
                        new ItemStack(
                                ModItems.RADISH.get(),
                                4
                        ),
                        16,
                        2,
                        0.05F
                )
        );
    }
}