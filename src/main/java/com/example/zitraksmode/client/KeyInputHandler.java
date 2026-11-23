package com.example.zitraksmode.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.abilities.AbilitySystem;
import com.example.zitraksmode.items.SniperItem;
import com.example.zitraksmode.network.NetworkHandler;
import com.example.zitraksmode.network.TriggerExplosionPacket;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, value = Dist.CLIENT)
public class KeyInputHandler {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    
    public static final KeyMapping ULTIMATE_SNIPER_KEY = new KeyMapping(
        "key.zitraksmode.sniper_ultimate",
        GLFW.GLFW_KEY_G,
        "key.categories.misc"
    );
    
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ULTIMATE_SNIPER_KEY);
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (MINECRAFT.player == null) return;
        
        // Вернул проверку клавиши R как было раньше
        if (event.getKey() == GLFW.GLFW_KEY_R && event.getAction() == GLFW.GLFW_PRESS) {
            if (AbilitySystem.isWearingFullTntArmor(MINECRAFT.player) && 
                AbilitySystem.isReadyToExplode(MINECRAFT.player) &&
                !AbilitySystem.isExplosionActivated(MINECRAFT.player)) {
                
                NetworkHandler.INSTANCE.sendToServer(new TriggerExplosionPacket());
            }
        }
        
        if (ULTIMATE_SNIPER_KEY.isDown()) {
            ItemStack mainHand = MINECRAFT.player.getMainHandItem();
            ItemStack offHand = MINECRAFT.player.getOffhandItem();
            
            if (mainHand.getItem() instanceof SniperItem sniper) {
                if (sniper.getTier() == com.example.zitraksmode.weapons.ModToolTiers.SNIPER_NETHERITE) {
                    sniper.activateUltimate(MINECRAFT.player, InteractionHand.MAIN_HAND);
                }
            } else if (offHand.getItem() instanceof SniperItem sniper) {
                if (sniper.getTier() == com.example.zitraksmode.weapons.ModToolTiers.SNIPER_NETHERITE) {
                    sniper.activateUltimate(MINECRAFT.player, InteractionHand.OFF_HAND);
                }
            }
        }
    }
}