package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.access.BullCowAccess;
import com.example.zitraksmode.access.TamedRavagerAccess;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = "zitraksmode", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CowMilkingHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ✅ Пути к ачивкам (совпадают с папкой 22_milk)
    private static final ResourceLocation BULL_ADV_ID = new ResourceLocation("zitraksmode", "22_milk/bull_milking");
    private static final ResourceLocation RAVAGER_ADV_ID = new ResourceLocation("zitraksmode", "22_milk/hero_of_village");

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (held.getItem() != Items.BUCKET) return;

        if (event.getTarget() instanceof Cow cow) {
            event.setCanceled(true);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                handleCowMilking(serverPlayer, cow);
            }
        } else if (event.getTarget() instanceof Ravager ravager) {
            event.setCanceled(true);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                handleRavagerMilking(serverPlayer, ravager);
            }
        }
    }

    private static void handleCowMilking(ServerPlayer player, Cow cow) {
        if (!(cow instanceof BullCowAccess access)) {
            LOGGER.error("[CowMilking] Cow does not implement BullCowAccess");
            return;
        }

        boolean isBull;
        if (!cow.getPersistentData().contains("IsBull")) {
            isBull = Math.random() < 0.5;
            cow.getPersistentData().putBoolean("IsBull", isBull);
            LOGGER.info("[CowMilking] Assigned gender: {}", isBull ? "Bull" : "Cow");
        } else {
            isBull = cow.getPersistentData().getBoolean("IsBull");
        }

        access.zitraksmode$setBull(isBull);
        player.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);

        if (isBull) {
            player.getInventory().add(new ItemStack(ModItems.SPERM_BUCKET.get()));
            awardAdvancement(player, BULL_ADV_ID); // 🏆 Выдаём ачивку за быка
            LOGGER.info("[CowMilking] Bull milked, gave sperm bucket");
        } else {
            player.getInventory().add(new ItemStack(Items.MILK_BUCKET));
            LOGGER.info("[CowMilking] Cow milked, gave milk bucket");
        }

        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
    }

    private static void handleRavagerMilking(ServerPlayer player, Ravager ravager) {
        if (!(ravager instanceof TamedRavagerAccess access)) {
            LOGGER.error("[CowMilking] Ravager does not implement TamedRavagerAccess");
            return;
        }

        if (ravager.getPersistentData().getBoolean("Tamed")) {
            LOGGER.info("[CowMilking] Ravager already tamed");
            return;
        }

        ravager.getPersistentData().putBoolean("Tamed", true);
        access.zitraksmode$setTamed(true);

        ravager.setTarget(null);
        ravager.targetSelector.removeAllGoals();
        ravager.goalSelector.removeAllGoals();
        ravager.goalSelector.addGoal(0, new FloatGoal(ravager));
        ravager.goalSelector.addGoal(1, new RandomStrollGoal(ravager, 0.4));
        ravager.goalSelector.addGoal(2, new LookAtPlayerGoal(ravager, Player.class, 6.0F));
        ravager.goalSelector.addGoal(3, new RandomLookAroundGoal(ravager));

        player.getItemInHand(InteractionHand.MAIN_HAND).shrink(1);
        player.getInventory().add(new ItemStack(ModItems.SPERM_BUCKET.get()));
        awardAdvancement(player, RAVAGER_ADV_ID); // 🏆 Выдаём ачивку за равагера

        ravager.playSound(SoundEvents.COW_MILK, 1.0F, 0.8F);
        LOGGER.info("[CowMilking] Ravager tamed!");
    }

    @SubscribeEvent
    public void onEntityLoad(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof Cow cow && cow instanceof BullCowAccess access) {
            if (cow.getPersistentData().contains("IsBull")) {
                access.zitraksmode$setBull(cow.getPersistentData().getBoolean("IsBull"));
            }
        }

        if (event.getEntity() instanceof Ravager ravager && ravager instanceof TamedRavagerAccess access) {
            if (ravager.getPersistentData().contains("Tamed") && ravager.getPersistentData().getBoolean("Tamed")) {
                access.zitraksmode$setTamed(true);
                tameRavager(ravager);
            }
        }
    }

    private static void tameRavager(Ravager ravager) {
        ravager.setTarget(null);
        ravager.targetSelector.removeAllGoals();
        ravager.goalSelector.removeAllGoals();
        ravager.goalSelector.addGoal(0, new FloatGoal(ravager));
        ravager.goalSelector.addGoal(1, new RandomStrollGoal(ravager, 0.4));
        ravager.goalSelector.addGoal(2, new LookAtPlayerGoal(ravager, Player.class, 6.0F));
        ravager.goalSelector.addGoal(3, new RandomLookAroundGoal(ravager));
    }

    /**
     * ✅ Универсальный метод выдачи ачивки.
     * Идеально работает с JSON-триггером "minecraft:impossible"
     */
    private static void awardAdvancement(ServerPlayer player, ResourceLocation advId) {
        if (player.getServer() == null) return;

        Advancement adv = player.getServer().getAdvancements().getAdvancement(advId);
        if (adv == null) {
            LOGGER.warn("[Advancement] Not found: {}", advId);
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(adv, criterion);
            }
            LOGGER.info("[Advancement] Awarded {} to {}", advId, player.getName().getString());
        }
    }
}