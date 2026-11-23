package com.example.zitraksmode.client;

import com.example.zitraksmode.items.SniperItem;
import com.example.zitraksmode.network.ModMessages;
import com.example.zitraksmode.network.SniperUltimatePacket;
import com.example.zitraksmode.weapons.ModToolTiers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "zitraksmode", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SniperUltimateKeybind {

    private static boolean wasPressedLastTick = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            wasPressedLastTick = false;
            return;
        }

        if (minecraft.screen != null) {
            wasPressedLastTick = false;
            return;
        }

        long window = minecraft.getWindow().getWindow();
        boolean pressedNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;

        if (pressedNow && !wasPressedLastTick) {
            if (isHoldingNetheriteSniper(minecraft.player.getMainHandItem())
                    || isHoldingNetheriteSniper(minecraft.player.getOffhandItem())) {
                ModMessages.INSTANCE.sendToServer(new SniperUltimatePacket());
            }
        }

        wasPressedLastTick = pressedNow;
    }

    private static boolean isHoldingNetheriteSniper(ItemStack stack) {
        if (!(stack.getItem() instanceof SniperItem sniperItem)) {
            return false;
        }

        return sniperItem.getTier() == ModToolTiers.SNIPER_NETHERITE;
    }
}