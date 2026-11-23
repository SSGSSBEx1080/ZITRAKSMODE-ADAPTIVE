package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.MahoragaSummonItem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class MahoragaCooldownCommandHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mahoragaresetcd")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> resetOwnCooldown(context.getSource()))
        );
    }

    private static int resetOwnCooldown(
            CommandSourceStack source
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.MAHORAGA_SCROLL.get()) {
                MahoragaSummonItem.clearStackCooldown(stack);
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == ModItems.MAHORAGA_SCROLL.get()) {
                MahoragaSummonItem.clearStackCooldown(stack);
            }
        }

        source.sendSuccess(
                Component.literal("§aКД свитков Махораги сброшен."),
                false
        );

        return 1;
    }
}