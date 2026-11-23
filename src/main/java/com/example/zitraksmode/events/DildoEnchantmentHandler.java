package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.DildoSeatEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DildoEnchantmentHandler {

    private static Field TIME_UNTIL_LURED_FIELD;
    private static Field TIME_UNTIL_HOOKED_FIELD;
    private static boolean REFLECTION_INIT = false;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player == null) {
            return;
        }

        if (!(player.getVehicle() instanceof DildoSeatEntity seat)) {
            return;
        }

        if (!seat.hasMorichekBuffActive()) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 10, 4, false, false, true));

        FishingHook hook = player.fishing;
        if (hook != null && hook.isAlive()) {
            speedUpFishingHook(hook);
        }
    }

    private static void speedUpFishingHook(FishingHook hook) {
        tryInitReflection();
        if (TIME_UNTIL_LURED_FIELD == null || TIME_UNTIL_HOOKED_FIELD == null) {
            return;
        }

        try {
            int lured = TIME_UNTIL_LURED_FIELD.getInt(hook);
            int hooked = TIME_UNTIL_HOOKED_FIELD.getInt(hook);

            if (hook.tickCount < 8) {
                TIME_UNTIL_LURED_FIELD.setInt(hook, Math.min(lured, 1));
            } else {
                TIME_UNTIL_LURED_FIELD.setInt(hook, Math.min(lured, 2));
            }

            TIME_UNTIL_HOOKED_FIELD.setInt(hook, Math.min(hooked, 2));
        } catch (Exception ignored) {
        }
    }

    private static void tryInitReflection() {
        if (REFLECTION_INIT) {
            return;
        }
        REFLECTION_INIT = true;

        try {
            TIME_UNTIL_LURED_FIELD = FishingHook.class.getDeclaredField("timeUntilLured");
            TIME_UNTIL_LURED_FIELD.setAccessible(true);
        } catch (Exception ignored) {
            TIME_UNTIL_LURED_FIELD = null;
        }

        try {
            TIME_UNTIL_HOOKED_FIELD = FishingHook.class.getDeclaredField("timeUntilHooked");
            TIME_UNTIL_HOOKED_FIELD.setAccessible(true);
        } catch (Exception ignored) {
            TIME_UNTIL_HOOKED_FIELD = null;
        }
    }
}