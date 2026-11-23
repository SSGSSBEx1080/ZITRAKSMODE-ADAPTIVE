package com.example.zitraksmode.events;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.access.BullCowAccess;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BullDropsHandler {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Cow cow)) {
            return;
        }
        if (!(cow instanceof BullCowAccess access) || !access.zitraksmode$isBull()) {
            return;
        }

        int variant = access.zitraksmode$getBullVariant();

        if (variant == BullCowAccess.VARIANT_BALD) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.LEATHER));
            return;
        }

        if (variant == BullCowAccess.VARIANT_COPPER) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.LEATHER));
            int count = 1 + cow.level.random.nextInt(3);
            event.getDrops().add(new ItemEntity(cow.level, cow.getX(), cow.getY(0.5D), cow.getZ(), new ItemStack(ModBlocks.COPPER_WOOL.get(), count)));
        }
    }
}
