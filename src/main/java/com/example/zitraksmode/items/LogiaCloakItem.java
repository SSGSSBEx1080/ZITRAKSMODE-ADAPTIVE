package com.example.zitraksmode.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.item.GeoArmorItem;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

/** A One Piece cloak which occupies the vanilla chest slot. */
public final class LogiaCloakItem extends GeoArmorItem implements IAnimatable {
    /** Gauge stored only while the cloak is on an armor stand. */
    public static final String ARMOR_STAND_GAUGE_TAG = "ZitraksStandGauge";
    public static final String ARMOR_STAND_INITIALIZED_TAG = "ZitraksStandGaugeInitialized";
    /** Server-written display-only value so the client tooltip shows shared current charge. */
    public static final String TOOLTIP_CHARGE_TAG = "ZitraksTooltipGauge";

    private final LogiaCloakType cloakType;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    public LogiaCloakItem(LogiaCloakType cloakType, Properties properties) {
        super(materialFor(cloakType), EquipmentSlot.CHEST,
                properties.stacksTo(1).rarity(cloakType.rarity).fireResistant());
        this.cloakType = cloakType;
    }

    private static LogiaArmorMaterial materialFor(LogiaCloakType type) {
        return switch (type) {
            case CROCODILE -> LogiaArmorMaterial.CROCODILE;
            case AKAINU, AOKIJI, KIZARU -> LogiaArmorMaterial.ADMIRAL;
            case SENGOKU, WHITEBEARD -> LogiaArmorMaterial.FINAL;
        };
    }

    public LogiaCloakType getCloakType() {
        return cloakType;
    }

    public static boolean isCloak(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LogiaCloakItem;
    }

    @Nullable
    public static LogiaCloakItem fromStack(ItemStack stack) {
        return isCloak(stack) ? (LogiaCloakItem) stack.getItem() : null;
    }

    @Nullable
    public static LogiaCloakItem wornBy(Player player) {
        return fromStack(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.zitraksmode.cloak_" + cloakType.id)
                .withStyle(cloakType.nameColor);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        // Keep the tooltip short. The number is the live shared player gauge,
        // written by the server into this stack; it is not the maximum capacity.
        tooltip.add(Component.translatable(cloakType.logia
                        ? "tooltip.zitraksmode.cloak.logia"
                        : "tooltip.zitraksmode.cloak.absolute")
                .withStyle(ChatFormatting.GRAY));
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TOOLTIP_CHARGE_TAG)) {
            tooltip.add(Component.literal("Заряд: " + tag.getInt(TOOLTIP_CHARGE_TAG))
                    .withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    /*
     * GeckoLib 3.1.31 (the version in this mod) does not use
     * IClientItemExtensions/prepForRender for GeoArmorItem.
     * The renderer is registered once from ClientSetup through
     * GeoArmorRenderer.registerArmorRenderer(...).
     */

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "cloak_idle", 5, this::idlePredicate));
    }

    private <T extends IAnimatable> PlayState idlePredicate(AnimationEvent<T> event) {
        event.getController().setAnimation(new AnimationBuilder()
                .addAnimation("animation.cloak.idle", ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
