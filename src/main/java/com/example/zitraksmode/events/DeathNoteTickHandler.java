package com.example.zitraksmode.events;

import com.example.zitraksmode.items.DeathNoteItem;
import com.example.zitraksmode.items.DeathNoteRequiemItem;
import com.example.zitraksmode.network.DeathNoteMusicPacket;
import com.example.zitraksmode.network.ModMessages;
import com.example.zitraksmode.util.DeathNoteHelper;
import com.example.zitraksmode.util.KillMethods;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "zitraksmode")
public class DeathNoteTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        for (ItemStack stack : player.getInventory().items) {
            processNote(player, stack);
        }

        for (ItemStack stack : player.getInventory().offhand) {
            processNote(player, stack);
        }
    }

    private static void processNote(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof DeathNoteItem) && !(stack.getItem() instanceof DeathNoteRequiemItem)) {
            return;
        }

        if (!DeathNoteHelper.isActive(stack)) {
            return;
        }

        if (stack.hasTag() && stack.getTag().contains(DeathNoteHelper.TAG_TARGET_UUID)) {
            UUID uuid = stack.getTag().getUUID(DeathNoteHelper.TAG_TARGET_UUID);
            Entity target = KillMethods.findTargetByUuid(player, uuid);
            if (target instanceof LivingEntity living && living.isAlive()) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 20, 0, false, false
                ));
            }
        }

        long now = player.level.getGameTime();
        long deathTime = stack.getOrCreateTag().getLong(DeathNoteHelper.TAG_DEATH_TIME);
        if (now >= deathTime) {
            String name = stack.getTag().getString(DeathNoteHelper.TAG_TARGET);
            byte method = stack.getTag().getByte(DeathNoteHelper.TAG_KILL_METHOD);
            boolean success = KillMethods.executeKill(player, name, method, stack);

            ModMessages.sendToPlayer(new DeathNoteMusicPacket(false), player);

            if (!success) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cЦель не найдена: " + name));
            }

            if (success && stack.getItem() instanceof DeathNoteItem) {
                int kills = DeathNoteHelper.incrementKills(stack);

                if (kills >= 5) {
                    Advancement advancement = player.getServer().getAdvancements()
                            .getAdvancement(new ResourceLocation("zitraksmode", "19_death/five_kills"));
                    if (advancement != null) {
                        player.getAdvancements().award(advancement, "five_kills");
                    }
                }
            }

            DeathNoteHelper.deactivate(stack);
        }
    }
}