package com.example.zitraksmode.util;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;

/** Awards the impossible-criterion cloak advancements after the capability
 * remembers that a player has possessed a cloak at least once. */
public final class LogiaAdvancementHelper {
    private LogiaAdvancementHelper() {
    }

    public static void grant(ServerPlayer player, String path) {
        if (player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(ZitraksMode.MODID, path));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static boolean isDone(ServerPlayer player, String path) {
        if (player.getServer() == null) return false;
        Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(ZitraksMode.MODID, path));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void unlockRecipes(ServerPlayer player, String... paths) {
        if (player.getServer() == null) return;
        List<Recipe<?>> recipes = new ArrayList<>();
        for (String path : paths) {
            player.getServer().getRecipeManager()
                    .byKey(new ResourceLocation(ZitraksMode.MODID, path))
                    .ifPresent(recipes::add);
        }
        if (!recipes.isEmpty()) player.awardRecipes(recipes);
    }
}
