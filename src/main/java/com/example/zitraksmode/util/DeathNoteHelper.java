package com.example.zitraksmode.util;

import com.example.zitraksmode.items.DeathNoteItem;
import com.example.zitraksmode.items.DeathNoteRequiemItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class DeathNoteHelper {

    public static final String TAG_ACTIVE = "active";
    public static final String TAG_TARGET = "target";
    public static final String TAG_TARGET_UUID = "targetUUID";
    public static final String TAG_DEATH_TIME = "deathTime";
    public static final String TAG_KILL_METHOD = "killMethod";
    public static final String TAG_KILLS = "kills";
    public static final String TAG_MAX_USES = "maxUses";

    public static void activate(ItemStack stack, String targetName, long deathTime, int killMethod, UUID targetUUID) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_ACTIVE, true);
        tag.putString(TAG_TARGET, targetName);
        tag.putLong(TAG_DEATH_TIME, deathTime);
        tag.putByte(TAG_KILL_METHOD, (byte) killMethod);
        if (targetUUID != null) {
            tag.putUUID(TAG_TARGET_UUID, targetUUID);
        }
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_ACTIVE);
    }

    public static void deactivate(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().putBoolean(TAG_ACTIVE, false);
            stack.getTag().remove(TAG_TARGET);
            stack.getTag().remove(TAG_DEATH_TIME);
            stack.getTag().remove(TAG_TARGET_UUID);
        }
    }

    public static int incrementKills(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int kills = tag.getInt(TAG_KILLS) + 1;
        tag.putInt(TAG_KILLS, kills);
        return kills;
    }

    public static boolean isAnyDeathNoteActive(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isDeathNoteStack(stack) && isActive(stack)) {
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (isDeathNoteStack(stack) && isActive(stack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isDeathNoteStack(ItemStack stack) {
        return stack.getItem() instanceof DeathNoteItem || stack.getItem() instanceof DeathNoteRequiemItem;
    }
}