package com.example.zitraksmode.mixin;

import com.example.zitraksmode.client.gui.Printer3DScreen;
import com.example.zitraksmode.printer.custom.PrinterRecipeLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Увеличивает клиентские лимиты рецептов 3D-принтера с 10 до 20.
 *
 * Скролл здесь не изменяем: метод mouseScrolled не объявлен
 * непосредственно в Printer3DScreen и из-за этого ломает запуск.
 */
@Mixin(Printer3DScreen.class)
public abstract class PrinterRecipeLimitScreenMixin {

    @ModifyConstant(
            method = "openImportDialog",
            constant = @Constant(intValue = 10),
            remap = false
    )
    private int zitraksmode$twentyRecipeImportLimit(int original) {
        return PrinterRecipeLimits.MAX_RECIPES_PER_PLAYER;
    }

    @ModifyConstant(
            method = "renderCustomRecipeList",
            constant = @Constant(intValue = 10),
            remap = false
    )
    private int zitraksmode$twentyRecipeRenderLimit(int original) {
        return PrinterRecipeLimits.MAX_RECIPES_PER_PLAYER;
    }
}