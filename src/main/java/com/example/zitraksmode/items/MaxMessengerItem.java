package com.example.zitraksmode.items;

import com.example.zitraksmode.client.max.MaxMessengerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MaxMessengerItem extends Item {
    private static final String MAX_MESSENGER_GRANTED_TAG = "MaxMessengerGranted";

    public MaxMessengerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new MaxMessengerScreen());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Выдаёт предмет только один раз за жизнь персонажа.
     * Метка хранится в persisted NBT и переживает смерть/перезаход.
     */
    public static void giveIfMissing(ServerPlayer player, Item item) {
        if (player == null || item == null) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistedTag;

        if (persistentData.contains(Player.PERSISTED_NBT_TAG)) {
            persistedTag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        } else {
            persistedTag = new CompoundTag();
            persistentData.put(Player.PERSISTED_NBT_TAG, persistedTag);
        }

        // Игрок уже получал Макса раньше — больше не выдаём.
        if (persistedTag.getBoolean(MAX_MESSENGER_GRANTED_TAG)) {
            return;
        }

        // Для старых игроков: если Макс уже был в инвентаре,
        // просто фиксируем выдачу и не создаём второй экземпляр.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                persistedTag.putBoolean(MAX_MESSENGER_GRANTED_TAG, true);
                return;
            }
        }

        ItemStack stack = new ItemStack(item);

        if (player.getInventory().getItem(8).isEmpty()) {
            player.getInventory().setItem(8, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        // Метка ставится даже если инвентарь был полон и предмет выпал рядом.
        persistedTag.putBoolean(MAX_MESSENGER_GRANTED_TAG, true);
        player.getInventory().setChanged();
    }
}