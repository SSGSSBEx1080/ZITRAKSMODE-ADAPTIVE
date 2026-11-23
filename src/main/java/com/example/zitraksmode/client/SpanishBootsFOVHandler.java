package com.example.zitraksmode.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "zitraksmode", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpanishBootsFOVHandler {

    private static final UUID SPEED_UUID = UUID.fromString("1d2e3f40-6a7b-4c8d-9123-456789abcdef");

    @SubscribeEvent
    public static void onFOVUpdate(ComputeFovModifierEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        var modifier = speedAttribute.getModifier(SPEED_UUID);
        if (modifier != null) {
            event.setNewFovModifier(1.0F);
        }
    }
}