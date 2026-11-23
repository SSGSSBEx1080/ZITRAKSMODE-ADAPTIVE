package com.example.zitraksmode.mixin;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Удаляет большие данные из автоматического update-пакета принтера.
 */
@Mixin(Printer3DBlockEntity.class)
public abstract class PrinterBlockEntitySyncLimitMixin {

    @Inject(
            method = "getUpdateTag",
            at = @At("RETURN")
    )
    private void zitraksmode$stripLargePrinterData(
            CallbackInfoReturnable<CompoundTag> callback
    ) {
        CompoundTag updateTag = callback.getReturnValue();

        if (updateTag == null) {
            return;
        }

        updateTag.remove("inventory");
        updateTag.remove("syncedCustomRecipe");
    }
}