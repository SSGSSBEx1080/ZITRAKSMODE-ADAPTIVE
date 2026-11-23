package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.access.BullCowAccess;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BullShearHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        Entity target = event.getTarget();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);

        if (!(target instanceof Cow cow)) {
            return;
        }
        if (!(cow instanceof BullCowAccess access) || !access.zitraksmode$isBull()) {
            return;
        }
        if (!held.is(Items.SHEARS)) {
            return;
        }
        if (access.zitraksmode$getBullVariant() != BullCowAccess.VARIANT_NORMAL) {
            return;
        }

        if (!level.isClientSide) {
            boolean copper = RANDOM.nextBoolean();

            if (copper) {
                access.zitraksmode$setBullVariant(BullCowAccess.VARIANT_COPPER);

                if (player instanceof ServerPlayer serverPlayer) {
                    grantAdvancement(serverPlayer, "22_milk/copper_bull");
                }
            } else {
                access.zitraksmode$setBullVariant(BullCowAccess.VARIANT_BALD);
                dropItem(level, cow, new ItemStack(Items.LEATHER, 1 + RANDOM.nextInt(2)));
            }

            held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            level.playSound(null, cow.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }

    private static void grantAdvancement(ServerPlayer player, String advancementPath) {
        if (player.getServer() == null) {
            return;
        }

        Advancement advancement = player.getServer().getAdvancements().getAdvancement(
                new ResourceLocation(ZitraksMode.MODID, advancementPath)
        );

        if (advancement == null) {
            return;
        }

        for (String criterion : advancement.getCriteria().keySet()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static void dropItem(Level level, Cow cow, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity entity = new ItemEntity(level, cow.getX(), cow.getY(0.6D), cow.getZ(), stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}