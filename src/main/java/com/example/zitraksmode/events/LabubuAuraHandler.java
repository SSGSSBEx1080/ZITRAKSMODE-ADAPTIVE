package com.example.zitraksmode.events;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LabubuType;
import com.example.zitraksmode.items.LogiaCloakItem;
import com.example.zitraksmode.items.LogiaCloakType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LabubuAuraHandler {
    private static final int AURA_RADIUS = 6;
    private static final int ANONYMOUS_AURA_RADIUS = 3;
    private static final int BUDDHA_AURA_RADIUS = 8;
    private static final int RESCAN_INTERVAL = 2;
    private static final int EFFECT_DURATION = 10;
    private static final int ANGEL_COOLDOWN_TICKS = 20 * 180;
    private static final int ANIME_CYCLE_TICKS = 100;
    private static final int ANIME_EFFECT_DURATION = 400;
    private static final int ANIME_EFFECT_COUNT = 5;

    private static final UUID MOVE_SPEED_UUID = uuid("labubu_move_speed");
    private static final UUID ATTACK_SPEED_UUID = uuid("labubu_attack_speed");
    private static final UUID MAX_HEALTH_UUID = uuid("labubu_max_health");
    private static final UUID ARMOR_UUID = uuid("labubu_armor");
    private static final UUID ARMOR_TOUGHNESS_UUID = uuid("labubu_armor_toughness");
    private static final UUID KNOCKBACK_RESIST_UUID = uuid("labubu_knockback_resist");

    private static final List<MobEffect> ANIME_POSITIVE_EFFECTS = List.of(
            MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE, MobEffects.JUMP, MobEffects.DIG_SPEED,
            MobEffects.FIRE_RESISTANCE, MobEffects.HEALTH_BOOST, MobEffects.ABSORPTION,
            MobEffects.LUCK, MobEffects.NIGHT_VISION
    );

    private static final Map<UUID, AuraSnapshot> SNAPSHOT_CACHE = new HashMap<>();
    private static final Map<UUID, Long> ANGEL_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, AnimeState> ANIME_STATES = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) return;

        Player player = event.player;
        AuraSnapshot snapshot = getSnapshot(player);
        removeAllLabubuModifiers(player);

        if (snapshot.isEmpty() && !hasAnonymousAura(player)) {
            clearAnimeEffects(player);
            return;
        }

        if (snapshot.count(LabubuType.ANIME) <= 0) clearAnimeEffects(player);
        applyPassiveBuffs(player, snapshot);
        applyAttributeBuffs(player, snapshot);
        applyWorldEffects(player, snapshot);
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        int blue = getSnapshot(player).count(LabubuType.BLUE);
        if (blue <= 0) return;

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0.42D + 0.24D * blue, motion.z);
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        AuraSnapshot snapshot = getSnapshot(player);
        Block block = event.getState().getBlock();
        boolean sengoku = wearsSengokuCloak(player);

        // Коран для всех; плащ Сэнгоку — рукой или любым предметом.
        if (isBuddhaLabubuBlock(block) && (isHoldingKoran(player) || sengoku)) {
            event.setNewSpeed(999999.0F);
            return;
        }

        float multiplier = 1.0F + 0.10F * snapshot.count(LabubuType.PURPLE);
        if (!sengoku && isInsideBuddhaAura(player.level, player.blockPosition())) {
            multiplier = 0.0F;
        }
        event.setNewSpeed(event.getOriginalSpeed() * multiplier);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level.isClientSide) return;

        BlockPos pos = event.getPos();
        Block block = event.getState().getBlock();
        boolean sengoku = wearsSengokuCloak(player);

        // Сэнгоку ломает Будду чем угодно, но получает фигурку, а не сам блок.
        if (isBuddhaLabubuBlock(block) && (isHoldingKoran(player) || sengoku)) {
            event.setCanceled(true);
            ItemStack drop = getLabubuFigurineDrop(block);
            if (!drop.isEmpty()) Block.popResource(player.level, pos, drop);
            player.level.destroyBlock(pos, false);
            return;
        }

        // Будда-аура не запрещает Сэнгоку ломать обычные блоки.
        if (!sengoku && isInsideBuddhaAura(player.level, player.blockPosition()) && !isLabubuBlock(block)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        ItemStack stack = event.getItem();
        if (stack.isEmpty() || !stack.isEdible()) return;

        AuraSnapshot snapshot = getSnapshot(player);
        int extraTicks = 0;
        int russian = snapshot.count(LabubuType.RUSSIAN);
        if (russian > 0) extraTicks += 2 + russian;
        int ukrainian = snapshot.count(LabubuType.UKRAINIAN);
        if (ukrainian > 0 && isPork(stack.getItem())) extraTicks += 4 * ukrainian;
        if (extraTicks > 0) event.setDuration(Math.max(1, event.getDuration() - extraTicks));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();

        // Buddha aura normally cancels every fight in its radius. Sengoku bypasses it
        // whether he is the victim or the attacker.
        boolean sengokuInvolved = wearsSengokuCloak(victim) || wearsSengokuCloak(attackerEntity);
        if (!sengokuInvolved && isInsideBuddhaAura(victim.level, victim.blockPosition())) {
            event.setCanceled(true);
            return;
        }
        if (attackerEntity instanceof LivingEntity attackerLiving
                && !sengokuInvolved
                && isInsideBuddhaAura(attackerLiving.level, attackerLiving.blockPosition())) {
            event.setCanceled(true);
            return;
        }

        if (attackerEntity instanceof Player attacker) {
            AuraSnapshot attackAura = getSnapshot(attacker);
            float amount = event.getAmount();
            amount += attackAura.count(LabubuType.RED);
            amount *= 1.0F + 0.05F * attackAura.count(LabubuType.BLACK);
            amount *= 1.0F + 0.05F * attackAura.count(LabubuType.BLACK_WHITE);
            if (attacker.level.isNight()) amount *= 1.0F + 0.10F * attackAura.count(LabubuType.STRIPED);
            amount *= 1.0F + 0.50F * attackAura.count(LabubuType.RUSSIAN);
            if (attacker.isShiftKeyDown()) amount *= 1.0F + 0.75F * attackAura.count(LabubuType.LITVIN);
            if (victim instanceof AbstractHorse) amount *= 1.0F + attackAura.count(LabubuType.KAZAKH);
            if (victim instanceof Pig) amount *= 1.0F + attackAura.count(LabubuType.UKRAINIAN);
            if (victim.isOnFire() || victim.isInLava()) amount *= 1.0F + attackAura.count(LabubuType.DEMON);
            event.setAmount(amount);
        }

        if (victim instanceof Player victimPlayer) {
            AuraSnapshot defenseAura = getSnapshot(victimPlayer);
            float multiplier = 1.0F;
            multiplier *= Math.max(0.0F, 1.0F - 0.06F * defenseAura.count(LabubuType.WHITE));
            multiplier *= Math.max(0.0F, 1.0F - 0.06F * defenseAura.count(LabubuType.BLACK_WHITE));
            if (victimPlayer.level.isDay()) {
                multiplier *= Math.max(0.0F, 1.0F - 0.10F * defenseAura.count(LabubuType.STRIPED));
            }
            if (defenseAura.count(LabubuType.ANGEL) > 0) multiplier *= 0.50F;
            multiplier *= Math.max(0.1F, 1.0F - getTopHatEnchantProtection(victimPlayer, defenseAura));
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level.isClientSide) return;
        int angel = getSnapshot(player).count(LabubuType.ANGEL);
        if (angel <= 0) return;

        long now = player.level.getGameTime();
        if (now < ANGEL_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) return;

        event.setCanceled(true);
        player.setHealth(Math.min(player.getMaxHealth(), 8.0F + angel * 2.0F));
        player.clearFire();
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2, false, false, true));
        ANGEL_COOLDOWNS.put(player.getUUID(), now + ANGEL_COOLDOWN_TICKS);
        player.level.broadcastEntityEvent(player, (byte) 35);
    }

    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile projectile)) return;
        if (projectile.getOwner() instanceof Player player) {
            int faceless = getSnapshot(player).count(LabubuType.FASELESS);
            if (faceless > 0) projectile.setDeltaMovement(projectile.getDeltaMovement().scale(1.0D + 5.55D * faceless));
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (event.getHookEntity() == null) return;
        Player player = event.getHookEntity().getPlayerOwner();
        if (player == null || player.level.isClientSide || getSnapshot(player).count(LabubuType.JAPANESE) <= 0) return;

        List<ItemStack> extraDrops = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) extraDrops.add(drop.copy());
        event.getDrops().addAll(extraDrops);
    }

    @SubscribeEvent
    public static void onMonsterTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Monster) || !(event.getNewTarget() instanceof Player player)) return;
        if (hasAnonymousAura(player)) event.setNewTarget(null);
    }

    // ============================================================
    // AURA SCANNING
    // ============================================================

    private static AuraSnapshot getSnapshot(Player player) {
        AuraSnapshot cached = SNAPSHOT_CACHE.get(player.getUUID());
        long now = player.level.getGameTime();
        if (cached != null && now - cached.lastScanTick <= RESCAN_INTERVAL) return cached;

        AuraSnapshot scanned = scanNearby(player);
        SNAPSHOT_CACHE.put(player.getUUID(), scanned);
        return scanned;
    }

    private static AuraSnapshot scanNearby(Player player) {
        AuraSnapshot snapshot = new AuraSnapshot(player.level.getGameTime());
        BlockPos center = player.blockPosition();
        for (int dx = -AURA_RADIUS; dx <= AURA_RADIUS; dx++) {
            for (int dy = -AURA_RADIUS; dy <= AURA_RADIUS; dy++) {
                for (int dz = -AURA_RADIUS; dz <= AURA_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    LabubuType type = getTypeForBlock(player.level.getBlockState(pos).getBlock());
                    if (type != null) snapshot.add(type, pos.immutable());
                }
            }
        }
        return snapshot;
    }

    @Nullable
    private static LabubuType getTypeForBlock(Block block) {
        for (Map.Entry<LabubuType, RegistryObject<Block>> entry : ModBlocks.LABUBU_FIGURINE_BLOCKS.entrySet()) {
            if (entry.getValue().get() == block) return entry.getKey();
        }
        return null;
    }

    // ============================================================
    // PASSIVE BUFFS / ANIME
    // ============================================================

    private static void applyPassiveBuffs(Player player, AuraSnapshot snapshot) {
        int green = snapshot.count(LabubuType.GREEN);
        if (green > 0 && player.tickCount % 100 == 0) player.heal(green);
        if (snapshot.count(LabubuType.NEON) > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false, true));
        }
        if (snapshot.count(LabubuType.DEMON) > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, false, false, true));
            player.clearFire();
        }
        if (hasAnonymousAura(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_DURATION, 0, false, false, true));
            clearHostileTargetsAroundPlayer(player, 64.0D);
        }
        if (snapshot.count(LabubuType.JAPANESE) > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, EFFECT_DURATION, 1, false, false, true));
        }
        if (snapshot.count(LabubuType.BOOBA) > 0 && player.tickCount % 200 == 0) {
            player.spawnAtLocation(new ItemStack(ModItems.SPERM_BUCKET.get()));
        }
        if (snapshot.count(LabubuType.LITVIN) > 0 && player.isShiftKeyDown() && player.tickCount % 200 == 0) {
            player.hurt(DamageSource.MAGIC, 1.0F);
        }
        if (snapshot.count(LabubuType.ANIME) > 0) applyAnimeEffects(player, snapshot.count(LabubuType.ANIME));
    }

    private static void applyAnimeEffects(Player player, int count) {
        AnimeState state = ANIME_STATES.get(player.getUUID());
        long now = player.level.getGameTime();
        if (state == null || now >= state.nextRollTick) {
            if (state != null) for (MobEffect old : state.effects) player.removeEffect(old);
            List<MobEffect> pool = new ArrayList<>(ANIME_POSITIVE_EFFECTS);
            Collections.shuffle(pool, new java.util.Random(now + player.getId()));
            state = new AnimeState(new ArrayList<>(pool.subList(0, Math.min(ANIME_EFFECT_COUNT, pool.size()))),
                    now + ANIME_CYCLE_TICKS);
            ANIME_STATES.put(player.getUUID(), state);
        }

        int amplifier = Math.max(0, count - 1);
        for (MobEffect effect : state.effects) {
            player.addEffect(new MobEffectInstance(effect, ANIME_EFFECT_DURATION, amplifier, false, false, true));
        }
    }

    private static void clearAnimeEffects(Player player) {
        AnimeState state = ANIME_STATES.remove(player.getUUID());
        if (state != null) for (MobEffect effect : state.effects) player.removeEffect(effect);
    }

    // ============================================================
    // ATTRIBUTE BUFFS
    // ============================================================

    private static void applyAttributeBuffs(Player player, AuraSnapshot snapshot) {
        removeAllLabubuModifiers(player);
        double moveSpeedMul = 0.07D * snapshot.count(LabubuType.YELLOW)
                + 0.25D * snapshot.count(LabubuType.ANGEL);
        double attackSpeedMul = player.isShiftKeyDown() ? 0.50D * snapshot.count(LabubuType.LITVIN) : 0.0D;
        double armorAdd = 2.0D * snapshot.count(LabubuType.ANGEL);
        double armorToughnessAdd = 0.0D;

        if (snapshot.count(LabubuType.TOP_HAT) > 0) {
            HelmetBonus bonus = getHelmetBonus(player);
            armorAdd += bonus.armor * 0.75D * snapshot.count(LabubuType.TOP_HAT);
            armorToughnessAdd += bonus.toughness * 0.75D * snapshot.count(LabubuType.TOP_HAT);
        }

        applyModifier(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_UUID, "labubu_move_speed", moveSpeedMul, AttributeModifier.Operation.MULTIPLY_TOTAL);
        applyModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID, "labubu_attack_speed", attackSpeedMul, AttributeModifier.Operation.MULTIPLY_TOTAL);
        applyModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_UUID, "labubu_max_health", 0.0D, AttributeModifier.Operation.ADDITION);
        applyModifier(player, Attributes.ARMOR, ARMOR_UUID, "labubu_armor", armorAdd, AttributeModifier.Operation.ADDITION);
        applyModifier(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID, "labubu_armor_toughness", armorToughnessAdd, AttributeModifier.Operation.ADDITION);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_UUID, "labubu_knockback_resist", 0.0D, AttributeModifier.Operation.ADDITION);
    }

    // ============================================================
    // WORLD EFFECTS
    // ============================================================

    private static void applyWorldEffects(Player player, AuraSnapshot snapshot) {
        int kazakh = snapshot.count(LabubuType.KAZAKH);
        if (player.getVehicle() instanceof AbstractHorse horse && kazakh > 0) {
            horse.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 1, false, false, true));
        }
        int ukrainian = snapshot.count(LabubuType.UKRAINIAN);
        if (player.getVehicle() instanceof Pig pig && ukrainian > 0) {
            pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 12, false, false, true));
        }
        if (snapshot.count(LabubuType.DEMON) > 0 && player.tickCount % 20 == 0) {
            player.level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(AURA_RADIUS), LivingEntity::isAlive)
                    .forEach(mob -> mob.setSecondsOnFire(3));
        }
        if (snapshot.count(LabubuType.FASELESS) > 0) slowEnemyProjectiles(player, snapshot.count(LabubuType.FASELESS));
        for (BlockPos pos : snapshot.positionsOf(LabubuType.GALACTIC)) galacticPullAndRepel(player, pos);
        if (player.tickCount % 20 == 0) {
            for (BlockPos pos : snapshot.positionsOf(LabubuType.RAINBOW)) breedAnimalsAround(player, pos);
        }

        // The wearer of Sengoku does not trigger Buddha pacification from their own scan.
        if (player.tickCount % 20 == 0 && !wearsSengokuCloak(player)) {
            for (BlockPos pos : snapshot.positionsOf(LabubuType.BUDDHA)) buddhaPacify(player, pos);
        }
        if (player.tickCount % 200 == 0) {
            for (BlockPos pos : snapshot.positionsOf(LabubuType.GOLDEN)) {
                transmuteGroundItems(player, pos, Math.max(1, snapshot.count(LabubuType.GOLDEN)));
            }
        }
        if (player.tickCount % 200 == 0 && snapshot.count(LabubuType.BOOBA) > 0) {
            for (BlockPos pos : snapshot.positionsOf(LabubuType.BOOBA)) villagerLoveParticles(player, pos);
        }
    }

    private static void slowEnemyProjectiles(Player player, int facelessCount) {
        player.level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(AURA_RADIUS + 4.0D), projectile ->
                projectile.getOwner() != player && projectile.isAlive()).forEach(projectile ->
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(Math.max(0.05D, 1.0D - 0.555D * facelessCount))));
    }

    private static void galacticPullAndRepel(Player player, BlockPos pos) {
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;
        double radius = 10.0D;

        AABB itemBox = new AABB(px - radius, py - 4.0D, pz - radius, px + radius, py + 4.0D, pz + radius);
        player.level.getEntitiesOfClass(ItemEntity.class, itemBox, Entity::isAlive).forEach(item -> {
            Vec3 offset = new Vec3(px - item.getX(), py - item.getY(), pz - item.getZ());
            double distance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            if (distance < 0.001D || distance > radius) return;
            double strength = 0.01D + (1.0D - distance / radius) * 0.08D;
            Vec3 radial = new Vec3(offset.x, 0.0D, offset.z).normalize();
            Vec3 clockwise = new Vec3(radial.z, 0.0D, -radial.x).scale(strength * 0.35D);
            item.setDeltaMovement(item.getDeltaMovement().add(radial.scale(strength).add(clockwise)));
        });

        AABB mobBox = new AABB(px - radius, py - 3.0D, pz - radius, px + radius, py + 3.0D, pz + radius);
        player.level.getEntitiesOfClass(Monster.class, mobBox, LivingEntity::isAlive).forEach(mob -> {
            Vec3 offset = new Vec3(mob.getX() - px, 0.0D, mob.getZ() - pz);
            double distance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            if (distance < 0.001D || distance > radius) return;
            double strength = 0.015D + (1.0D - distance / radius) * 0.10D;
            Vec3 radial = offset.normalize();
            Vec3 clockwise = new Vec3(radial.z, 0.0D, -radial.x).scale(strength * 0.25D);
            mob.setDeltaMovement(mob.getDeltaMovement().add(radial.scale(strength).add(clockwise)));
        });
    }

    private static void breedAnimalsAround(Player player, BlockPos pos) {
        AABB box = new AABB(pos.getX() - 6.0D, pos.getY() - 3.0D, pos.getZ() - 6.0D,
                pos.getX() + 6.0D, pos.getY() + 3.0D, pos.getZ() + 6.0D);
        player.level.getEntitiesOfClass(Animal.class, box, animal -> animal.isAlive() && animal.getAge() == 0)
                .forEach(animal -> {
                    if (!animal.isInLove()) animal.setInLove(null);
                });
    }

    private static void transmuteGroundItems(Player player, BlockPos pos, int goldenCount) {
        AABB box = new AABB(pos.getX() - 4.0D, pos.getY() - 2.0D, pos.getZ() - 4.0D,
                pos.getX() + 4.0D, pos.getY() + 2.0D, pos.getZ() + 4.0D);
        int changed = 0;
        for (ItemEntity itemEntity : player.level.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive)) {
            ItemStack stack = itemEntity.getItem();
            Item result = stack.is(Items.APPLE) ? Items.GOLDEN_APPLE
                    : stack.is(Items.CARROT) ? Items.GOLDEN_CARROT : null;
            if (result == null) continue;

            int convert = Math.min(stack.getCount(), Math.max(1, goldenCount));
            stack.shrink(convert);
            player.level.addFreshEntity(new ItemEntity(player.level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                    new ItemStack(result, convert)));
            changed++;
            if (stack.isEmpty()) itemEntity.discard();
            if (changed >= goldenCount) break;
        }
    }

    private static void buddhaPacify(Player player, BlockPos pos) {
        AABB box = new AABB(pos.getX() - BUDDHA_AURA_RADIUS, pos.getY() - 4.0D, pos.getZ() - BUDDHA_AURA_RADIUS,
                pos.getX() + BUDDHA_AURA_RADIUS, pos.getY() + 4.0D, pos.getZ() + BUDDHA_AURA_RADIUS);
        player.level.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive).forEach(mob -> {
            // Buddha does not pacify/disarm a monster actively fighting a Sengoku wearer.
            if (wearsSengokuCloak(mob.getTarget())) return;
            mob.setTarget(null);
            if (!mob.getMainHandItem().isEmpty()) {
                ItemStack dropped = mob.getMainHandItem().copy();
                mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                mob.spawnAtLocation(dropped);
            }
        });
    }

    private static void villagerLoveParticles(Player player, BlockPos pos) {
        if (!(player.level instanceof ServerLevel serverLevel)) return;
        AABB box = new AABB(pos.getX() - 6.0D, pos.getY() - 3.0D, pos.getZ() - 6.0D,
                pos.getX() + 6.0D, pos.getY() + 3.0D, pos.getZ() + 6.0D);
        serverLevel.getEntitiesOfClass(Villager.class, box, LivingEntity::isAlive).forEach(villager ->
                serverLevel.sendParticles(ParticleTypes.HEART, villager.getX(), villager.getY() + 1.2D, villager.getZ(),
                        3, 0.2D, 0.2D, 0.2D, 0.0D));
    }

    // ============================================================
    // UTILS
    // ============================================================

    private static float getTopHatEnchantProtection(Player player, AuraSnapshot snapshot) {
        int topHat = snapshot.count(LabubuType.TOP_HAT);
        if (topHat <= 0) return 0.0F;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return 0.0F;

        int prot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, helmet);
        int blast = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, helmet);
        int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, helmet);
        int projectile = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, helmet);
        return (prot + blast + fire + projectile) * 0.04F * 0.75F * topHat;
    }

    private static ItemStack getLabubuFigurineDrop(Block block) {
        for (Map.Entry<LabubuType, RegistryObject<Block>> entry : ModBlocks.LABUBU_FIGURINE_BLOCKS.entrySet()) {
            if (entry.getValue().get() == block) {
                RegistryObject<Item> itemObject = ModItems.LABUBU_FIGURINES.get(entry.getKey());
                if (itemObject != null) return new ItemStack(itemObject.get());
            }
        }
        return ItemStack.EMPTY;
    }

    private static HelmetBonus getHelmetBonus(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return new HelmetBonus(0.0D, 0.0D);
        double armor = 0.0D;
        double toughness = 0.0D;
        for (Map.Entry<Attribute, AttributeModifier> entry : helmet.getAttributeModifiers(EquipmentSlot.HEAD).entries()) {
            if (entry.getKey() == Attributes.ARMOR) armor += entry.getValue().getAmount();
            else if (entry.getKey() == Attributes.ARMOR_TOUGHNESS) toughness += entry.getValue().getAmount();
        }
        return new HelmetBonus(armor, toughness);
    }

    private static void clearHostileTargetsAroundPlayer(Player player, double radius) {
        player.level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(radius), LivingEntity::isAlive)
                .forEach(mob -> mob.setTarget(null));
    }

    private static boolean wearsSengokuCloak(@Nullable Entity entity) {
        return entity instanceof Player player && wearsSengokuCloak(player);
    }

    private static boolean wearsSengokuCloak(Player player) {
        LogiaCloakItem cloak = LogiaCloakItem.wornBy(player);
        return cloak != null && cloak.getCloakType() == LogiaCloakType.SENGOKU;
    }

    private static boolean isPork(Item item) {
        return item == Items.PORKCHOP || item == Items.COOKED_PORKCHOP;
    }

    private static boolean isLabubuBlock(Block block) {
        return getTypeForBlock(block) != null;
    }

    private static boolean isBuddhaLabubuBlock(Block block) {
        return getTypeForBlock(block) == LabubuType.BUDDHA;
    }

    private static boolean isHoldingKoran(Player player) {
        return player.getMainHandItem().is(ModItems.KORAN.get()) || player.getOffhandItem().is(ModItems.KORAN.get());
    }

    private static boolean isInsideBuddhaAura(Level level, BlockPos center) {
        for (int dx = -BUDDHA_AURA_RADIUS; dx <= BUDDHA_AURA_RADIUS; dx++) {
            for (int dy = -BUDDHA_AURA_RADIUS; dy <= BUDDHA_AURA_RADIUS; dy++) {
                for (int dz = -BUDDHA_AURA_RADIUS; dz <= BUDDHA_AURA_RADIUS; dz++) {
                    if (getTypeForBlock(level.getBlockState(center.offset(dx, dy, dz)).getBlock()) == LabubuType.BUDDHA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasAnonymousAura(Player player) {
        BlockPos center = player.blockPosition();
        for (int dx = -ANONYMOUS_AURA_RADIUS; dx <= ANONYMOUS_AURA_RADIUS; dx++) {
            for (int dy = -ANONYMOUS_AURA_RADIUS; dy <= ANONYMOUS_AURA_RADIUS; dy++) {
                for (int dz = -ANONYMOUS_AURA_RADIUS; dz <= ANONYMOUS_AURA_RADIUS; dz++) {
                    if (getTypeForBlock(player.level.getBlockState(center.offset(dx, dy, dz)).getBlock()) == LabubuType.ANONYMOUS) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void applyModifier(Player player, Attribute attribute, UUID uuid, String name,
                                      double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(uuid);
        if (Math.abs(amount) > 0.0001D) instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }

    private static void removeAllLabubuModifiers(Player player) {
        removeModifier(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_UUID);
        removeModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID);
        removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_UUID);
        removeModifier(player, Attributes.ARMOR, ARMOR_UUID);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_UUID);
    }

    private static void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(uuid);
    }

    private static UUID uuid(String key) {
        return UUID.nameUUIDFromBytes(("zitraksmode:" + key).getBytes(StandardCharsets.UTF_8));
    }

    private record HelmetBonus(double armor, double toughness) { }
    private record AnimeState(List<MobEffect> effects, long nextRollTick) { }

    private static class AuraSnapshot {
        private final Map<LabubuType, Integer> counts = new EnumMap<>(LabubuType.class);
        private final Map<LabubuType, List<BlockPos>> positions = new EnumMap<>(LabubuType.class);
        private final long lastScanTick;

        private AuraSnapshot(long lastScanTick) {
            this.lastScanTick = lastScanTick;
        }

        private void add(LabubuType type, BlockPos pos) {
            counts.merge(type, 1, Integer::sum);
            positions.computeIfAbsent(type, key -> new ArrayList<>()).add(pos);
        }

        private int count(LabubuType type) {
            return counts.getOrDefault(type, 0);
        }

        private List<BlockPos> positionsOf(LabubuType type) {
            return positions.getOrDefault(type, List.of());
        }

        private boolean isEmpty() {
            return counts.isEmpty();
        }
    }
}
