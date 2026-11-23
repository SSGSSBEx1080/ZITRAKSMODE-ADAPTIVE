package com.example.zitraksmode.util;

import com.example.zitraksmode.ZitraksMode;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * grant(player, "24_scooter/speed_320")
 * → data/{modid}/advancements/24_scooter/speed_320.json
 *
 * criterion "speed_100" + impossible — award по имени из JSON.
 */
public final class ScooterAdvancementHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean missingLogged;

    private ScooterAdvancementHelper() {}

    public static boolean grant(ServerPlayer player, String path) {
        return grant(player, new String[]{path});
    }

    public static boolean grant(ServerPlayer player, String... paths) {
        if (player == null || player.getServer() == null || paths == null) {
            return false;
        }

        for (String path : paths) {
            if (path == null || path.isEmpty()) continue;
            ResourceLocation id = new ResourceLocation(ZitraksMode.MODID, path);
            Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
            if (advancement == null) {
                continue;
            }

            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (progress.isDone()) {
                return true;
            }

            // сначала явный speed_100 (как в JSON), потом остальные
            if (advancement.getCriteria().containsKey("speed_100")) {
                player.getAdvancements().award(advancement, "speed_100");
            }
            for (String criterion : advancement.getCriteria().keySet()) {
                CriterionProgress cp = progress.getCriterion(criterion);
                if (cp != null && !cp.isDone()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }

            if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                LOGGER.info("[ScooterAdv] OK {} → {}", id, player.getGameProfile().getName());
                return true;
            }
        }

        if (!missingLogged) {
            missingLogged = true;
            StringBuilder sb = new StringBuilder("[ScooterAdv] NONE found. Tried: ");
            for (String p : paths) {
                sb.append(ZitraksMode.MODID).append(':').append(p).append(' ');
            }
            sb.append("| put JSON in data/").append(ZitraksMode.MODID)
                    .append("/advancements/24_scooter/speed_320.json");
            LOGGER.warn(sb.toString());
            // один раз в чат — чтобы сразу видно без лога
            player.displayClientMessage(Component.literal(
                    "§c[Scooter] Ачивка не найдена. Нужен файл data/"
                            + ZitraksMode.MODID + "/advancements/24_scooter/speed_320.json"), false);
        }
        return false;
    }
}
