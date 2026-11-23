package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Persistent recipe-history unlocks. Vanilla recipe advancements only know the
 * current inventory; this stores an "ever had" flag in the player's persistent
 * NBT and also checks vanilla pickup statistics for existing worlds.
 */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NinaRecipeHistoryHandler {
    private static final String EVER_IRON = "NinaEverHadIronIngot";
    private static final String KNIFE_GRANTED = "NinaKnifeRecipeGranted";
    private static final String EVER_BORSCHT_INGREDIENT = "NinaEverHadBorschtIngredient";
    private static final String BORSCHT_GRANTED = "NinaBorschtRecipeGranted";

    private NinaRecipeHistoryHandler() {
    }

    @SubscribeEvent
    public static void unlockLifetimeRecipes(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        CompoundTag rootData = player.getPersistentData();
        CompoundTag data = rootData.getCompound(Player.PERSISTED_NBT_TAG);
        rootData.put(Player.PERSISTED_NBT_TAG, data);

        if (!data.getBoolean(EVER_IRON) && hasEverHad(player, Items.IRON_INGOT)) {
            data.putBoolean(EVER_IRON, true);
        }
        if (data.getBoolean(EVER_IRON) && !data.getBoolean(KNIFE_GRANTED)) {
            grantRecipe(player, "nina_knife");
            data.putBoolean(KNIFE_GRANTED, true);
        }

        boolean hasBorschtIngredient = hasEverHad(player, ModItems.SALT.get())
                || hasEverHad(player, ModItems.RADISH.get())
                || hasEverHad(player, Items.POTATO);
        if (!data.getBoolean(EVER_BORSCHT_INGREDIENT) && hasBorschtIngredient) {
            data.putBoolean(EVER_BORSCHT_INGREDIENT, true);
        }
        if (data.getBoolean(EVER_BORSCHT_INGREDIENT) && !data.getBoolean(BORSCHT_GRANTED)) {
            grantRecipe(player, "borscht");
            data.putBoolean(BORSCHT_GRANTED, true);
        }
    }

    private static boolean hasEverHad(ServerPlayer player, Item item) {
        return player.getInventory().items.stream().anyMatch(stack -> stack.is(item))
                || player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item)) > 0;
    }

    private static void grantRecipe(ServerPlayer player, String recipePath) {
        player.getServer().getRecipeManager()
                .byKey(new ResourceLocation(ZitraksMode.MODID, recipePath))
                .ifPresent(recipe -> player.awardRecipes(List.<Recipe<?>>of(recipe)));
    }
}
