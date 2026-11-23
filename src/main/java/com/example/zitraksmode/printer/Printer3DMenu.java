package com.example.zitraksmode.printer;

import com.example.zitraksmode.ModMenuTypes;
import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class Printer3DMenu extends AbstractContainerMenu {

    public static final int PRINTER_COLUMNS = 9;
    public static final int VISIBLE_PRINTER_ROWS = 5;
    public static final int VISIBLE_PRINTER_SLOT_COUNT = PRINTER_COLUMNS * VISIBLE_PRINTER_ROWS;

    private static final int PRINTER_SLOT_START_X = 104;
    private static final int PRINTER_SLOT_START_Y = 30;

    private static final int PLAYER_INV_START = VISIBLE_PRINTER_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private static final int PLAYER_INV_START_Y = 154;
    private static final int HOTBAR_START_Y = 212;

    private final Printer3DBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final PagedPrinterItemHandler visiblePrinterHandler;

    private int printerScrollRow = 0;

    public Printer3DMenu(int id, Inventory playerInv, Printer3DBlockEntity be) {
        super(ModMenuTypes.PRINTER_3D_MENU.get(), id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.visiblePrinterHandler = new PagedPrinterItemHandler(be.getItemHandler(), VISIBLE_PRINTER_SLOT_COUNT);

        addPrinterSlots();
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
        updateVisiblePage();
    }

    public Printer3DMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, (Printer3DBlockEntity) playerInv.player.level.getBlockEntity(extraData.readBlockPos()));
    }

    private void addPrinterSlots() {
        for (int i = 0; i < VISIBLE_PRINTER_SLOT_COUNT; i++) {
            int row = i / PRINTER_COLUMNS;
            int col = i % PRINTER_COLUMNS;
            addSlot(new SlotItemHandler(
                    visiblePrinterHandler,
                    i,
                    PRINTER_SLOT_START_X + col * 18,
                    PRINTER_SLOT_START_Y + row * 18
            ));
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_START_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInv) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, HOTBAR_START_Y));
        }
    }

    public int getPrinterScrollRow() {
        return printerScrollRow;
    }

    public int getMaxPrinterScrollRow() {
        int totalRows = (int) Math.ceil((double) Printer3DBlockEntity.PRINTER_INTERNAL_SLOT_COUNT / PRINTER_COLUMNS);
        return Math.max(0, totalRows - VISIBLE_PRINTER_ROWS);
    }

    public void setPrinterScrollRow(int row) {
        this.printerScrollRow = Math.max(0, Math.min(row, getMaxPrinterScrollRow()));
        updateVisiblePage();
    }

    public void setPrinterScrollRowServer(int row) {
        this.printerScrollRow = Math.max(0, Math.min(row, getMaxPrinterScrollRow()));
        visiblePrinterHandler.setFirstRealSlot(this.printerScrollRow * PRINTER_COLUMNS);
        broadcastChanges();
    }

    private void updateVisiblePage() {
        visiblePrinterHandler.setFirstRealSlot(printerScrollRow * PRINTER_COLUMNS);
        broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index < VISIBLE_PRINTER_SLOT_COUNT) {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveIntoFullPrinter(stackInSlot)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stackInSlot);
        return result;
    }

    private boolean moveIntoFullPrinter(ItemStack stack) {
        if (stack.isEmpty()) return false;

        var handler = blockEntity.getItemHandler();

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack existing = handler.getStackInSlot(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
                int canMove = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (canMove > 0) {
                    existing.grow(canMove);
                    stack.shrink(canMove);
                    handler.setStackInSlot(i, existing);
                    if (stack.isEmpty()) return true;
                }
            }
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack existing = handler.getStackInSlot(i);
            if (existing.isEmpty()) {
                int put = Math.min(stack.getMaxStackSize(), stack.getCount());
                ItemStack copy = stack.copy();
                copy.setCount(put);
                handler.setStackInSlot(i, copy);
                stack.shrink(put);
                if (stack.isEmpty()) return true;
            }
        }

        return stack.getCount() == 0;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level.isClientSide) {
            blockEntity.releaseUserIfMatches(player);
        }
    }

    public Printer3DBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
