package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ModMenuTypes;
import com.example.zitraksmode.caps.EnderCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class EnderSafeMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    // Конструктор для клиента (Forge вызывает его через Network)
    public EnderSafeMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    // Основной конструктор
    public EnderSafeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenuTypes.ENDER_SAFE_MENU.get(), id);
        this.access = access;

        // ВАЖНО: Берем инвентарь из CAPABILITY игрока, а не из блока!
        inv.player.getCapability(EnderCapability.BIG_ENDER_INVENTORY).ifPresent(handler -> {
            // Добавляем 54 слота сейфа (6 рядов по 9 слотов)
            for (int row = 0; row < 6; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new SlotItemHandler(handler, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }
        });

        // Инвентарь игрока (3 ряда)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Хотбар игрока (1 ряд)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 198));
        }
    }

    // Быстрое перемещение (Shift-Click)
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 54) { // Если кликаем в сейфе
                if (!this.moveItemStackTo(itemstack1, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Если кликаем в инвентаре игрока
                if (!this.moveItemStackTo(itemstack1, 0, 54, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        // Проверяем, стоит ли игрок рядом с блоком сейфа
        return stillValid(this.access, player, ModBlocks.ENDER_SAFE.get());
    }
}