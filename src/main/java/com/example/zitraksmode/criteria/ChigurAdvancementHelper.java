package com.example.zitraksmode.criteria;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Grants every remaining criterion of a code-driven Chigur advancement. */
public final class ChigurAdvancementHelper {
    private ChigurAdvancementHelper() {
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
}
