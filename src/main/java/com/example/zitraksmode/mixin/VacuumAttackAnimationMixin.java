package com.example.zitraksmode.mixin;

import com.example.zitraksmode.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Holding LMB with the vacuum must render only GeckoLib's suction animation,
 * never vanilla's hand attack/swing bob.
 */
@Mixin(LivingEntity.class)
public abstract class VacuumAttackAnimationMixin {
    @Inject(method = "getAttackAnim", at = @At("HEAD"), cancellable = true)
    private void zitraksmode$hideVacuumSwing(
            float partialTick,
            CallbackInfoReturnable<Float> callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if ((Object) this != minecraft.player) {
            return;
        }

        if (!minecraft.player.getMainHandItem().is(ModItems.VACUUM.get())) {
            return;
        }

        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }

        callback.setReturnValue(0.0F);
    }
}