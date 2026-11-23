package com.example.zitraksmode.mixin;

import com.example.zitraksmode.client.gui.Printer3DScreen;
import com.example.zitraksmode.printer.Printer3DMenu;
import com.example.zitraksmode.network.ModPackets;
import com.example.zitraksmode.network.SyncPrinterScrollPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class PrinterScrollBarDragMixin {

    private static final int PRINTER_SCREEN_WIDTH = 420;
    private static final int PRINTER_SCREEN_HEIGHT = 256;
    private static final int SCROLL_X_OFFSET = 268;
    private static final int SCROLL_Y_OFFSET = 30;
    private static final int SCROLL_HEIGHT = 88;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    private boolean zitraksmode$draggingPrinterScroll;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void zitraksmode$startPrinterScrollDrag(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (button != 0 || !isPrinterScreen()) {
            return;
        }

        if (!isOverScrollRail(mouseX, mouseY)) {
            return;
        }

        setScrollFromMouse(mouseY);
        zitraksmode$draggingPrinterScroll = true;
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void zitraksmode$dragPrinterScroll(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!zitraksmode$draggingPrinterScroll
                || button != 0
                || !isPrinterScreen()) {
            return;
        }

        setScrollFromMouse(mouseY);
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void zitraksmode$stopPrinterScrollDrag(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!zitraksmode$draggingPrinterScroll || button != 0) {
            return;
        }

        zitraksmode$draggingPrinterScroll = false;
        callback.setReturnValue(true);
    }

    private boolean isPrinterScreen() {
        return (Object) this instanceof Printer3DScreen
                && menu instanceof Printer3DMenu;
    }

    private boolean isOverScrollRail(double mouseX, double mouseY) {
        int left = (
                Minecraft.getInstance().getWindow().getGuiScaledWidth()
                        - PRINTER_SCREEN_WIDTH
        ) / 2;

        int top = (
                Minecraft.getInstance().getWindow().getGuiScaledHeight()
                        - PRINTER_SCREEN_HEIGHT
        ) / 2;

        return mouseX >= left + SCROLL_X_OFFSET - 4
                && mouseX <= left + SCROLL_X_OFFSET + 6
                && mouseY >= top + SCROLL_Y_OFFSET
                && mouseY <= top + SCROLL_Y_OFFSET + SCROLL_HEIGHT;
    }

    private void setScrollFromMouse(double mouseY) {
        Printer3DMenu printerMenu = (Printer3DMenu) menu;

        int maxScroll = printerMenu.getMaxPrinterScrollRow();

        if (maxScroll <= 0) {
            return;
        }

        int top = (
                Minecraft.getInstance().getWindow().getGuiScaledHeight()
                        - PRINTER_SCREEN_HEIGHT
        ) / 2;

        int thumbHeight = 8;

        double fraction = Math.max(
                0.0D,
                Math.min(
                        1.0D,
                        (mouseY - top - SCROLL_Y_OFFSET - thumbHeight / 2.0D)
                                / (SCROLL_HEIGHT - thumbHeight)
                )
        );

        int row = (int) Math.round(fraction * maxScroll);

        printerMenu.setPrinterScrollRow(row);

        ModPackets.sendToServer(
                new SyncPrinterScrollPacket(
                        printerMenu.getBlockEntity().getBlockPos(),
                        row
                )
        );
    }
}