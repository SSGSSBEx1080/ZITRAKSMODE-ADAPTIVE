package com.example.zitraksmode.printer;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class PagedPrinterItemHandler implements IItemHandlerModifiable {
    private final ItemStackHandler backingHandler;
    private final int visibleSlots;
    private int firstRealSlot = 0;

    public PagedPrinterItemHandler(ItemStackHandler backingHandler, int visibleSlots) {
        this.backingHandler = backingHandler;
        this.visibleSlots = visibleSlots;
    }

    public void setFirstRealSlot(int firstRealSlot) {
        int maxStart = Math.max(0, backingHandler.getSlots() - visibleSlots);
        this.firstRealSlot = Math.max(0, Math.min(firstRealSlot, maxStart));
    }

    public int getFirstRealSlot() {
        return firstRealSlot;
    }

    private int toRealSlot(int visibleSlot) {
        return firstRealSlot + visibleSlot;
    }

    @Override
    public int getSlots() {
        return visibleSlots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= visibleSlots) {
            return ItemStack.EMPTY;
        }
        int realSlot = toRealSlot(slot);
        if (realSlot < 0 || realSlot >= backingHandler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return backingHandler.getStackInSlot(realSlot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= visibleSlots) {
            return stack;
        }
        int realSlot = toRealSlot(slot);
        if (realSlot < 0 || realSlot >= backingHandler.getSlots()) {
            return stack;
        }
        return backingHandler.insertItem(realSlot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= visibleSlots) {
            return ItemStack.EMPTY;
        }
        int realSlot = toRealSlot(slot);
        if (realSlot < 0 || realSlot >= backingHandler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return backingHandler.extractItem(realSlot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= visibleSlots) {
            return 64;
        }
        int realSlot = toRealSlot(slot);
        if (realSlot < 0 || realSlot >= backingHandler.getSlots()) {
            return 64;
        }
        return backingHandler.getSlotLimit(realSlot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= visibleSlots) {
            return false;
        }
        int realSlot = toRealSlot(slot);
        if (realSlot < 0 || realSlot >= backingHandler.getSlots()) {
            return false;
        }
        return backingHandler.isItemValid(realSlot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= visibleSlots) {
            return;
        }
        int realSlot = toRealSlot(slot);
        if (realSlot < 0 || realSlot >= backingHandler.getSlots()) {
            return;
        }
        backingHandler.setStackInSlot(realSlot, stack);
    }
}
