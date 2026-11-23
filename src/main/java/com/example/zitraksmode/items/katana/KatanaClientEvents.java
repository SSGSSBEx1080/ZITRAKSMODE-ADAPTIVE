package com.example.zitraksmode.items.katana;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "zitraksmode", value = Dist.CLIENT)
public class KatanaClientEvents {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    private static final Map<UUID, AttackType> COMBO_TRACKER = new HashMap<>();
    
    // Система комбо (работает на клиенте!)
    public static AttackType getNextAttackType(Player player) {
        UUID uuid = player.getUUID();
        AttackType last = COMBO_TRACKER.getOrDefault(uuid, AttackType.HORIZONTAL);
        AttackType next = AttackType.getRandomExcluding(last);
        COMBO_TRACKER.put(uuid, next);
        return next;
    }
    
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        
        if (stack.getItem() instanceof KatanaItem) {
            LOGGER.info("🔥 LeftClickEmpty - Charging start");
            event.setCanceled(true); // Отменяем копание
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        
        if (stack.getItem() instanceof KatanaItem) {
            event.setCanceled(true); // Катана не копает блоки!
        }
    }
}