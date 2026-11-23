package com.example.zitraksmode.mixin;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.access.BullCowAccess;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
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

@Mixin(Cow.class)
public abstract class CowMergedMixin implements BullCowAccess {

    @Unique
    private static final EntityDataAccessor<Boolean> ZITRAKSMODE$IS_BULL =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private static final EntityDataAccessor<Integer> ZITRAKSMODE$BULL_VARIANT =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.INT);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void zitraksmode$initBullData(EntityType<? extends Cow> type, Level level, CallbackInfo ci) {
        Cow cow = (Cow) (Object) this;
        cow.getEntityData().define(ZITRAKSMODE$IS_BULL, false);
        cow.getEntityData().define(ZITRAKSMODE$BULL_VARIANT, VARIANT_NORMAL);
    }

    @Override
    public boolean zitraksmode$isBull() {
        return ((Cow) (Object) this).getEntityData().get(ZITRAKSMODE$IS_BULL);
    }

    @Override
    public void zitraksmode$setBull(boolean bull) {
        Cow cow = (Cow) (Object) this;
        cow.getEntityData().set(ZITRAKSMODE$IS_BULL, bull);
        if (!bull) {
            cow.getEntityData().set(ZITRAKSMODE$BULL_VARIANT, VARIANT_NORMAL);
        }
    }

    @Override
    public int zitraksmode$getBullVariant() {
        return ((Cow) (Object) this).getEntityData().get(ZITRAKSMODE$BULL_VARIANT);
    }

    @Override
    public void zitraksmode$setBullVariant(int variant) {
        int safe = switch (variant) {
            case VARIANT_BALD, VARIANT_COPPER -> variant;
            default -> VARIANT_NORMAL;
        };
        ((Cow) (Object) this).getEntityData().set(ZITRAKSMODE$BULL_VARIANT, safe);
    }
}

@Mixin(CowRenderer.class)
abstract class CowRendererMixin {

    @Unique
    private static final ResourceLocation BULL_TEXTURE =
            new ResourceLocation(ZitraksMode.MODID, "textures/entity/cow/cow_bull.png");

    @Unique
    private static final ResourceLocation BALD_BULL_TEXTURE =
            new ResourceLocation(ZitraksMode.MODID, "textures/entity/cow/cow_bull_bald.png");

    @Unique
    private static final ResourceLocation COPPER_BULL_TEXTURE =
            new ResourceLocation(ZitraksMode.MODID, "textures/entity/cow/cow_bull_copper.png");

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Cow;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zitraksmode$changeCowTexture(Cow cow, CallbackInfoReturnable<ResourceLocation> cir) {
        BullCowAccess access = (BullCowAccess) cow;
        if (!access.zitraksmode$isBull()) {
            return;
        }

        int variant = access.zitraksmode$getBullVariant();
        if (variant == BullCowAccess.VARIANT_BALD) {
            cir.setReturnValue(BALD_BULL_TEXTURE);
        } else if (variant == BullCowAccess.VARIANT_COPPER) {
            cir.setReturnValue(COPPER_BULL_TEXTURE);
        } else {
            cir.setReturnValue(BULL_TEXTURE);
        }
    }
}

@Mixin(BucketItem.class)
abstract class BucketItemBullMixin {

    @Inject(method = "interactLivingEntity", at = @At("HEAD"))
    private void zitraksmode$onMilkBull(ItemStack stack, Player player, LivingEntity entity,
                                        InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof Cow cow && ((BullCowAccess) cow).zitraksmode$isBull()) {
            if (!entity.level.isClientSide && player instanceof ServerPlayer sp) {
                ZitraksMode.MILK_BULL.trigger(sp);
            }
        }
    }
}