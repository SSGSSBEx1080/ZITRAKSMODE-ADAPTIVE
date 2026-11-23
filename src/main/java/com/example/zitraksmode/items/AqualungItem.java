package com.example.zitraksmode.items;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
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

public class AqualungItem extends GeoArmorItem implements IAnimatable {

    private static final String LEGACY_AIR_TAG = "AqualungAir";
    private static final String MIGRATED_TAG = "AqualungMigrated";
    private static final String RECHARGE_TIMER_TAG = "AqualungRechargeTimer";

    private final AnimationFactory factory = new AnimationFactory(this);

    public static final int MAX_AIR = 367 * 20; // 7340 тиков = 367 секунд

    private static final UUID SWIM_SPEED_UUID = UUID.fromString("b82f7344-9c4c-4b48-91b6-8f4bb4a76c01");

    public AqualungItem(Properties properties) {
        super(AqualungMaterial.INSTANCE, EquipmentSlot.CHEST, properties.stacksTo(1).rarity(Rarity.RARE));
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
        if (level.isClientSide) {
            return;
        }

        boolean underwater = player.isUnderWater();
        boolean inWater = player.isInWater();

        int air = getAir(stack);

        // Расходуем воздух только под водой
        if (underwater && air > 0) {
            setAir(stack, air - 1);
            air = air - 1;
        }

        // Водохлёбус: на воздухе каждые 2 сек восстанавливает 1 сек дыхания
        if (!underwater) {
            rechargeOnAir(stack);
            air = getAir(stack);
        } else {
            clearRechargeTimer(stack);
        }

        boolean hasAir = air > 0;

        if (underwater && hasAir) {
            player.setAirSupply(player.getMaxAirSupply());
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 0, false, false, true)); // базово Спешка I
            applySwimSpeedModifier(player);
        } else {
            removeSwimSpeedModifier(player);
            player.removeEffect(MobEffects.DIG_SPEED);
            if (underwater) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }

        // Жирный пидор: тянет на дно в воде
        if (getZhirnyPidorLevel(stack) > 0 && inWater) {
            applyZhirnyPidorPull(player);
        }

        // Если воздух кончился — на суше душит, пока не снимет
        if (!hasAir && !underwater) {
            if (level.getGameTime() % 20L == 0L) {
                player.hurt(DamageSource.DROWN, 2.0f);
            }
        }

        // Ачивка за 67 секунд под водой
        String timerKey = "AqualungDeepDiveTime";
        int underwaterTime = player.getPersistentData().getInt(timerKey);

        if (underwater) {
            underwaterTime++;
            player.getPersistentData().putInt(timerKey, underwaterTime);

            if (underwaterTime >= 1340) {
                if (player instanceof ServerPlayer sp) {
                    ZitraksMode.AQUALUNG_DEEP_BREATH.trigger(sp);
                    player.getPersistentData().putInt(timerKey, 0);
                }
            }
        } else if (underwaterTime > 0) {
            player.getPersistentData().putInt(timerKey, 0);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack equipped = player.getItemBySlot(EquipmentSlot.CHEST);
        if (equipped != stack) {
            removeSwimSpeedModifier(player);
            player.removeEffect(MobEffects.DIG_SPEED);
            clearRechargeTimer(stack);
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.IRON_INGOT)
                || repair.is(Items.COPPER_INGOT)
                || repair.is(Items.NAUTILUS_SHELL)
                || super.isValidRepairItem(toRepair, repair);
    }

    private int getVodohlebusLevel(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.VODOHLEBUS.get(), stack);
    }

    private int getZhirnyPidorLevel(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ZHIRNY_PIDOR.get(), stack);
    }

    private void rechargeOnAir(ItemStack stack) {
        if (getVodohlebusLevel(stack) <= 0) {
            clearRechargeTimer(stack);
            return;
        }

        if (getAir(stack) >= MAX_AIR) {
            clearRechargeTimer(stack);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        int timer = tag.getInt(RECHARGE_TIMER_TAG) + 1;

        if (timer >= 40) { // каждые 2 секунды
            timer = 0;
            setAir(stack, Math.min(MAX_AIR, getAir(stack) + 20)); // +1 сек воздуха
        }

        tag.putInt(RECHARGE_TIMER_TAG, timer);
    }

    private void clearRechargeTimer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        tag.remove(RECHARGE_TIMER_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private void applyZhirnyPidorPull(Player player) {
        var motion = player.getDeltaMovement();
        double newY = motion.y - 0.08D;
        if (newY < -0.35D) {
            newY = -0.35D;
        }
        player.setDeltaMovement(motion.x, newY, motion.z);
        player.hurtMarked = true;
    }

    private void applySwimSpeedModifier(Player player) {
        AttributeInstance swimAttr = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swimAttr == null) return;

        if (swimAttr.getModifier(SWIM_SPEED_UUID) == null) {
            AttributeModifier modifier = new AttributeModifier(
                    SWIM_SPEED_UUID,
                    "aqualung_swim_speed",
                    1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            swimAttr.addTransientModifier(modifier);
        }
    }

    private void removeSwimSpeedModifier(Player player) {
        AttributeInstance swimAttr = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swimAttr == null) return;

        if (swimAttr.getModifier(SWIM_SPEED_UUID) != null) {
            swimAttr.removeModifier(SWIM_SPEED_UUID);
        }
    }

    private static void migrateLegacyAirIfNeeded(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(MIGRATED_TAG)) {
            return;
        }

        if (tag.contains(LEGACY_AIR_TAG)) {
            int legacyAir = Math.max(0, Math.min(MAX_AIR, tag.getInt(LEGACY_AIR_TAG)));
            int maxDamage = Math.max(1, stack.getMaxDamage());

            int mappedDamage = maxDamage - Math.max(0, Math.min(maxDamage, legacyAir));
            stack.setDamageValue(Math.max(0, Math.min(maxDamage, mappedDamage)));

            tag.remove(LEGACY_AIR_TAG);
        }

        tag.putBoolean(MIGRATED_TAG, true);
    }

    public static int getAir(ItemStack stack) {
        migrateLegacyAirIfNeeded(stack);

        int maxDamage = Math.max(1, stack.getMaxDamage());
        int remaining = maxDamage - stack.getDamageValue();
        return Math.max(0, Math.min(maxDamage, remaining));
    }

    public static void setAir(ItemStack stack, int air) {
        migrateLegacyAirIfNeeded(stack);

        int maxDamage = Math.max(1, stack.getMaxDamage());
        int clampedAir = Math.max(0, Math.min(maxDamage, air));
        int damage = maxDamage - clampedAir;
        stack.setDamageValue(Math.max(0, Math.min(maxDamage, damage)));
    }

    public static float getAirPercent(ItemStack stack) {
        migrateLegacyAirIfNeeded(stack);

        int maxDamage = Math.max(1, stack.getMaxDamage());
        return (float) getAir(stack) / (float) maxDamage;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int seconds = getAir(stack) / 20;

        tooltip.add(Component.literal("Воздух: " + seconds + " сек")
                .withStyle(seconds > 183 ? ChatFormatting.AQUA :
                        seconds > 91 ? ChatFormatting.YELLOW :
                                ChatFormatting.RED));

        tooltip.add(Component.literal("Чинится: железо, медь, ракушка")
                .withStyle(ChatFormatting.GRAY));

        if (getVodohlebusLevel(stack) > 0) {
            tooltip.add(Component.literal("Водохлёбус: на воздухе +1 сек воздуха каждые 2 сек")
                    .withStyle(ChatFormatting.BLUE));
        }

        if (getZhirnyPidorLevel(stack) > 0) {
            tooltip.add(Component.literal("Жирный пидор: в воде тянет на дно")
                    .withStyle(ChatFormatting.RED));
        }
    }
}