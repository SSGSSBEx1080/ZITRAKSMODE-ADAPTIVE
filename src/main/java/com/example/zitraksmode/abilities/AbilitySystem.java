package com.example.zitraksmode.abilities;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID)
public class AbilitySystem {

    private static final Map<UUID, Float> ENERGY_LEVELS = new HashMap<>();
    private static final Map<UUID, Boolean> READY_TO_EXPLODE = new HashMap<>();
    private static final Map<UUID, Boolean> EXPLOSION_ACTIVATED = new HashMap<>();
    private static final Map<UUID, Boolean> NEEDS_VELOCITY_RESET = new HashMap<>();
    private static final Map<UUID, Long> EXPLOSION_TIMERS = new HashMap<>();
    private static final Map<UUID, Long> FULL_ENERGY_TIMERS = new HashMap<>();

    private static final float MAX_ENERGY = 100.0f;

    private static final float MIN_ENERGY_GAIN = 2.0f;
    private static final float MAX_ENERGY_GAIN = 35.0f;
    private static final float ENERGY_MULTIPLIER = 2.5f;

    private static final int EXPLOSION_DELAY = 40;

    // БЫЛО 3.0f
    private static final float BASE_EXPLOSION_RADIUS = 5.0f;

    private static final float MAX_RESISTANCE_TO_BREAK = 100.0f;
    private static final int PENTAGRAM_POINTS = 5;
    private static final double PENTAGRAM_RADIUS = 2.0;
    private static final double PENTAGRAM_HEIGHT = 4.0;

    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.5f);

    public static boolean isWearingFullTntArmor(Player player) {
        return hasFullTntArmor(player);
    }

    public static float getEnergyLevel(Player player) {
        return ENERGY_LEVELS.getOrDefault(player.getUUID(), 0.0f);
    }

    public static boolean isReadyToExplode(Player player) {
        return READY_TO_EXPLODE.getOrDefault(player.getUUID(), false);
    }

    public static boolean isExplosionActivated(Player player) {
        return EXPLOSION_ACTIVATED.getOrDefault(player.getUUID(), false);
    }

    private static boolean hasFullTntArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.ZITRAKS_HELMET.get()
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.ZITRAKS_CHESTPLATE.get()
                && player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.ZITRAKS_LEGGINGS.get()
                && player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.ZITRAKS_BOOTS.get();
    }

    private static float getExplosionRadius(Player player) {
        float radius = BASE_EXPLOSION_RADIUS;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                int lvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BOMBARDIER.get(), stack);
                if (lvl > 0) {
                    radius += lvl * 0.5f;
                }
            }
        }

        return radius;
    }

    private static int getPeacemakerLevel(Player player) {
        int best = 0;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                int lvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PEACEMAKER.get(), stack);
                if (lvl > best) {
                    best = lvl;
                }
            }
        }

        return best;
    }

    private static boolean isHostileOrBoss(LivingEntity entity) {
        return entity instanceof Enemy
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon;
    }

    private static boolean shouldDamageTarget(Player player, LivingEntity target, int peacemakerLevel) {
        if (target == player) {
            return false;
        }

        if (peacemakerLevel >= 3) {
            return isHostileOrBoss(target);
        }

        return true;
    }

    private static boolean isSoftBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(BlockTags.DIRT)) return true;
        if (state.is(BlockTags.SAND)) return true;
        if (state.is(BlockTags.LEAVES)) return true;
        if (state.is(BlockTags.WOOL)) return true;

        if (state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MOSS_CARPET)) {
            return true;
        }

        return state.getDestroySpeed(level, pos) >= 0.0f
                && state.getDestroySpeed(level, pos) <= 1.0f
                && state.getBlock().getExplosionResistance() <= 2.5f;
    }

    public static void triggerExplosion(Player player) {
        UUID id = player.getUUID();

        if (!READY_TO_EXPLODE.getOrDefault(id, false)) return;
        if (EXPLOSION_ACTIVATED.getOrDefault(id, false)) return;

        Level level = player.getCommandSenderWorld();
        if (level == null) return;

        if (player instanceof ServerPlayer serverPlayer) {
            ZitraksMode.TNT_ULTIMATE.trigger(serverPlayer);
        }

        EXPLOSION_ACTIVATED.put(id, true);
        EXPLOSION_TIMERS.put(id, level.getGameTime());
        READY_TO_EXPLODE.put(id, false);
        ENERGY_LEVELS.put(id, 0.0f);

        level.playSound(null, player.blockPosition(),
                ModSounds.CUSTOM_EXPLOSION.get(),
                SoundSource.PLAYERS, 3.0f, 1.0f);
    }

    private static void spawnChargingParticles(ServerLevel serverLevel, Player player, long ticksElapsed) {
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.5;
        double z = player.getZ();

        float progress = (float) ticksElapsed / EXPLOSION_DELAY;
        DustParticleOptions redstoneDust = DustParticleOptions.REDSTONE;

        if (ticksElapsed <= 30) {
            if (ticksElapsed < 10) {
                double horizontalRadius = 1.0 + progress * 0.5;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    serverLevel.sendParticles(redstoneDust,
                            x + Math.cos(angle) * horizontalRadius,
                            player.getY() + 0.1,
                            z + Math.sin(angle) * horizontalRadius,
                            0, 0, 0, 0, 0);
                }
            }

            if (ticksElapsed >= 5) {
                int spiralCount = 10;
                double spiralRadius = 1.5;
                double spiralHeight = player.getBbHeight() + 1.0;
                double currentY = player.getY() + (spiralHeight * progress);

                for (int i = 0; i < spiralCount; i++) {
                    double angle = (serverLevel.getGameTime() * 0.2 + i * 0.6) % (2 * Math.PI);
                    serverLevel.sendParticles(redstoneDust,
                            x + Math.cos(angle) * spiralRadius,
                            currentY + 0.1 * i,
                            z + Math.sin(angle) * spiralRadius,
                            0, 0, 0, 0, 0);
                }
            }
        }

        if (ticksElapsed >= 30) {
            float pentagramProgress = (float) (ticksElapsed - 30) / (EXPLOSION_DELAY - 30);
            double pentagramY = player.getY() + PENTAGRAM_HEIGHT
                    - (PENTAGRAM_HEIGHT * pentagramProgress);

            if (pentagramY > player.getY() - 0.5) {
                for (int i = 0; i < PENTAGRAM_POINTS; i++) {
                    double angle = Math.toRadians(-90 + i * 144);
                    double px = x + Math.cos(angle) * PENTAGRAM_RADIUS;
                    double pz = z + Math.sin(angle) * PENTAGRAM_RADIUS;

                    serverLevel.sendParticles(redstoneDust, px, pentagramY, pz, 0, 0, 0, 0, 0);

                    int nextIndex = (i + 2) % PENTAGRAM_POINTS;
                    double nextAngle = Math.toRadians(-90 + nextIndex * 144);
                    double npx = x + Math.cos(nextAngle) * PENTAGRAM_RADIUS;
                    double npz = z + Math.sin(nextAngle) * PENTAGRAM_RADIUS;

                    for (int j = 1; j <= 5; j++) {
                        float seg = j / 6.0f;
                        double lineX = px + (npx - px) * seg;
                        double lineZ = pz + (npz - pz) * seg;
                        serverLevel.sendParticles(redstoneDust, lineX, pentagramY, lineZ, 0, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private static void executeExplosion(Player player) {
        Level level = player.getCommandSenderWorld();
        if (level == null || level.isClientSide()) return;

        UUID id = player.getUUID();
        float radius = getExplosionRadius(player);
        int peacemakerLevel = getPeacemakerLevel(player);
        BlockPos center = player.blockPosition();

        double px = player.getX();
        double py = player.getY() + player.getBbHeight() * 0.7;
        double pz = player.getZ();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(RED_DUST, px, py, pz, 30, 0.5, 0.5, 0.5, 0.2);
            serverLevel.sendParticles(ParticleTypes.FLASH, px, py, pz, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.LAVA, px, player.getY() + 0.5, pz, 10, 0.5, 0.5, 0.5, 0.0);

            for (int i = 0; i < 360; i += 5) {
                double rad = Math.toRadians(i);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        px, player.getY() + 0.5, pz,
                        1,
                        Math.cos(rad) * 0.3, 0.2, Math.sin(rad) * 0.3,
                        0.0);
            }
        }

        float damageRadius = radius * 1.5f;
        AABB aabb = new AABB(
                player.getX() - damageRadius, player.getY() - damageRadius, player.getZ() - damageRadius,
                player.getX() + damageRadius, player.getY() + damageRadius, player.getZ() + damageRadius
        );

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class, aabb, entity -> entity != player
        );

        int totalBombardier = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                totalBombardier += EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.BOMBARDIER.get(), stack);
            }
        }

        for (LivingEntity target : entities) {
            if (!shouldDamageTarget(player, target, peacemakerLevel)) {
                continue;
            }

            double distance = target.distanceTo(player);
            if (distance > damageRadius) continue;

            float baseDamage = 15.0f;
            float intensity = 1.0f - (float) (distance / damageRadius);

            float adjustedDamage = (totalBombardier >= 10)
                    ? 100.0f * intensity
                    : (baseDamage + radius * 1.5f) * intensity;

            DamageSource explosionDamage =
                    new IndirectEntityDamageSource("explosion.tnt_ability", player, player).setExplosion();

            target.hurt(explosionDamage, adjustedDamage);

            if (target.isDeadOrDying() && player instanceof ServerPlayer sp) {
                ZitraksMode.TNT_EXPLOSION_KILL.trigger(sp);
            }

            Vec3 vec3 = target.position().subtract(player.position());
            double d0 = Math.sqrt(vec3.horizontalDistanceSqr());
            if (d0 != 0.0D) {
                vec3 = vec3.normalize();
                double knockbackStrength = (radius * 0.5) * intensity;
                target.setDeltaMovement(target.getDeltaMovement().add(vec3.scale(knockbackStrength)));
            }
        }

        if (peacemakerLevel < 2) {
            int r = (int) Math.ceil(radius);

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        if (x * x + y * y + z * z > radius * radius) {
                            continue;
                        }

                        BlockPos targetPos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(targetPos);

                        if (state.isAir()) {
                            continue;
                        }

                        if (state.getBlock().getExplosionResistance() > MAX_RESISTANCE_TO_BREAK) {
                            continue;
                        }

                        if (peacemakerLevel == 1 && !isSoftBlock(level, targetPos, state)) {
                            continue;
                        }

                        level.destroyBlock(targetPos, true);
                    }
                }
            }
        }

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int damage = Math.max(1, (int) (stack.getMaxDamage() * 0.10f));
                stack.hurtAndBreak(damage, player, p -> p.broadcastBreakEvent(slot));
            }
        }

        NEEDS_VELOCITY_RESET.put(id, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.getCommandSenderWorld();
        if (level == null || level.isClientSide()) return;

        UUID id = player.getUUID();
        long currentTime = level.getGameTime();

        if (!hasFullTntArmor(player)) {
            if (ENERGY_LEVELS.containsKey(id)) {
                ENERGY_LEVELS.remove(id);
                READY_TO_EXPLODE.remove(id);
                EXPLOSION_ACTIVATED.remove(id);
                EXPLOSION_TIMERS.remove(id);
                NEEDS_VELOCITY_RESET.remove(id);
            }
            return;
        }

        if (currentTime % 20 == 0 && player instanceof ServerPlayer sp) {
            ZitraksMode.BOMBARDIER_SET.checkAndTrigger(sp);
        }

        if (EXPLOSION_ACTIVATED.getOrDefault(id, false)) {
            long activationTime = EXPLOSION_TIMERS.getOrDefault(id, 0L);
            long ticksElapsed = currentTime - activationTime;

            if (level instanceof ServerLevel serverLevel) {
                spawnChargingParticles(serverLevel, player, ticksElapsed);
            }

            if (ticksElapsed >= EXPLOSION_DELAY) {
                executeExplosion(player);
                EXPLOSION_ACTIVATED.put(id, false);
            }
        }

        if (NEEDS_VELOCITY_RESET.getOrDefault(id, false)) {
            player.setDeltaMovement(Vec3.ZERO);
            NEEDS_VELOCITY_RESET.put(id, false);
        }

        if (ENERGY_LEVELS.getOrDefault(id, 0.0f) >= MAX_ENERGY
                && !READY_TO_EXPLODE.getOrDefault(id, false)) {
            READY_TO_EXPLODE.put(id, true);
            FULL_ENERGY_TIMERS.put(id, currentTime);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Level level = player.getCommandSenderWorld();
        if (level == null || level.isClientSide()) return;

        if (!hasFullTntArmor(player)) return;

        UUID id = player.getUUID();
        float currentEnergy = ENERGY_LEVELS.getOrDefault(id, 0.0f);
        if (currentEnergy >= MAX_ENERGY) return;

        float incomingDamage = event.getAmount();

        float energyGain = incomingDamage * ENERGY_MULTIPLIER;
        energyGain = Math.max(MIN_ENERGY_GAIN, Math.min(MAX_ENERGY_GAIN, energyGain));

        float newEnergy = Math.min(MAX_ENERGY, currentEnergy + energyGain);
        ENERGY_LEVELS.put(id, newEnergy);

        if (energyGain >= 15.0f && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(RED_DUST,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    (int) (energyGain / 5),
                    0.3, 0.5, 0.3, 0.01);
        }

        if (newEnergy >= MAX_ENERGY) {
            READY_TO_EXPLODE.put(id, true);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 1.0f, 0.5f);
        }
    }
}