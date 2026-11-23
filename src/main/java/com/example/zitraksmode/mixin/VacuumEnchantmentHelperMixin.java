package com.example.zitraksmode.mixin;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Makes the enchanting table offer exactly Airuhan when the input item is the vacuum. */
@Mixin(EnchantmentHelper.class)
public abstract class VacuumEnchantmentHelperMixin {
    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
    private static void zitraksmode$vacuumOnlyAiruhan(
            int level,
            ItemStack stack,
            boolean allowTreasure,
            CallbackInfoReturnable<List<EnchantmentInstance>> callback
    ) {
        if (!stack.is(ModItems.VACUUM.get())) {
            return;
        }

        List<EnchantmentInstance> results = callback.getReturnValue();
        results.clear();

        int airuhanLevel = level >= 26 ? 3 : level >= 16 ? 2 : 1;

        results.add(
                new EnchantmentInstance(
                        ModEnchantments.AIRUHAN.get(),
                        airuhanLevel
                )
        );
    }
}