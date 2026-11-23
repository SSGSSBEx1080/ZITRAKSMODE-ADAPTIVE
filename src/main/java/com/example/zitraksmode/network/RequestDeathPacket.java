package com.example.zitraksmode.network;

import com.example.zitraksmode.items.DeathNoteItem;
import com.example.zitraksmode.items.DeathNoteRequiemItem;
import com.example.zitraksmode.util.DeathNoteHelper;
import com.example.zitraksmode.util.KillMethods;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class RequestDeathPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String targetName;
    private final int time;
    private final int killMethod;

    public RequestDeathPacket(String targetName) {
        this(targetName, 40, 0);
    }

    public RequestDeathPacket(String targetName, int timeSeconds, int killMethod) {
        this.targetName = targetName;
        this.time = timeSeconds * 20;
        this.killMethod = killMethod;
    }

    public static void encode(RequestDeathPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.targetName);
        buf.writeInt(msg.time);
        buf.writeByte(msg.killMethod);
    }

    public static RequestDeathPacket decode(FriendlyByteBuf buf) {
        return new RequestDeathPacket(buf.readUtf(), buf.readInt() / 20, buf.readByte());
    }

    public static void handle(RequestDeathPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!isDeathNote(stack)) {
                stack = player.getOffhandItem();
            }
            if (!isDeathNote(stack)) {
                for (ItemStack s : player.getInventory().items) {
                    if (isDeathNote(s)) {
                        stack = s;
                        break;
                    }
                }
            }
            if (!isDeathNote(stack)) {
                player.sendSystemMessage(Component.literal("§cТетрадь не найдена"));
                LOGGER.warn("[DeathNote] No death note found in inventory or hands");
                return;
            }

            if (DeathNoteHelper.isAnyDeathNoteActive(player)) {
                player.sendSystemMessage(Component.literal("§cТетрадь уже активна, дождись исполнения казни."));
                LOGGER.warn("[DeathNote] Death note already active");
                return;
            }

            boolean isRequiem = stack.getItem() instanceof DeathNoteRequiemItem;
            Entity targetEntity = KillMethods.findTarget(player.level, player, msg.targetName, isRequiem);

            if (targetEntity == null) {
                player.sendSystemMessage(Component.literal("§cЦель не найдена: " + msg.targetName));
                DeathNoteHelper.deactivate(stack);
                ModMessages.sendToPlayer(new DeathNoteMusicPacket(false), player);
                return;
            }

            stack.hurtAndBreak(1, player, p -> {});
            long deathTime = player.level.getGameTime() + msg.time;
            DeathNoteHelper.activate(stack, msg.targetName, deathTime, msg.killMethod, targetEntity.getUUID());

            ModMessages.sendToPlayer(new DeathNoteMusicPacket(true), player);

            LOGGER.info("[DeathNote] Timer set for '{}' at tick {}", msg.targetName, deathTime);
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isDeathNote(ItemStack stack) {
        return stack.getItem() instanceof DeathNoteItem || stack.getItem() instanceof DeathNoteRequiemItem;
    }
}