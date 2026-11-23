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
 * Valya's GOST point: every ordinary professional villager can sell sausage.
 * Nitwits and unemployed villagers are deliberately excluded.
 */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GostVillagerTrades {
    private GostVillagerTrades() {
    }

    @SubscribeEvent
    public static void addDoctorskayaTrade(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) return;

        event.getTrades().get(1).add(new BasicItemListing(
                1,
                new ItemStack(ModItems.GOST_DOCTORSKAYA_SAUSAGE.get()),
                16,
                1,
                0.05F
        ));
    }
}
