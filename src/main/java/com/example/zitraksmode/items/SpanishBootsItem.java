package com.example.zitraksmode.items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.item.GeoArmorItem;

import java.util.List;
import java.util.UUID;

public class SpanishBootsItem extends GeoArmorItem implements IAnimatable {

    private static final UUID SPEED_UUID = UUID.fromString("1d2e3f40-6a7b-4c8d-9123-456789abcdef");

    private final AnimationFactory factory = new AnimationFactory(this);

    public SpanishBootsItem(Properties properties) {
        super(SpanishBootsMaterial.INSTANCE, EquipmentSlot.FEET, properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <P extends IAnimatable> PlayState predicate(AnimationEvent<P> event) {
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(SPEED_UUID) == null) {
                speed.addTransientModifier(new AttributeModifier(
                        SPEED_UUID,
                        "spanish_boots_speed",
                        1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }

            if (player.tickCount % 200 == 0) {
                player.hurt(DamageSource.MAGIC, 1.0F);
                ((ServerLevel) level).sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 0.1D, player.getZ(),
                        8, 0.25D, 0.05D, 0.25D, 0.0D
                );
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5F, 1.15F);
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 1, false, false, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Испанские сапоги"));
        tooltip.add(Component.literal("§7Скорость бега x2"));
        tooltip.add(Component.literal("§7Прыгучесть"));
        tooltip.add(Component.literal("§cТратят полсердца раз в 10 секунд"));
        tooltip.add(Component.literal("§4Кровавые эффекты"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static void removeSpeedIfNeeded(Player player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_UUID) != null) {
            speed.removeModifier(SPEED_UUID);
        }
    }
}