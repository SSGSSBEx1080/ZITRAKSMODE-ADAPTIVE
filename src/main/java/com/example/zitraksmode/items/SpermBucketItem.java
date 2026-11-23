package com.example.zitraksmode.items;

import com.example.zitraksmode.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Random;

public class SpermBucketItem extends Item {

    private static final Random RANDOM = new Random();

    public SpermBucketItem() {
        super(new Properties()
                .tab(CreativeModeTab.TAB_MISC)
                .stacksTo(1)
                .craftRemainder(Items.BUCKET)
                .food(new FoodProperties.Builder()
                        .alwaysEat()
                        .nutrition(0)
                        .saturationMod(0)
                        .effect(() -> new MobEffectInstance(MobEffects.POISON, 40, 0), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), 1.0F)
                        .build()
                )
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        
        if (!level.isClientSide && entityLiving instanceof ServerPlayer player) {
            // ✅ ВЫДАЧА АЧИВКИ ЗА ВЫПИВАНИЕ
            Advancement adv = player.getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("zitraksmode", "22_milk/drink_sperm"));
            if (adv != null) {
                player.getAdvancements().award(adv, "drank");
            }

            level.getServer().tell(new net.minecraft.server.TickTask(40, () -> {
                if (player.isAlive()) {
                    applyRandomBuff(player);
                }
            }));
        }

        if (entityLiving instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        entityLiving.gameEvent(GameEvent.DRINK);
        return stack.isEmpty() ? new ItemStack(Items.BUCKET) : stack;
    }

    private void applyRandomBuff(ServerPlayer player) {
        MobEffect[] effects = {
                MobEffects.JUMP,
                MobEffects.DIG_SPEED,
                MobEffects.MOVEMENT_SPEED,
                MobEffects.HERO_OF_THE_VILLAGE
        };
        MobEffect effect = effects[RANDOM.nextInt(effects.length)];
        player.addEffect(new MobEffectInstance(effect, 100, 0));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}