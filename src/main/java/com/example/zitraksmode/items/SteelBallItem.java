package com.example.zitraksmode.items;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.entities.SteelBallEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class SteelBallItem extends Item {

    public SteelBallItem(Properties props) {
        super(props);
    }

    // ===== 🔥 ДОБАВЛЯЕМ =====
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    // ===== 🔥 ДОБАВЛЯЕМ (сила зачарований как у алмаза) =====
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 10;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEEL_BALL_CHARGE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int useTicks = this.getUseDuration(stack) - timeLeft;
        boolean ballBreaker = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BALL_BREAKER.get(), stack) > 0;

        float charge = ballBreaker ? 1.0F : getChargePower(useTicks);

        if (charge < 0.15F) {
            return;
        }

        if (!level.isClientSide) {
            boolean isCharged = ballBreaker || useTicks >= 20;

            SteelBallEntity ball = new SteelBallEntity(level, player, isCharged);
            ball.setBallBreaker(ballBreaker);
            ball.setStoredStack(stack.copy());

            float speed = isCharged
                    ? 1.45F + charge * 1.10F
                    : 0.90F + charge * 0.75F;

            if (ballBreaker) {
                speed += 0.35F;
            }

            float inaccuracy = ballBreaker
                    ? 0.0F
                    : Math.max(0.05F, 1.10F - charge * 0.90F);

            ball.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    speed,
                    inaccuracy
            );

            level.addFreshEntity(ball);

            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEEL_BALL_THROW.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    isCharged ? 0.9F : 1.05F
            );

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private float getChargePower(int useTicks) {
        float charge = (float) useTicks / 20.0F;
        charge = (charge * charge + charge * 2.0F) / 3.0F;

        if (charge > 1.0F) {
            charge = 1.0F;
        }

        return charge;
    }
}