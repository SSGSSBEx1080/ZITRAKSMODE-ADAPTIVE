package com.example.zitraksmode.commands;

import com.example.zitraksmode.items.katana.KatanaCooldownManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class KatanaResetCooldownCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("katanaresetcd")
                        .requires(source -> source.hasPermission(2))

                        // Сбросить себе.
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            KatanaCooldownManager.clearPlayer(player);

                            player.sendSystemMessage(
                                    Component.literal("§aКД катаны и зарядка сброшены.")
                            );

                            return 1;
                        })

                        // Сбросить другим игрокам.
                        .then(
                                Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets =
                                                    EntityArgument.getPlayers(context, "targets");

                                            for (ServerPlayer target : targets) {
                                                KatanaCooldownManager.clearPlayer(target);

                                                target.sendSystemMessage(
                                                        Component.literal(
                                                                "§eТвой КД катаны был сброшен."
                                                        )
                                                );
                                            }

                                            try {
                                                ServerPlayer executor =
                                                        context.getSource().getPlayerOrException();

                                                executor.sendSystemMessage(
                                                        Component.literal(
                                                                "§aСброшен КД катаны у игроков: "
                                                                        + targets.size()
                                                        )
                                                );
                                            } catch (Exception ignored) {
                                            }

                                            return targets.size();
                                        })
                        )
        );
    }
}