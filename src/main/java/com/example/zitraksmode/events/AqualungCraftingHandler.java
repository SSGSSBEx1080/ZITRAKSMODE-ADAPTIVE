package com.example.zitraksmode.events;

import com.example.zitraksmode.items.AqualungItem;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "zitraksmode")
public class AqualungCraftingHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!(result.getItem() instanceof AqualungItem)) return;

        if (event.getInventory() instanceof CraftingContainer inv) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() == Items.IRON_CHESTPLATE) {
                    int damage = stack.getDamageValue();
                    int maxDamage = Math.max(1, stack.getMaxDamage());
                    float durabilityPercent = (float) (maxDamage - damage) / (float) maxDamage;

                    int resultMaxDamage = Math.max(1, result.getMaxDamage());
                    int resultDamage = Math.round((1.0f - durabilityPercent) * resultMaxDamage);

                    result.setDamageValue(Math.max(0, Math.min(resultMaxDamage, resultDamage)));
                    break;
                }
            }
        }
    }
}