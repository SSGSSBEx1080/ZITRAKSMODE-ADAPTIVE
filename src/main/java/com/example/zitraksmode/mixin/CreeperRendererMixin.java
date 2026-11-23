package com.example.zitraksmode.mixin;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
abstract class CreeperDataMixin {
    // Добавляем новый синхронизируемый параметр прямо в класс Крипера
    @Unique
    private static final EntityDataAccessor<Boolean> ZITRAKSMODE$IS_CASTED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void zitraksmode$defineData(CallbackInfo ci) {
        ((Creeper)(Object)this).getEntityData().define(ZITRAKSMODE$IS_CASTED, false);
    }

    // Метод для проверки (будет виден в рендерере)
    @Inject(method = "getSwellDir", at = @At("HEAD"), cancellable = true)
    private void zitraksmode$checkCasted(CallbackInfoReturnable<Integer> cir) {
        if (((Creeper)(Object)this).getEntityData().get(ZITRAKSMODE$IS_CASTED)) {
            cir.setReturnValue(-2); // Возвращаем -2 как метку
        }
    }
}

@Mixin(CreeperRenderer.class)
public class CreeperRendererMixin {

    @Unique
    private static final ResourceLocation ZITRAKSMODE$CASTED_TEXTURE = 
        new ResourceLocation(ZitraksMode.MODID, "textures/entity/creeper/casted_creeper.png");

    @Inject(
        method = "getTextureLocation(Lnet/minecraft/world/entity/monster/Creeper;)Lnet/minecraft/resources/ResourceLocation;", 
        at = @At("HEAD"), 
        cancellable = true
    )
    private void zitraksmode$changeCreeperTexture(Creeper entity, CallbackInfoReturnable<ResourceLocation> cir) {
        // Теперь проверяем SwellDir, который мы подменили выше через DataWatcher
        if (entity.getSwellDir() == -2) {
            cir.setReturnValue(ZITRAKSMODE$CASTED_TEXTURE);
        }
    }
}