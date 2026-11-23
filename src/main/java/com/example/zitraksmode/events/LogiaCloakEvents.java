package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.items.LabubuType;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LogiaCloakItem;
import com.example.zitraksmode.items.LogiaCloakType;
import com.example.zitraksmode.network.LogiaNetwork;
import com.example.zitraksmode.util.LogiaAdvancementHelper;
import com.example.zitraksmode.util.LogiaGaugeCapability;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

/** All gameplay rules for One Piece cloaks. Server is authoritative. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LogiaCloakEvents {
    private static final UUID WHITEBEARD_HEALTH_UUID = UUID.fromString("8c239a4e-bfbd-4c62-b2bf-a4d3836e3db1");
    private static final AttributeModifier WHITEBEARD_HEALTH = new AttributeModifier(
            WHITEBEARD_HEALTH_UUID, "Whitebeard cloak health", 8.0D,
            AttributeModifier.Operation.ADDITION);

    private static final int KIZARU_COOLDOWN_TICKS = 20 * 60;
    private static final int WHITEBEARD_LAST_STAND_TICKS = 20 * 10;
    /** Starts when last stand triggers; the 10s immunity is part of the 120s total cooldown. */
    private static final int WHITEBEARD_COOLDOWN_TICKS = 20 * 120;
    /** One total 1% roll per ore block broken while Sengoku is worn. */
    private static final float SENGOKU_ORE_BONUS_CHANCE = 0.01F;
    private static final int AOKIJI_ICE_RADIUS = 10;
    private static final String LAST_WORN_CLOAK_TAG = "zitraksmode_last_worn_cloak";

    private static final DustParticleOptions GOLD_LIGHT =
            new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.12F), 1.25F);
    private static final DustParticleOptions WHITE_CRACK =
            new DustParticleOptions(new Vector3f(0.93F, 0.97F, 1.0F), 1.05F);

    private LogiaCloakEvents() {
    }

    // ---------------------------------------------------------------------
    // Player state, recharge and passive abilities
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            data.tick();
            absorbArmorStandCharge(player, data);
            trackObtainedCloaks(player, data);

            LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
            LogiaCloakType wornType = cloak == null ? null : cloak.getCloakType();

            // Used only for Aokiji's one-time "equipped underwater" air chamber.
            CompoundTag persistent = player.getPersistentData();
            String previousCloak = persistent.getString(LAST_WORN_CLOAK_TAG);
            String currentCloak = wornType == null ? "" : wornType.id;
            boolean justEquippedAokiji = wornType == LogiaCloakType.AOKIJI
                    && !LogiaCloakType.AOKIJI.id.equals(previousCloak);
            persistent.putString(LAST_WORN_CLOAK_TAG, currentCloak);

            LogiaCloakType chargingType = wornType != null ? wornType : highestCloakInInventory(player);
            int cap = wornType != null ? wornType.maxGauge
                    : (chargingType == null ? 0 : chargingType.maxGauge);

            if (cap > 0) {
                if (!data.isInitialized()) {
                    data.setGauge(cap); // first found cloak starts charged
                    data.setInitialized(true);
                }
                if (data.getGauge() > cap) data.setGauge(cap);

                // A spent gauge has a strict 5-second lockout. Then it ramps in clear
                // 1-point/second steps: 1, 2, 3... to 5/s (or 10/s for final cloaks).
                // Cloaks merely held in inventory use a quarter of that already-small rate.
                float sourceMultiplier = wornType == null ? 0.25F : 1.0F;
                float regenPerSecond = getGaugeRegenPerSecond(chargingType, data.getTicksSinceSpend());
                data.addRegen(regenPerSecond * sourceMultiplier / 20.0F, cap);
            }

            if (player.tickCount % 5 == 0 || data.isDirty()) {
                updateTooltipCharges(player, data.getGauge());
            }

            updateWhitebeardHealth(player, wornType);
            if (wornType == null) {
                syncIfNeeded(player, data);
                return;
            }

            switch (wornType) {
                case CROCODILE -> { }
                case AKAINU -> player.clearFire();
                case AOKIJI -> {
                    player.setTicksFrozen(0);
                    // First clear the two blocks occupied by the player, then freeze
                    // the whole surrounding sphere without sealing that air pocket.
                    if (justEquippedAokiji && player.isEyeInFluid(FluidTags.WATER)) {
                        createAokijiBubble(player);
                    }
                    tickAokijiIce(player);
                }
                case KIZARU -> { }
                case SENGOKU -> { }
                case WHITEBEARD -> {
                    if (data.getWhitebeardLockTicks() > 0) {
                        if (player.getHealth() != 1.0F) player.setHealth(1.0F);
                        if (player.tickCount % 4 == 0) spawnWhitebeardLastStandParticles(player);
                    }
                }
            }

            // Match AqualungItem's working surface-water test exactly: isInWater()
            // is true even while the player is trying to remain on the surface.
            if (player.isInWater()) applyCloakWaterSink(player);

            // Logia users additionally lose air and take drowning damage when their eyes remain in water.
            if (wornType.logia && player.isEyeInFluid(FluidTags.WATER)) {
                applyLogiaWaterWeakness(player);
            }

            syncIfNeeded(player, data);
        });
    }

    /**
     * 0/s for five seconds after any charge expenditure. Then it increases
     * forever: 2, 4, 6, 8... points per second, adding another +2/s every
     * five seconds without a new gauge expense. There is intentionally no cap.
     */
    private static float getGaugeRegenPerSecond(LogiaCloakType type, int ticksSinceSpend) {
        if (ticksSinceSpend < 20 * 5) return 0.0F;
        long stage = 1L + (ticksSinceSpend - 20L * 5L) / (20L * 5L);
        return stage * 2.0F;
    }

    private static void syncIfNeeded(ServerPlayer player, LogiaGaugeCapability.LogiaGauge data) {
        if (data.isDirty() || player.tickCount % 10 == 0) {
            LogiaNetwork.sync(player, data);
            data.clearDirty();
        }
    }

    @Nullable
    private static LogiaCloakType highestCloakInInventory(Player player) {
        LogiaCloakType result = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            LogiaCloakItem cloak = LogiaCloakItem.fromStack(player.getInventory().getItem(i));
            if (cloak != null && (result == null || cloak.getCloakType().maxGauge > result.maxGauge)) {
                result = cloak.getCloakType();
            }
        }
        return result;
    }

    /** Writes the shared current gauge to each player-owned cloak for its client tooltip. */
    private static void updateTooltipCharges(Player player, int sharedGauge) {
        updateTooltipCharge(player.getItemBySlot(EquipmentSlot.CHEST), sharedGauge);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            updateTooltipCharge(player.getInventory().getItem(i), sharedGauge);
        }
    }

    private static void updateTooltipCharge(ItemStack stack, int sharedGauge) {
        LogiaCloakItem cloak = LogiaCloakItem.fromStack(stack);
        if (cloak == null) return;
        int shownGauge = Math.min(sharedGauge, cloak.getCloakType().maxGauge);
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(LogiaCloakItem.TOOLTIP_CHARGE_TAG)
                || tag.getInt(LogiaCloakItem.TOOLTIP_CHARGE_TAG) != shownGauge) {
            tag.putInt(LogiaCloakItem.TOOLTIP_CHARGE_TAG, shownGauge);
        }
    }

    private static void trackObtainedCloaks(ServerPlayer player, LogiaGaugeCapability.LogiaGauge data) {
        LogiaCloakItem worn = LogiaCloakItem.wornBy(player);
        if (worn != null) recordCloak(player, data, worn.getCloakType());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            LogiaCloakItem cloak = LogiaCloakItem.fromStack(player.getInventory().getItem(i));
            if (cloak != null) recordCloak(player, data, cloak.getCloakType());
        }
    }

    private static void recordCloak(ServerPlayer player, LogiaGaugeCapability.LogiaGauge data,
                                    LogiaCloakType type) {
        if (!data.markCloakObtained(1 << type.ordinal())) return;

        LogiaAdvancementHelper.grant(player, "25_cloaks/" + type.id);
        // Finding the starter cloak immediately reveals every craftable cape recipe.
        if (type == LogiaCloakType.CROCODILE) {
            unlockAllCloakRecipes(player);
        }
        grantAllCloaksIfComplete(player, data);
    }

    /** Works both for new capability data and older worlds whose individual achievements already exist. */
    private static void grantAllCloaksIfComplete(ServerPlayer player, LogiaGaugeCapability.LogiaGauge data) {
        if (data.hasAllCloaks() || hasEveryIndividualCloakAdvancement(player)) {
            LogiaAdvancementHelper.grant(player, "25_cloaks/all_cloaks");
        }
    }

    private static boolean hasEveryIndividualCloakAdvancement(ServerPlayer player) {
        for (LogiaCloakType type : LogiaCloakType.values()) {
            if (!LogiaAdvancementHelper.isDone(player, "25_cloaks/" + type.id)) {
                return false;
            }
        }
        return true;
    }

    /** All recipes except Crocodile itself, which is loot-only. */
    private static void unlockAllCloakRecipes(ServerPlayer player) {
        LogiaAdvancementHelper.unlockRecipes(player,
                "cloak_akainu", "cloak_aokiji", "cloak_kizaru",
                "cloak_sengoku", "cloak_whitebeard");
    }

    private static void updateWhitebeardHealth(Player player, @Nullable LogiaCloakType wornType) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        boolean shouldHaveModifier = wornType == LogiaCloakType.WHITEBEARD;
        boolean hasModifier = maxHealth.getModifier(WHITEBEARD_HEALTH_UUID) != null;
        if (shouldHaveModifier && !hasModifier) {
            maxHealth.addTransientModifier(WHITEBEARD_HEALTH);
        } else if (!shouldHaveModifier && hasModifier) {
            maxHealth.removeModifier(WHITEBEARD_HEALTH_UUID);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Exact proven Aqualung/Zhirny-Pidor movement logic. isInWater() calls this
     * even at the water surface, and hurtMarked forces the server velocity to
     * synchronize to the client immediately.
     */
    private static void applyCloakWaterSink(Player player) {
        Vec3 motion = player.getDeltaMovement();
        double newY = motion.y - 0.08D;
        if (newY < -0.35D) newY = -0.35D;
        player.setDeltaMovement(motion.x, newY, motion.z);
        player.hurtMarked = true;
    }

    private static void applyLogiaWaterWeakness(Player player) {
        player.setAirSupply(0);
        if (player.tickCount % 10 == 0) {
            player.hurt(DamageSource.DROWN, 2.0F);
        }
    }

    /**
     * Ice Age: a real 360-degree sphere, not only the water below the player.
     * This intentionally runs every tick as requested; radius 10 checks at most
     * 4,169 positions inside the sphere, not the entire 21x21x21 cube.
     */
    private static void tickAokijiIce(Player player) {
        Level level = player.level;
        BlockPos center = player.blockPosition();
        int radiusSq = AOKIJI_ICE_RADIUS * AOKIJI_ICE_RADIUS;

        for (int x = -AOKIJI_ICE_RADIUS; x <= AOKIJI_ICE_RADIUS; x++) {
            for (int y = -AOKIJI_ICE_RADIUS; y <= AOKIJI_ICE_RADIUS; y++) {
                for (int z = -AOKIJI_ICE_RADIUS; z <= AOKIJI_ICE_RADIUS; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;
                    BlockPos target = center.offset(x, y, z);
                    // The player's feet and head block stay air when Aokiji was equipped underwater.
                    if (target.equals(center) || target.equals(center.above())) continue;
                    BlockState state = level.getBlockState(target);
                    if (state.getFluidState().is(FluidTags.WATER)) {
                        level.setBlock(target, Blocks.ICE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /** On equipping Aokiji underwater, exactly feet + head become air; Ice Age freezes the rest. */
    private static void createAokijiBubble(Player player) {
        Level level = player.level;
        BlockPos feet = player.blockPosition();
        for (BlockPos pos : new BlockPos[] {feet, feet.above()}) {
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE)
                    || state.is(Blocks.FROSTED_ICE)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Armor stand recharge and charge transfer
    // ---------------------------------------------------------------------

    @SubscribeEvent
    public static void onArmorStandTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || stand.level.isClientSide) return;
        ItemStack stack = stand.getItemBySlot(EquipmentSlot.CHEST);
        LogiaCloakItem cloak = LogiaCloakItem.fromStack(stack);
        if (cloak == null) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean(LogiaCloakItem.ARMOR_STAND_INITIALIZED_TAG)) {
            tag.putBoolean(LogiaCloakItem.ARMOR_STAND_INITIALIZED_TAG, true);
            tag.putInt(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG, 0);
            tag.putInt("ZitraksStandSafeTicks", 0);
            tag.putFloat("ZitraksStandRegen", 0.0F);
        }

        int safeTicks = tag.getInt("ZitraksStandSafeTicks");
        if (safeTicks < Integer.MAX_VALUE) safeTicks++;
        tag.putInt("ZitraksStandSafeTicks", safeTicks);
        // Armor stand has the same delayed staircase as a worn cloak, but charges x2.
        float remainder = tag.getFloat("ZitraksStandRegen")
                + getGaugeRegenPerSecond(cloak.getCloakType(), safeTicks) * 2.0F / 20.0F;
        int increase = (int) remainder;
        tag.putFloat("ZitraksStandRegen", remainder - increase);
        if (increase > 0) {
            tag.putInt(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG,
                    Math.min(cloak.getCloakType().maxGauge,
                            tag.getInt(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG) + increase));
        }
    }

    @SubscribeEvent
    public static void onArmorStandInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof ArmorStand stand)) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        // Before a player puts a cloak onto an empty stand, copy the shared value
        // into its independent stand battery.
        if (LogiaCloakItem.isCloak(held) && stand.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
                LogiaCloakItem cloak = LogiaCloakItem.fromStack(held);
                CompoundTag tag = held.getOrCreateTag();
                tag.putBoolean(LogiaCloakItem.ARMOR_STAND_INITIALIZED_TAG, true);
                tag.putInt(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG,
                        Math.min(data.getGauge(), cloak.getCloakType().maxGauge));
                tag.putInt("ZitraksStandSafeTicks", data.getTicksSinceSpend());
                tag.putFloat("ZitraksStandRegen", 0.0F);
            });
        }

        // Taking a cloak/swap interaction brings its stand charge back to the player's
        // shared pool. The actual stack transfer is performed by vanilla afterwards.
        ItemStack onStand = stand.getItemBySlot(EquipmentSlot.CHEST);
        if (LogiaCloakItem.isCloak(onStand)) {
            player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
                CompoundTag tag = onStand.getTag();
                if (tag != null && tag.getBoolean(LogiaCloakItem.ARMOR_STAND_INITIALIZED_TAG)) {
                    data.setGauge(Math.max(data.getGauge(), tag.getInt(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG)));
                }
            });
        }
    }

    private static void absorbArmorStandCharge(Player player, LogiaGaugeCapability.LogiaGauge data) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            LogiaCloakItem cloak = LogiaCloakItem.fromStack(stack);
            if (cloak == null || !stack.hasTag()) continue;
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.getBoolean(LogiaCloakItem.ARMOR_STAND_INITIALIZED_TAG)) continue;

            data.setGauge(Math.max(data.getGauge(), tag.getInt(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG)));
            tag.remove(LogiaCloakItem.ARMOR_STAND_GAUGE_TAG);
            tag.remove(LogiaCloakItem.ARMOR_STAND_INITIALIZED_TAG);
            tag.remove("ZitraksStandSafeTicks");
            tag.remove("ZitraksStandRegen");
        }
    }

    // ---------------------------------------------------------------------
    // Damage handling
    // ---------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
        if (cloak == null) return;
        LogiaCloakType type = cloak.getCloakType();
        DamageSource source = event.getSource();

        player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            if (type == LogiaCloakType.WHITEBEARD && data.getWhitebeardLockTicks() > 0) {
                event.setCanceled(true);
                return;
            }
            if (isNativePassiveDamage(type, player, source)) {
                event.setCanceled(true);
                return;
            }
            // Sengoku's Buddha protection turns an incoming arrow/projectile around.
            // Reflection uses the normal projectile gauge cost, so it cannot be spammed forever.
            if (type == LogiaCloakType.SENGOKU && reflectProjectile(player, source, data)) {
                event.setCanceled(true);
                return;
            }
            if (isUnblockable(source) || isHakiDamage(source)) return;

            if (!type.logia) {
                int cost = source.isExplosion() ? type.explosionBlockCost
                        : (isProjectile(source) ? type.projectileBlockCost : type.meleeBlockCost);
                if (data.trySpend(cost)) {
                    event.setCanceled(true);
                    spawnDefenseParticles(player, type, source);
                }
                return;
            }

            if (source.isExplosion()) {
                if (data.spendForExplosion(type.explosionBlockCost, player.level.getGameTime())) {
                    spawnDefenseParticles(player, type, source);
                }
                return;
            }

            if (!isLogiaPhaseDamage(source)) return;
            int cost = isProjectile(source) ? type.projectileBlockCost : type.meleeBlockCost;
            if (data.trySpend(cost)) {
                event.setCanceled(true);
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.15D, 1.0D, 0.15D));
                spawnDefenseParticles(player, type, source);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
        if (cloak == null) return;
        LogiaCloakType type = cloak.getCloakType();
        DamageSource source = event.getSource();

        if (isNativePassiveDamage(type, player, source)) {
            event.setCanceled(true);
            return;
        }

        player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            if (type == LogiaCloakType.WHITEBEARD && data.getWhitebeardLockTicks() > 0) {
                event.setCanceled(true);
                player.setHealth(1.0F);
                return;
            }

            if (type.logia && source.isExplosion()
                    && data.consumeExplosionShield(player.level.getGameTime())) {
                event.setAmount(event.getAmount() * type.explosionDamageMul);
                return;
            }

            // Whitebeard's final half-heart: 10 seconds locked at half a heart,
            // then this emergency effect cannot trigger again for 120 seconds total.
            if (type == LogiaCloakType.WHITEBEARD
                    && data.getWhitebeardCooldownTicks() <= 0
                    && !isUnblockable(source)
                    && player.getHealth() - event.getAmount() <= 1.0F) {
                event.setCanceled(true);
                player.setHealth(1.0F);
                data.setWhitebeardLockTicks(WHITEBEARD_LAST_STAND_TICKS);
                data.setWhitebeardCooldownTicks(WHITEBEARD_COOLDOWN_TICKS);
                spawnDefenseParticles(player, type, source);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
        if (cloak == null || cloak.getCloakType() != LogiaCloakType.SENGOKU) return;
        player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            if (data.getGauge() > 0 || data.getWhitebeardLockTicks() > 0) event.setCanceled(true);
        });
    }

    private static boolean isNativePassiveDamage(LogiaCloakType type, Player player, DamageSource source) {
        if (type == LogiaCloakType.AKAINU) {
            return source == DamageSource.IN_FIRE || source == DamageSource.ON_FIRE
                    || source == DamageSource.LAVA || source == DamageSource.HOT_FLOOR;
        }
        if (type == LogiaCloakType.AOKIJI) return source == DamageSource.FREEZE;
        return type == LogiaCloakType.CROCODILE && source == DamageSource.IN_WALL && isInsideSand(player);
    }

    private static boolean isInsideSand(Player player) {
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.above(1))) {
            BlockState state = player.level.getBlockState(pos);
            if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) return true;
        }
        return false;
    }

    private static boolean isUnblockable(DamageSource source) {
        return source == DamageSource.OUT_OF_WORLD;
    }

    private static boolean isLogiaPhaseDamage(DamageSource source) {
        return isProjectile(source) || source.getEntity() instanceof LivingEntity;
    }

    private static boolean isProjectile(DamageSource source) {
        return source.isProjectile() || source.getDirectEntity() instanceof Projectile;
    }

    /** Sengoku's passive: reflect hostile projectiles toward their shooter. */
    private static boolean reflectProjectile(Player player, DamageSource source,
                                             LogiaGaugeCapability.LogiaGauge data) {
        if (!(source.getDirectEntity() instanceof Projectile projectile)) return false;
        if (projectile.getOwner() == player || source.getEntity() == player) return false;
        if (!data.trySpend(LogiaCloakType.SENGOKU.projectileBlockCost)) return false;

        Entity originalShooter = source.getEntity();
        Vec3 direction;
        if (originalShooter instanceof LivingEntity living && living.isAlive()) {
            direction = living.getEyePosition().subtract(projectile.position());
        } else {
            direction = projectile.getDeltaMovement().scale(-1.0D);
        }
        if (direction.lengthSqr() < 0.0001D) direction = player.getLookAngle();

        double speed = Math.max(0.8D, projectile.getDeltaMovement().length() * 1.20D);
        projectile.setDeltaMovement(direction.normalize().scale(speed));
        projectile.setOwner(player);

        if (player.level instanceof ServerLevel level) {
            Vec3 point = projectile.position();
            level.sendParticles(GOLD_LIGHT, point.x, point.y, point.z, 18, 0.22D, 0.22D, 0.22D, 0.04D);
            level.sendParticles(ParticleTypes.WAX_ON, point.x, point.y, point.z, 12, 0.25D, 0.25D, 0.25D, 0.03D);
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.9F, 1.45F);
        }
        return true;
    }

    /** Katana and all netherite tools/items count as Armament Haki. */
    private static boolean isHakiDamage(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity living)) return false;
        ItemStack weapon = living.getMainHandItem();
        if (weapon.is(ModItems.KATANA.get())) return true;
        if (weapon.getItem() instanceof TieredItem tiered && tiered.getTier() == Tiers.NETHERITE) return true;
        return ForgeRegistries.ITEMS.getKey(weapon.getItem()) != null
                && ForgeRegistries.ITEMS.getKey(weapon.getItem()).getPath().contains("netherite");
    }

    /**
     * The visible "attack passed through a Logia" effect. The particles form
     * and immediately disperse at the wearer, which reads as a temporary hole
     * in the elemental body without modifying the player's skin texture.
     */
    private static void spawnDefenseParticles(Player player, LogiaCloakType type, @Nullable DamageSource source) {
        if (!(player.level instanceof ServerLevel level)) return;

        // For arrows/tridents, put the burst exactly at their impact point. It makes
        // the brief elemental gap read as a projectile passing through the body.
        double x = player.getX();
        double y = player.getY() + 1.0D;
        double z = player.getZ();
        if (source != null && source.getDirectEntity() instanceof Projectile projectile) {
            x = projectile.getX();
            y = projectile.getY() + projectile.getBbHeight() * 0.5D;
            z = projectile.getZ();
        }
        switch (type) {
            case CROCODILE -> {
                level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                        x, y, z, 22, 0.48D, 0.65D, 0.48D, 0.06D);
                level.sendParticles(ParticleTypes.POOF, x, y, z, 7, 0.3D, 0.45D, 0.3D, 0.02D);
            }
            case AKAINU -> {
                level.sendParticles(ParticleTypes.LAVA, x, y, z, 13, 0.35D, 0.55D, 0.35D, 0.02D);
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 25, 0.42D, 0.68D, 0.42D, 0.03D);
            }
            case AOKIJI -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 22, 0.45D, 0.65D, 0.45D, 0.02D);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                        x, y, z, 12, 0.35D, 0.55D, 0.35D, 0.04D);
            }
            case KIZARU -> {
                // Yellow Dust + Wax On make actual gold light streaks, unlike white End Rod particles.
                level.sendParticles(GOLD_LIGHT, x, y, z, 28, 0.55D, 0.85D, 0.55D, 0.08D);
                level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 18, 0.48D, 0.7D, 0.48D, 0.05D);
                level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case SENGOKU -> {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 20, 0.55D, 0.85D, 0.55D, 0.04D);
                level.sendParticles(GOLD_LIGHT, x, y, z, 13, 0.45D, 0.75D, 0.45D, 0.03D);
            }
            case WHITEBEARD -> {
                level.sendParticles(WHITE_CRACK, x, y, z, 20, 0.55D, 0.75D, 0.55D, 0.07D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 12, 0.55D, 0.7D, 0.55D, 0.04D);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH,
                SoundSource.PLAYERS, 0.55F, 1.25F);
    }

    /** Whitebeard's ten-second last stand: white quake fissures orbit the player. */
    private static void spawnWhitebeardLastStandParticles(Player player) {
        if (!(player.level instanceof ServerLevel level)) return;
        double y = player.getY() + 1.0D;
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2.0D * i / 8.0D) + player.tickCount * 0.12D;
            double x = player.getX() + Math.cos(angle) * 0.9D;
            double z = player.getZ() + Math.sin(angle) * 0.9D;
            level.sendParticles(WHITE_CRACK, x, y + (i % 3) * 0.25D, z,
                    3, 0.08D, 0.16D, 0.08D, 0.04D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), y, player.getZ(),
                8, 0.6D, 0.8D, 0.6D, 0.03D);
    }

    // ---------------------------------------------------------------------
    // Kizaru: reliable fall event makes water/slime/hay/bed modifiers count
    // ---------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
        if (cloak == null || cloak.getCloakType() != LogiaCloakType.KIZARU) return;

        player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
            if (data.getKizaruCooldown() > 0) return;
            // The event's multiplier already includes hay bales, beds and similar safe landings.
            float predictedDamage = Math.max(0.0F, event.getDistance() - 3.0F) * event.getDamageMultiplier();
            if (predictedDamage < player.getHealth()) return;

            event.setCanceled(true);
            performKizaruDash(player);
            data.setKizaruCooldown(KIZARU_COOLDOWN_TICKS);
        });
    }

    private static void performKizaruDash(Player player) {
        if (!(player.level instanceof ServerLevel level)) return;
        Vec3 direction = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (direction.lengthSqr() < 0.0001D) direction = new Vec3(0.0D, 0.0D, 1.0D);
        direction = direction.normalize();

        Vec3 start = player.position();
        Vec3 destination = start;
        for (int step = 1; step <= 10; step++) {
            Vec3 point = start.add(direction.scale(step));
            BlockPos foot = new BlockPos(point);
            if (isKizaruBarrier(level.getBlockState(foot)) || isKizaruBarrier(level.getBlockState(foot.above()))) break;

            hitKizaruLine(level, player, point, foot);
            destination = point;
        }

        spawnKizaruDashBeams(level, start, destination, direction);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(direction.scale(0.35D));
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 1.15F, 1.85F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.75F, 1.75F);
    }

    /** Long, dense golden lines left in the exact travel direction of Kizaru's fall dash. */
    private static void spawnKizaruDashBeams(ServerLevel level, Vec3 start, Vec3 end, Vec3 direction) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.1D) return;
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x).normalize();

        // Three parallel light rays: center plus two thin side streaks.
        for (int ray = -1; ray <= 1; ray++) {
            Vec3 offset = side.scale(ray * 0.22D);
            for (int i = 0; i <= 48; i++) {
                Vec3 point = start.add(delta.scale(i / 48.0D)).add(offset).add(0.0D, 0.85D, 0.0D);
                level.sendParticles(GOLD_LIGHT, point.x, point.y, point.z,
                        1, 0.025D, 0.025D, 0.025D, 0.015D);
                if (ray == 0 && i % 3 == 0) {
                    level.sendParticles(ParticleTypes.WAX_ON, point.x, point.y, point.z,
                            1, 0.04D, 0.04D, 0.04D, 0.02D);
                }
            }
        }
        level.sendParticles(ParticleTypes.FLASH, end.x, end.y + 0.85D, end.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void hitKizaruLine(ServerLevel level, Player player, Vec3 point, BlockPos foot) {
        AABB box = new AABB(foot).inflate(0.65D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive())) {
            target.hurt(DamageSource.playerAttack(player), 12.0F);
        }
        breakKizaruBlock(level, player, foot);
        breakKizaruBlock(level, player, foot.above());
        level.sendParticles(GOLD_LIGHT, point.x, point.y + 0.9D, point.z,
                12, 0.10D, 0.25D, 0.10D, 0.04D);
        level.sendParticles(ParticleTypes.WAX_ON, point.x, point.y + 0.9D, point.z,
                8, 0.12D, 0.22D, 0.12D, 0.03D);
        level.sendParticles(ParticleTypes.FLASH, point.x, point.y + 0.9D, point.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void breakKizaruBlock(ServerLevel level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !isKizaruBarrier(state)) {
            level.destroyBlock(pos, true, player);
        }
    }

    /** Obsidian, barrier and all unbreakable/command-like blocks stop the light dash. */
    private static boolean isKizaruBarrier(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.BARRIER) || state.is(Blocks.BEDROCK)
                || state.is(Blocks.COMMAND_BLOCK) || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK) || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.JIGSAW);
    }

    // ---------------------------------------------------------------------
    // Sengoku passive, desert loot, persistence
    // ---------------------------------------------------------------------

    /**
     * A single 1% total roll when Sengoku mines any Forge-tagged ore.
     * On success, exactly one of the four bonus items is selected with equal chance.
     */
    @SubscribeEvent
    public static void onSengokuOreBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level.isClientSide || !(player instanceof ServerPlayer)) return;

        LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
        if (cloak == null || cloak.getCloakType() != LogiaCloakType.SENGOKU) return;
        if (!event.getState().is(Tags.Blocks.ORES)) return;
        if (player.getRandom().nextFloat() >= SENGOKU_ORE_BONUS_CHANCE) return;

        ItemStack bonus = switch (player.getRandom().nextInt(4)) {
            case 0 -> new ItemStack(ModItems.LABUBU_FIGURINES.get(LabubuType.BUDDHA).get());
            case 1 -> new ItemStack(Items.GOLDEN_APPLE);
            case 2 -> new ItemStack(Items.GOLDEN_CARROT);
            default -> new ItemStack(Items.COOKIE);
        };

        BlockPos pos = event.getPos();
        ItemEntity drop = new ItemEntity(player.level,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, bonus);
        drop.setDefaultPickUpDelay();
        player.level.addFreshEntity(drop);
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!BuiltInLootTables.DESERT_PYRAMID.equals(event.getName())) return;
        LootPool pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModItems.CLOAK_CROCODILE.get())
                        .when(LootItemRandomChanceCondition.randomChance(0.35F)))
                .build();
        event.getTable().addPool(pool);
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LogiaCloakItem cloak = LogiaCloakItem.fromStack(event.getCrafting());
        if (cloak == null) return;
        player.getCapability(LogiaGaugeCapability.CAPABILITY)
                .ifPresent(data -> recordCloak(player, data, cloak.getCloakType()));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(oldData ->
                event.getEntity().getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(newData ->
                        newData.copyFrom(oldData)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> {
                // Existing worlds: a previously found Crocodile unlocks everything on next login too.
                if (data.hasCloakObtained(1 << LogiaCloakType.CROCODILE.ordinal())
                        || LogiaAdvancementHelper.isDone(player, "25_cloaks/crocodile")) {
                    unlockAllCloakRecipes(player);
                }
                grantAllCloaksIfComplete(player, data);
                LogiaNetwork.sync(player, data);
            });
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(LogiaGaugeCapability.CAPABILITY).ifPresent(data -> LogiaNetwork.sync(player, data));
        }
    }
}
