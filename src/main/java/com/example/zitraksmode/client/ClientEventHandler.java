package com.example.zitraksmode.client;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.client.audio.SpeakerAudioManager;
import com.example.zitraksmode.items.JBLSpeakerItem;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.VolumeChangePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.is(ModItems.JBL_SPEAKER.get())) {
                double scroll = event.getScrollDelta();
                float volumeDelta = (float) (scroll * 0.1F);

                float currentVolume = JBLSpeakerItem.getVolume(stack);
                float newVolume = Math.max(0.0F, Math.min(1.0F, currentVolume + volumeDelta));

                JBLSpeakerItem.setVolume(stack, newVolume);
                PacketHandler.sendToServer(new VolumeChangePacket(hand, newVolume));
                SpeakerAudioManager.setVolume(player.getId(), newVolume);

                event.setCanceled(true);
                break;
            }
        }
    }
}