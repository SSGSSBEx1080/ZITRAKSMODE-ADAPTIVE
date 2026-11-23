package com.example.zitraksmode.commands;

import com.example.zitraksmode.events.IntimacyManager;
import com.example.zitraksmode.events.IntimacyTargetHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class IntimacyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /sex <цель>
        dispatcher.register(Commands.literal("sex")
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(ctx -> {
                            Player sender = ctx.getSource().getPlayerOrException();
                            String targetName = StringArgumentType.getString(ctx, "target");
                            Entity target = IntimacyTargetHelper.findTarget(sender, targetName);
                            if (target == null) {
                                sender.displayClientMessage(Component.literal(
                                        "Цель не найдена."
                                ), false);
                                return 1;
                            }
                            IntimacyManager.sendInvitation(sender, target, false);
                            return 1;
                        })
                )
        );

        // /blowjob <цель>
        dispatcher.register(Commands.literal("blowjob")
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(ctx -> {
                            Player sender = ctx.getSource().getPlayerOrException();
                            String targetName = StringArgumentType.getString(ctx, "target");
                            Entity target = IntimacyTargetHelper.findTarget(sender, targetName);
                            if (target == null) {
                                sender.displayClientMessage(Component.literal(
                                        "Цель не найдена."
                                ), false);
                                return 1;
                            }
                            IntimacyManager.sendInvitation(sender, target, true);
                            return 1;
                        })
                )
        );

        // /yes
        dispatcher.register(Commands.literal("yes")
                .executes(ctx -> {
                    Player player = ctx.getSource().getPlayerOrException();
                    IntimacyManager.acceptInvitation(player);
                    return 1;
                })
        );

        // /no
        dispatcher.register(Commands.literal("no")
                .executes(ctx -> {
                    Player player = ctx.getSource().getPlayerOrException();
                    IntimacyManager.declineInvitation(player);
                    return 1;
                })
        );
    }
}