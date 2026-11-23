package com.example.zitraksmode.mixin;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.access.TamedRavagerAccess;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1. Добавляем данные tame-нутости в Равaгера
@Mixin(Ravager.class)
public abstract class RavagerMergedMixin implements TamedRavagerAccess {

    @Unique
    private static final EntityDataAccessor<Boolean> ZITRAKSMODE$IS_TAMED =
            SynchedEntityData.defineId(Ravager.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void zitraksmode$initData(EntityType<? extends Ravager> type, Level level, CallbackInfo ci) {
        ((Ravager)(Object) this).getEntityData().define(ZITRAKSMODE$IS_TAMED, false);
    }

    @Override
    public boolean zitraksmode$isTamed() {
        return ((Ravager)(Object) this).getEntityData().get(ZITRAKSMODE$IS_TAMED);
    }

    @Override
    public void zitraksmode$setTamed(boolean tamed) {
        ((Ravager)(Object) this).getEntityData().set(ZITRAKSMODE$IS_TAMED, tamed);
    }
}

// 2. Меняем текстуру tame-нутного Равaгера
@Mixin(RavagerRenderer.class)
abstract class RavagerRendererMixin {

    @Unique
    private static final ResourceLocation TAMED_TEXTURE =
            new ResourceLocation(ZitraksMode.MODID, "textures/entity/ravager/ravager_tamed.png");

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/world/entity/monster/Ravager;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zitraksmode$changeRavagerTexture(Ravager ravager, CallbackInfoReturnable<ResourceLocation> cir) {
        if (((TamedRavagerAccess) ravager).zitraksmode$isTamed()) {
            cir.setReturnValue(TAMED_TEXTURE);
        }
    }
}

// 3. Ачивка при дойке Равaгера
@Mixin(BucketItem.class)
abstract class BucketItemRavagerMixin {
    @Inject(method = "interactLivingEntity", at = @At("HEAD"))
    private void zitraksmode$onMilkRavager(ItemStack stack, Player player, LivingEntity entity,
                                           InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Проверяем, что это Равагер и что он tame-нут
        if (entity instanceof Ravager ravager && ((TamedRavagerAccess) ravager).zitraksmode$isTamed()) {
            if (!entity.level.isClientSide && player instanceof ServerPlayer sp) {
                // Выдаем ачивку "Дойка Равагера"
                ZitraksMode.MILK_RAVAGER.trigger(sp);
            }
        }
    }
}