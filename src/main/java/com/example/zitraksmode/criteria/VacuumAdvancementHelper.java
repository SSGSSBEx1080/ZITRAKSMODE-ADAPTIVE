package com.example.zitraksmode.criteria;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class VacuumAdvancementHelper {
    private VacuumAdvancementHelper() {
    }

    public static void grant(Player player, String path) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) return;
        Advancement advancement = serverPlayer.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(ZitraksMode.MODID, path));
        if (advancement == null) return;
        AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            serverPlayer.getAdvancements().award(advancement, criterion);
        }
    }
}
