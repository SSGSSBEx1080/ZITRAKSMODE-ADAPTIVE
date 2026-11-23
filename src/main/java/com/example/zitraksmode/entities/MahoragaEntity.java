package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.entities.AntonChigurEntity;
import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.MahoragaSummonItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MahoragaEntity extends PathfinderMob implements IAnimatable {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUMMON_TOKEN_KEY = "MahoragaSummonToken";

    private static final float BASE_MAX_HP = 500.0F;
    private static final float PHASE_TWO_MAX_HP = 100.0F;
    private static final float SEGMENT = BASE_MAX_HP / 10F;
    private static final double BASE_MOVE_SPEED = 0.6D;
    private static final double PHASE_TWO_MOVE_SPEED = 1.2D;

    private static final int ANIM_NONE = 0;
    private static final int ANIM_MELEE_LEFT = 1;
    private static final int ANIM_MELEE_RIGHT = 2;
    private static final int ANIM_MELEE_DOUBLE = 3;
    private static final int ANIM_LEAP = 4;
    private static final int ANIM_SCREAM = 5;
    private static final int ANIM_LASER = 6;
    private static final int ANIM_SPEED_BLITZ = 7;

    private static final int MELEE_COOLDOWN = 10;
    private static final int Scream_RADIUS = 10;
    private static final int SCREAM_MIN_TARGETS = 6;
    private static final int LEAP_VERTICAL_THRESHOLD = 5;

    // === КД СКИЛЛОВ (фаза 1 / фаза 2) — увеличены ===
    private static final int LEAP_CD_P1 = 300;
    private static final int LEAP_CD_P2 = 180;
    private static final int BLITZ_CD_P1 = 280;
    private static final int BLITZ_CD_P2 = 160;
    private static final int LASER_CD_P1 = 300;
    private static final int LASER_CD_P2 = 180;
    private static final int SCREAM_CD_P1 = 300;
    private static final int SCREAM_CD_P2 = 180;

    // Расширенный пул блоков заражения (для приземления)
    private static final Block[] CORRUPTION_BLOCK_POOL = new Block[]{
            Blocks.SCULK,
            Blocks.SCULK_SENSOR,
            Blocks.SCULK_CATALYST,
            Blocks.SCULK_SHRIEKER,
            Blocks.MAGMA_BLOCK,
            Blocks.SOUL_SAND,
            Blocks.SOUL_SOIL,
            Blocks.BLACKSTONE,
            Blocks.BASALT,
            Blocks.NETHERRACK,
            Blocks.CRYING_OBSIDIAN,
            Blocks.GILDED_BLACKSTONE,
            Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Blocks.POLISHED_BLACKSTONE_BRICKS
    };

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private static final EntityDataAccessor<Integer> SYNC_ATTACK_ANIM =
            SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SYNC_ATTACK_TICKS =
            SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SYNC_WHEEL_ROTATION =
            SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.FLOAT);

    private int lastClientAttackAnim = -1;

    boolean isAttackingMelee = false;
    String currentMeleeAnim = "right";

    boolean isLeaping = false;
    boolean isScreaming = false;
    boolean isLasering = false;
    boolean isSpeedBlitzing = false;

    private float wheelRotation = 0.0F;
    boolean isAdaptingWheel = false;
    private int adaptWheelTimer = 0;
    private static final int ADAPT_WHEEL_DURATION = 10;

    private int musicTimer = 20;
    private static final int MUSIC_DURATION = 3640;
    private static final float MUSIC_VOLUME = 0.15F;
    private boolean ambientStopped = false;

    private int meleeAttackTimer = 0;
    private LivingEntity meleeTarget = null;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Махорага"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private UUID ownerUUID = null;
    private int adaptationStage = 0;
    private boolean secondPhase = false;
    private boolean ownerScrollConsumed = false;
    private final Set<String> adaptedDamageTypes = new HashSet<>();
    private float currentHealthThreshold = BASE_MAX_HP - SEGMENT;

    private final Map<UUID, Integer> targetPriority = new LinkedHashMap<>();
    private final Map<UUID, Long> targetOrder = new HashMap<>();
    private long nextTargetOrder = 1L;
    private int lastOwnerAttackTimestamp = -1;

    public int leapCooldown = 0;
    private int leapTimer = 0;
    private Vec3 leapTargetPos = null;
    private Vec3 leapStartPos = null;
    private Vec3 leapPeakPos = null;
    private Vec3 leapLandPos = null;

    public int speedBlitzCooldown = 0;
    private int speedBlitzTimer = 0;
    private Vec3 blitzTargetPos = null;
    private boolean hasGrabbed = false;
    private LivingEntity grabbedEntity = null;
    private int grabTimer = 0;

    public int laserCooldown = 0;
    private int screamCooldown = 0;

    private boolean summonLandingArmed = false;
    private boolean summonLandingProcessed = false;
    private final Set<UUID> damagedPlayers = new HashSet<>();

    // === ДЕТЕКЦИЯ ЗАСТРЕВАНИЯ ===
    private Vec3 lastCombatPos = Vec3.ZERO;
    private double lastDistToTargetSq = Double.MAX_VALUE;
    private int noProgressTicks = 0;          // тиков без прогресса к цели
    private int emergencyEscapeCooldown = 0;  // КД на экстренный выход

    // === АНТИ-АФК В МИЛИ ===
    // Если у нас есть готовый дальний скилл и мы долго стоим в мили — отступаем и кастуем
    private int consecutiveMeleeHits = 0;
    private int meleeStaleTicks = 0;          // тиков в мили-замесе подряд
    private boolean isRetreating = false;
    private int retreatTimer = 0;
    private int retreatCooldown = 0;
    private int plannedSkillAfterRetreat = ANIM_NONE; // что кастанём после отбегания

    public MahoragaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.getPersistentData().putBoolean("mahoraga_keep", true);
        LOGGER.info("[Mahoraga] Spawned at {}", this.blockPosition());
    }

    @Override
    public void checkDespawn() {
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HP)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SYNC_ATTACK_ANIM, ANIM_NONE);
        this.entityData.define(SYNC_ATTACK_TICKS, 0);
        this.entityData.define(SYNC_WHEEL_ROTATION, 0.0F);
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "movement", 5, this::movementPredicate));
        data.addAnimationController(new AnimationController<>(this, "attack", 2, this::attackPredicate));
    }

    private PlayState movementPredicate(AnimationEvent<MahoragaEntity> event) {
        int attackAnim = this.entityData.get(SYNC_ATTACK_ANIM);
        if (attackAnim != ANIM_NONE) {
            return PlayState.STOP;
        }
        if (this.dead || this.getHealth() <= 0) {
            event.getController().setAnimation(new AnimationBuilder()
                    .addAnimation("animation.mahoraga.death", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (event.isMoving()) {
            event.getController().setAnimation(new AnimationBuilder()
                    .addAnimation("animation.mahoraga.walk", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }
        event.getController().setAnimation(new AnimationBuilder()
                .addAnimation("animation.mahoraga.idle", ILoopType.EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }

    private PlayState attackPredicate(AnimationEvent<MahoragaEntity> event) {
        int anim = this.entityData.get(SYNC_ATTACK_ANIM);
        if (anim != lastClientAttackAnim) {
            event.getController().markNeedsReload();
            lastClientAttackAnim = anim;
        }
        switch (anim) {
            case ANIM_MELEE_LEFT -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.attack_left", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            case ANIM_MELEE_RIGHT -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.attack_right", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            case ANIM_MELEE_DOUBLE -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.attack_double", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            case ANIM_LEAP -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.leap", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            case ANIM_SCREAM -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.scream", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            case ANIM_LASER -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.laser", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            case ANIM_SPEED_BLITZ -> {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.mahoraga.speed_blitz", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
                return PlayState.CONTINUE;
            }
            default -> {
                return PlayState.STOP;
            }
        }
    }

    private void startSyncedAttackAnimation(int animId, int ticks) {
        if (!this.level.isClientSide) {
            this.entityData.set(SYNC_ATTACK_ANIM, animId);
            this.entityData.set(SYNC_ATTACK_TICKS, ticks);
        }
    }

    public float getWheelRotation() {
        return this.entityData.get(SYNC_WHEEL_ROTATION);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ============================================================
    // ГЛАВНЫЙ ТИКЕР
    // ============================================================
    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (!this.level.isClientSide) {
            syncOwnerTargets();
            cleanupTargetPriorities();
            updatePriorityTargetSelection();

            int animTicks = this.entityData.get(SYNC_ATTACK_TICKS);
            if (animTicks > 0) {
                animTicks--;
                this.entityData.set(SYNC_ATTACK_TICKS, animTicks);
                if (animTicks <= 0) {
                    this.entityData.set(SYNC_ATTACK_ANIM, ANIM_NONE);
                }
            }

            if (!this.isDeadOrDying()) {
                musicTimer--;
                if (musicTimer <= 0) {
                    musicTimer = MUSIC_DURATION;
                    this.level.playSound(null, this.blockPosition(),
                            ModSounds.MAHORAGA_AMBIENT.get(), SoundSource.MUSIC, MUSIC_VOLUME, 1.0F);
                }
            }

            handleSummonLanding();
            handleStuckDetection();
            handleAntiAfkMelee();
        }

        if (leapCooldown > 0) leapCooldown--;
        if (speedBlitzCooldown > 0) speedBlitzCooldown--;
        if (laserCooldown > 0) laserCooldown--;
        if (screamCooldown > 0) screamCooldown--;
        if (retreatCooldown > 0) retreatCooldown--;
        if (emergencyEscapeCooldown > 0) emergencyEscapeCooldown--;

        if (isLeaping) {
            leapTimer++;
            updateLeapPhysics();
            if (leapTimer >= 64) {
                endLeap();
            }
        }

        if (isSpeedBlitzing) {
            speedBlitzTimer++;
            updateSpeedBlitzPhysics();
            if (speedBlitzTimer >= 90) {
                endSpeedBlitz();
            }
        }

        updateMeleeAttack();

        if (isAdaptingWheel) {
            wheelRotation += 45.0F / ADAPT_WHEEL_DURATION;
            adaptWheelTimer--;
            if (!this.level.isClientSide) {
                this.entityData.set(SYNC_WHEEL_ROTATION, wheelRotation);
            }
            if (adaptWheelTimer <= 0) {
                isAdaptingWheel = false;
                wheelRotation = Math.round(wheelRotation / 45.0F) * 45.0F;
                if (!this.level.isClientSide) {
                    this.entityData.set(SYNC_WHEEL_ROTATION, wheelRotation);
                }
            }
        }

        // Обработка отступления
        if (isRetreating) {
            retreatTimer--;
            if (retreatTimer <= 0) {
                isRetreating = false;
                // После отступления — кастуем запланированный скилл
                tryCastPlannedSkill();
            }
        }
    }

    // ============================================================
    // ДЕТЕКЦИЯ ЗАСТРЕВАНИЯ — главная "умная" логика
    // Срабатывает ТОЛЬКО когда нет прогресса к цели >= 10 тиков
    // Реакция: 1) Leap → 2) SpeedBlitz → 3) ломаем блоки → 4) телепорт вверх
    // ============================================================
    private void handleStuckDetection() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !isValidPriorityTarget(target)) {
            noProgressTicks = 0;
            lastDistToTargetSq = Double.MAX_VALUE;
            lastCombatPos = this.position();
            return;
        }

        // Во время скиллов/отступления — не трогаем
        if (isLeaping || isSpeedBlitzing || isLasering || isScreaming || isRetreating) {
            noProgressTicks = 0;
            lastDistToTargetSq = this.distanceToSqr(target);
            lastCombatPos = this.position();
            return;
        }

        double currentDistSq = this.distanceToSqr(target);

        // Если уже в радиусе мили — не считаем "застрял"
        if (currentDistSq <= 9.0D) {
            noProgressTicks = 0;
            lastDistToTargetSq = currentDistSq;
            lastCombatPos = this.position();
            return;
        }

        // Проверяем прогресс каждые 5 тиков
        if (this.tickCount % 5 == 0) {
            double movedSq = this.position().distanceToSqr(this.lastCombatPos);
            // "Нет прогресса" = (двигаемся почти на месте) ИЛИ (расстояние до цели не уменьшается)
            boolean noMovement = movedSq < 0.10D;
            boolean noDistanceProgress = currentDistSq >= lastDistToTargetSq - 0.25D;

            if (noMovement || noDistanceProgress) {
                noProgressTicks += 5;
            } else {
                noProgressTicks = Math.max(0, noProgressTicks - 5);
            }

            this.lastCombatPos = this.position();
            this.lastDistToTargetSq = currentDistSq;
        }

        // === ЗАСТРЯЛ! 10+ тиков без прогресса ===
        if (noProgressTicks >= 10 && emergencyEscapeCooldown <= 0) {
            tryEscapeStuck(target);
        }
    }

    /**
     * Реакция на застревание:
     * 1) Leap (приоритет, лучше всего пробивает преграды и высоту)
     * 2) SpeedBlitz (если Leap на КД)
     * 3) Ломаем блоки на пути (если оба скилла на КД)
     * 4) Телепорт вверх (последний шанс, если совсем затупил >= 40 тиков)
     */
    private void tryEscapeStuck(LivingEntity target) {
        // Самый крайний случай — телепорт вверх
        if (noProgressTicks >= 40) {
            noProgressTicks = 0;
            emergencyEscapeCooldown = 80;
            BlockPos above = this.blockPosition().above(3);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        destroyNonBedrock(above.offset(dx, dy, dz));
                    }
                }
            }
            this.teleportTo(above.getX() + 0.5D, above.getY(), above.getZ() + 0.5D);
            LOGGER.info("[Mahoraga] CRITICAL stuck → forced teleport up");
            return;
        }

        // Приоритет 1: Leap
        if (leapCooldown <= 0) {
            noProgressTicks = 0;
            emergencyEscapeCooldown = 60;
            forceLeapToTarget(target);
            LOGGER.info("[Mahoraga] Stuck → Emergency LEAP");
            return;
        }

        // Приоритет 2: SpeedBlitz (если по высоте подходит)
        double dy = Math.abs(target.getY() - this.getY());
        if (speedBlitzCooldown <= 0 && dy <= 6.0D) {
            noProgressTicks = 0;
            emergencyEscapeCooldown = 60;
            forceSpeedBlitzToTarget(target);
            LOGGER.info("[Mahoraga] Stuck → Emergency SPEED BLITZ");
            return;
        }

        // Приоритет 3: ломаем блоки на пути (узким коридором)
        // Делаем это ОДИН РАЗ при срабатывании, не каждый тик
        emergencyEscapeCooldown = 30;
        breakBlocksAlongLine(this.position(), target.position(), 1, 1, 3, true);
        // И прыгаем вверх+вперёд физически
        if (this.onGround) {
            Vec3 toward = target.position().subtract(this.position());
            Vec3 flat = new Vec3(toward.x, 0.0D, toward.z);
            if (flat.lengthSqr() > 1.0E-4D) {
                flat = flat.normalize().scale(0.45D);
                this.setDeltaMovement(flat.x, 0.65D, flat.z);
                this.hurtMarked = true;
            }
        }
        LOGGER.info("[Mahoraga] Stuck → breaking blocks along path");
    }

    // ============================================================
    // АНТИ-АФК В МИЛИ
    // Если стоим в мили-замесе слишком долго и есть готовый дальний скилл —
    // отбегаем и кастуем
    // ============================================================
    private void handleAntiAfkMelee() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !isValidPriorityTarget(target)) {
            meleeStaleTicks = 0;
            consecutiveMeleeHits = 0;
            return;
        }

        if (isLeaping || isSpeedBlitzing || isLasering || isScreaming || isRetreating) {
            return;
        }

        double distSq = this.distanceToSqr(target);
        boolean inMeleeRange = distSq <= 12.0D;

        if (inMeleeRange) {
            meleeStaleTicks++;
        } else {
            meleeStaleTicks = Math.max(0, meleeStaleTicks - 2);
        }

        // Условие: 60+ тиков в мили (3 сек) ИЛИ 3+ удара подряд И есть готовый скилл И КД отбега прошёл
        boolean longMelee = meleeStaleTicks >= 60 || consecutiveMeleeHits >= 3;
        if (!longMelee || retreatCooldown > 0) return;

        // Выбираем что кастовать
        int planned = pickBestRangedSkillFor(target);
        if (planned == ANIM_NONE) return; // нет готовых скиллов — продолжаем месить

        startSmartRetreat(target, planned);
    }

    /**
     * Выбирает лучший дальний скилл для текущей цели.
     * Возвращает ANIM_* или ANIM_NONE если нет готовых.
     */
    private int pickBestRangedSkillFor(LivingEntity target) {
        double distSq = this.distanceToSqr(target);
        double dy = Math.abs(target.getY() - this.getY());

        // Scream — если много врагов рядом
        if (screamCooldown <= 0 && countNearbyEnemies(Scream_RADIUS) >= SCREAM_MIN_TARGETS) {
            // Scream не требует отступления — но раз уж планируем, кастанём после
            return ANIM_SCREAM;
        }
        // Laser — дальняя атака, нужно расстояние
        if (laserCooldown <= 0) {
            return ANIM_LASER;
        }
        // SpeedBlitz — средняя дистанция
        if (speedBlitzCooldown <= 0 && dy <= 5.0D) {
            return ANIM_SPEED_BLITZ;
        }
        // Leap — если есть вертикаль или хоть что-то
        if (leapCooldown <= 0) {
            return ANIM_LEAP;
        }
        return ANIM_NONE;
    }

    private void startSmartRetreat(LivingEntity target, int plannedSkill) {
        isRetreating = true;
        retreatTimer = secondPhase ? 15 : 20;
        retreatCooldown = secondPhase ? 60 : 100;
        plannedSkillAfterRetreat = plannedSkill;
        consecutiveMeleeHits = 0;
        meleeStaleTicks = 0;

        Vec3 away = this.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        double retreatDist = secondPhase ? 8.0D : 7.0D;
        Vec3 retreatPos = this.position().add(away.scale(retreatDist));

        this.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, secondPhase ? 1.6D : 1.4D);

        // Прыжок при отступлении для скорости (физический буст)
        if (this.onGround) {
            this.setDeltaMovement(away.scale(0.55D).add(0, 0.45D, 0));
            this.hurtMarked = true;
        }

        LOGGER.info("[Mahoraga] Smart retreat from {} → will cast {}",
                target.getDisplayName().getString(), animName(plannedSkill));
    }

    private void tryCastPlannedSkill() {
        int planned = plannedSkillAfterRetreat;
        plannedSkillAfterRetreat = ANIM_NONE;
        if (planned == ANIM_NONE) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        // Кастуем напрямую (Goals в этом же тике подхватят благодаря shouldUse*)
        switch (planned) {
            case ANIM_LASER -> {
                if (laserCooldown <= 0) {
                    // Поставим состояние, Goal LaserAttackGoal сам подхватит на следующий тик
                    // Но для надёжности — форсируем
                    // (Goal проверяет shouldUseLaser которое требует >= 196 distSq.
                    //  После отступления мы дальше, так что подхватит.)
                }
            }
            case ANIM_SPEED_BLITZ -> {
                if (speedBlitzCooldown <= 0) forceSpeedBlitzToTarget(target);
            }
            case ANIM_LEAP -> {
                if (leapCooldown <= 0) forceLeapToTarget(target);
            }
            // SCREAM подхватится автоматически если condition выполнено
            default -> {}
        }
    }

    private static String animName(int id) {
        return switch (id) {
            case ANIM_LASER -> "LASER";
            case ANIM_SPEED_BLITZ -> "SPEED_BLITZ";
            case ANIM_LEAP -> "LEAP";
            case ANIM_SCREAM -> "SCREAM";
            default -> "NONE";
        };
    }

    // ============================================================
    // ПРИЗЕМЛЕНИЕ — КРАТЕР + ЗАРАЖЕНИЕ ВОКРУГ
    // ============================================================
    private void handleSummonLanding() {
        if (summonLandingProcessed) {
            return;
        }
        if (this.tickCount < 120 && this.getDeltaMovement().y < -0.15D) {
            summonLandingArmed = true;
        }
        if (summonLandingArmed && this.onGround) {
            summonLandingProcessed = true;
            if (this.level instanceof ServerLevel serverLevel) {
                BlockPos landPos = this.blockPosition();

                // Фаза 1: чёткий кратер 3x3x1 (продавленный слой)
                createImpactCrater(landPos);

                // Фаза 2: заражение ВОКРУГ кратера (кольцо рандомного радиуса)
                createCorruptionSpread(serverLevel, landPos);

                // Визуальные эффекты
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        getX(), getY(), getZ(), 12, 1.8D, 0.5D, 1.8D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        getX(), getY() + 0.2D, getZ(), 30, 2.5D, 0.6D, 2.5D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        getX(), getY() + 0.3D, getZ(), 20, 2.0D, 0.4D, 2.0D, 0.03D);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        getX(), getY() + 0.1D, getZ(), 25, 2.2D, 0.5D, 2.2D, 0.02D);
            }
        }
    }

    /**
     * Кратер 3×3×1: продавливает слой земли под Махорагой при приземлении.
     * Также чистит блоки на уровне его тела (чтобы не застрял в стенах).
     */
    private void createImpactCrater(BlockPos center) {
        BlockPos base = center.below(); // слой под ногами
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // Продавленный слой (это и есть кратер)
                destroyNonBedrock(base.offset(dx, 0, dz));
                // Очистка для тела (чтобы не застрял)
                destroyNonBedrock(base.offset(dx, 1, dz));
                destroyNonBedrock(base.offset(dx, 2, dz));
            }
        }
    }

    /**
     * Заражение ВОКРУГ кратера: кольцо рандомного радиуса 4-7,
     * заменяет верхний блок в каждой колонке на случайный зловещий блок из пула.
     * Внутренние 3×3 (кратер) НЕ трогает.
     */
    private void createCorruptionSpread(ServerLevel serverLevel, BlockPos center) {
        BlockPos base = center.below();
        int minRadius = 4;
        int maxRadius = 7;
        int radius = minRadius + this.random.nextInt(maxRadius - minRadius + 1);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Внутри кратера 3×3 пропускаем (там уже разрушено)
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    continue;
                }

                double distSq = dx * dx + dz * dz;
                double radiusSq = radius * radius;
                if (distSq > radiusSq) continue;

                // Шанс заражения уменьшается к краям (плотно у центра, разрежено по краям)
                float distanceRatio = (float) (distSq / radiusSq);
                float chance = 0.90F - distanceRatio * 0.55F; // ~90% у центра, ~35% по краям
                if (this.random.nextFloat() > chance) continue;

                // Ищем верхний твёрдый блок в колонке (±3 от базы)
                BlockPos surfacePos = null;
                for (int dy = 3; dy >= -3; dy--) {
                    BlockPos check = base.offset(dx, dy, dz);
                    BlockPos above = check.above();
                    if (!level.getBlockState(check).isAir()
                            && level.getBlockState(check).getBlock() != Blocks.BEDROCK
                            && level.getBlockState(above).isAir()) {
                        surfacePos = check;
                        break;
                    }
                }
                if (surfacePos == null) continue;

                // Заменяем на случайный зловещий блок
                Block corruptBlock = CORRUPTION_BLOCK_POOL[random.nextInt(CORRUPTION_BLOCK_POOL.length)];
                level.setBlock(surfacePos, corruptBlock.defaultBlockState(), 3);

                // Редкие частицы поверх
                if (this.random.nextFloat() < 0.25F) {
                    serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                            surfacePos.getX() + 0.5D, surfacePos.getY() + 1.0D, surfacePos.getZ() + 0.5D,
                            2, 0.2D, 0.2D, 0.2D, 0.01D);
                }
            }
        }
    }

    // ============================================================
    // ДВИЖЕНИЕ К ЦЕЛИ (БЕЗ ЛОМКИ БЛОКОВ!)
    // Ломка происходит ТОЛЬКО при застревании или внутри скиллов.
    // ============================================================

    @Nullable
    private Player getOwnerPlayer() {
        if (ownerUUID == null) {
            return null;
        }
        return this.level.getPlayerByUUID(ownerUUID);
    }

    private void syncOwnerTargets() {
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        LivingEntity attackedByOwner = owner.getLastHurtMob();
        if (attackedByOwner == null || !attackedByOwner.isAlive()) {
            return;
        }
        if (owner.getLastHurtMobTimestamp() == lastOwnerAttackTimestamp) {
            return;
        }
        lastOwnerAttackTimestamp = owner.getLastHurtMobTimestamp();
        if (!owner.getMainHandItem().isEmpty()) {
            return;
        }
        if (attackedByOwner == this) {
            return;
        }
        if (ownerUUID != null && attackedByOwner.getUUID().equals(ownerUUID)) {
            return;
        }
        if (!this.canAttack(attackedByOwner)) {
            return;
        }
        addHostileByOwner(attackedByOwner);
    }

    private void cleanupTargetPriorities() {
        List<UUID> toRemove = new ArrayList<>();
        for (UUID uuid : targetPriority.keySet()) {
            LivingEntity entity = resolveLiving(uuid);
            if (entity == null || !entity.isAlive() || entity == this
                    || (ownerUUID != null && uuid.equals(ownerUUID))) {
                toRemove.add(uuid);
            }
        }
        for (UUID uuid : toRemove) {
            clearTargetPriority(uuid);
        }
        LivingEntity current = this.getTarget();
        if (current != null && (!current.isAlive() || !targetPriority.containsKey(current.getUUID()))) {
            this.setTarget(null);
        }
    }

    private void updatePriorityTargetSelection() {
        LivingEntity best = chooseBestPriorityTarget();
        LivingEntity current = this.getTarget();
        if (best == null) {
            this.setTarget(null);
            return;
        }
        if (current == null || !current.isAlive()) {
            this.setTarget(best);
            return;
        }
        if (current.getUUID().equals(best.getUUID())) {
            return;
        }
        int currentPriority = getPriority(current.getUUID());
        int bestPriority = getPriority(best.getUUID());
        long currentOrder = getPriorityOrder(current.getUUID());
        long bestOrder = getPriorityOrder(best.getUUID());
        if (bestPriority > currentPriority
                || (bestPriority == currentPriority && bestOrder < currentOrder)) {
            this.setTarget(best);
            this.getNavigation().stop();
            LOGGER.info("[Mahoraga] Switching target -> {} (prio={})",
                    best.getDisplayName().getString(), bestPriority);
        }
    }

    @Nullable
    private LivingEntity chooseBestPriorityTarget() {
        LivingEntity best = null;
        int bestPriority = Integer.MIN_VALUE;
        long bestOrder = Long.MAX_VALUE;
        for (Map.Entry<UUID, Integer> entry : targetPriority.entrySet()) {
            LivingEntity entity = resolveLiving(entry.getKey());
            if (entity == null || !entity.isAlive() || !isValidPriorityTarget(entity)) {
                continue;
            }
            int priority = entry.getValue();
            long order = getPriorityOrder(entry.getKey());
            if (priority > bestPriority || (priority == bestPriority && order < bestOrder)) {
                best = entity;
                bestPriority = priority;
                bestOrder = order;
            }
        }
        return best;
    }

    @Nullable
    private LivingEntity resolveLiving(UUID uuid) {
        if (!(this.level instanceof ServerLevel serverLevel) || uuid == null) {
            return null;
        }
        Entity entity = serverLevel.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean isValidPriorityTarget(LivingEntity entity) {
        return entity != null
                && entity.isAlive()
                && entity != this
                && (ownerUUID == null || !entity.getUUID().equals(ownerUUID))
                && this.canAttack(entity);
    }

    private int getPriority(UUID uuid) {
        return targetPriority.getOrDefault(uuid, 0);
    }

    private long getPriorityOrder(UUID uuid) {
        return targetOrder.getOrDefault(uuid, Long.MAX_VALUE);
    }

    private void clearTargetPriority(UUID uuid) {
        targetPriority.remove(uuid);
        targetOrder.remove(uuid);
        if (this.getTarget() != null && this.getTarget().getUUID().equals(uuid)) {
            this.setTarget(null);
        }
    }

    // ============================================================
    // УСЛОВИЯ ИСПОЛЬЗОВАНИЯ СКИЛЛОВ
    // ============================================================
    private boolean shouldUseLaser(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (laserCooldown > 0 || isLeaping || isSpeedBlitzing || isLasering
                || isAttackingMelee || isRetreating) return false;
        return this.distanceToSqr(target) >= 196.0D;
    }

    private boolean shouldUseLeap(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (leapCooldown > 0 || isLeaping || isSpeedBlitzing || isLasering
                || isAttackingMelee || isRetreating) return false;
        return Math.abs(target.getY() - this.getY()) > LEAP_VERTICAL_THRESHOLD
                && this.distanceToSqr(target) <= 484.0D;
    }

    private boolean shouldUseSpeedBlitz(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (speedBlitzCooldown > 0 || isLeaping || isSpeedBlitzing || isLasering
                || isAttackingMelee || isRetreating) return false;
        double distSq = this.distanceToSqr(target);
        double dy = Math.abs(target.getY() - this.getY());
        return dy <= 5.0D && distSq > 9.0D && distSq < 225.0D;
    }

    private int countNearbyEnemies(double radius) {
        return this.level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius), this::isValidPriorityTarget).size();
    }

    // ============================================================
    // ФИЗИКА И УТИЛИТЫ (ломка блоков теперь вызывается ТОЛЬКО из скиллов и при застревании)
    // ============================================================
    private static float easeOutQuad(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static float easeInQuad(float t) {
        return t * t;
    }

    private void breakBlocksAlongLine(Vec3 from, Vec3 to, int horizontalRadius,
                                      int below, int above, boolean breakFloorLayer) {
        if (level.isClientSide) {
            return;
        }
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(distance / 0.5D));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = from.lerp(to, t);
            BlockPos center = new BlockPos(point.x, point.y, point.z);
            for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    for (int dy = -below; dy <= above; dy++) {
                        destroyNonBedrock(center.offset(dx, dy, dz));
                    }
                    if (breakFloorLayer) {
                        destroyNonBedrock(center.offset(dx, -below - 1, dz));
                    }
                }
            }
        }
    }

    private void destroyNonBedrock(BlockPos pos) {
        if (level == null) {
            return;
        }
        if (level.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
            return;
        }
        if (!level.getBlockState(pos).isAir()) {
            level.destroyBlock(pos, false);
        }
    }

    private void breakImpactArea3x3(BlockPos center) {
        BlockPos base = center.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                destroyNonBedrock(base.offset(dx, 0, dz));
                destroyNonBedrock(base.offset(dx, 1, dz));
            }
        }
    }

    // ============================================================
    // ПРЫЖОК
    // ============================================================
    private void forceLeapToTarget(LivingEntity target) {
        Vec3 start = this.position();
        Vec3 targetPos = target.position();
        Vec3 flat = new Vec3(targetPos.x - start.x, 0, targetPos.z - start.z);
        double maxDistance = 12.0D;
        if (flat.length() > maxDistance) {
            flat = flat.normalize().scale(maxDistance);
        }

        this.leapStartPos = start;
        this.leapLandPos = new Vec3(start.x + flat.x, targetPos.y, start.z + flat.z);
        this.leapPeakPos = this.leapStartPos.lerp(this.leapLandPos, 0.5D).add(0, 8.0D, 0);
        this.isLeaping = true;
        this.leapTimer = 0;
        this.leapTargetPos = this.leapLandPos;
        this.consecutiveMeleeHits = 0;
        this.meleeStaleTicks = 0;

        startSyncedAttackAnimation(ANIM_LEAP, 64);
        playSound(ModSounds.MAHORAGA_JUMP.get(), 1.5F, 1.0F);
        leapCooldown = secondPhase ? LEAP_CD_P2 : LEAP_CD_P1;
    }

    private void updateLeapPhysics() {
        if (leapStartPos == null || leapPeakPos == null || leapLandPos == null) return;

        Vec3 previousPos = this.position();

        if (leapTimer < 20) {
            setDeltaMovement(Vec3.ZERO);
        } else if (leapTimer < 40) {
            float t = (leapTimer - 20) / 20.0F;
            float eased = easeOutQuad(t);
            Vec3 pos = leapStartPos.lerp(leapPeakPos, eased);
            setPos(pos.x, pos.y, pos.z);
            this.hurtMarked = true;
            // Ломаем по траектории (это часть скилла)
            breakBlocksAlongLine(previousPos, pos, 1, 1, 3, false);
        } else if (leapTimer < 44) {
            setPos(leapPeakPos.x, leapPeakPos.y, leapPeakPos.z);
            setDeltaMovement(Vec3.ZERO);
            this.hurtMarked = true;
        } else if (leapTimer < 54) {
            float t = (leapTimer - 44) / 10.0F;
            float eased = easeInQuad(t);
            Vec3 pos = leapPeakPos.lerp(leapLandPos, eased);
            setPos(pos.x, pos.y, pos.z);
            this.hurtMarked = true;
            breakBlocksAlongLine(previousPos, pos, 1, 1, 3, false);

            if (leapTimer == 53) {
                level.getEntitiesOfClass(LivingEntity.class,
                                getBoundingBox().inflate(3), this::isValidPriorityTarget)
                        .forEach(e -> e.hurt(DamageSource.mobAttack(this),
                                secondPhase ? 40.0F : 25.0F));
                breakImpactArea3x3(this.blockPosition());

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                            getX(), getY(), getZ(), 8, 1.2D, 0.3D, 1.2D, 0.0D);
                }
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void endLeap() {
        isLeaping = false;
        leapTimer = 0;
        leapTargetPos = null;
        leapStartPos = null;
        leapPeakPos = null;
        leapLandPos = null;
        leapCooldown = secondPhase ? LEAP_CD_P2 : LEAP_CD_P1;
        setDeltaMovement(Vec3.ZERO);
    }

    // ============================================================
    // СПИДБЛИЦ
    // ============================================================
    private void forceSpeedBlitzToTarget(LivingEntity target) {
        this.isSpeedBlitzing = true;
        this.speedBlitzTimer = 0;
        this.blitzTargetPos = target.position();
        this.consecutiveMeleeHits = 0;
        this.meleeStaleTicks = 0;

        startSyncedAttackAnimation(ANIM_SPEED_BLITZ, 90);
        playSound(ModSounds.MAHORAGA_SPEEDBLITZ.get(), 1.5F, 1.0F);
        speedBlitzCooldown = secondPhase ? BLITZ_CD_P2 : BLITZ_CD_P1;
    }

    private void updateSpeedBlitzPhysics() {
        if (blitzTargetPos == null) return;

        if (speedBlitzTimer < 20) {
            setDeltaMovement(0, 0, 0);
        } else if (speedBlitzTimer < 36) {
            Vec3 previousPos = this.position();
            Vec3 diff = blitzTargetPos.subtract(this.position());
            if (diff.lengthSqr() > 0.25D) {
                Vec3 motion = diff.normalize().scale(secondPhase ? 3.4D : 2.8D);
                setDeltaMovement(motion);

                Vec3 predictedPos = this.position().add(motion);
                // Ломаем по траектории спидблица (часть скилла)
                breakBlocksAlongLine(previousPos, predictedPos, 1, 1, 2, true);

                AABB rushBox = new AABB(
                        Math.min(previousPos.x, predictedPos.x) - 1.2D,
                        Math.min(previousPos.y, predictedPos.y) - 1.2D,
                        Math.min(previousPos.z, predictedPos.z) - 1.2D,
                        Math.max(previousPos.x, predictedPos.x) + 1.2D,
                        Math.max(previousPos.y, predictedPos.y) + 1.2D,
                        Math.max(previousPos.z, predictedPos.z) + 1.2D
                );
                level.getEntitiesOfClass(LivingEntity.class, rushBox,
                        this::isValidPriorityTarget).forEach(e -> {
                    e.hurt(DamageSource.mobAttack(this), secondPhase ? 14.0F : 8.0F);
                    Vec3 push = e.position().subtract(position()).normalize().scale(1.2D);
                    e.setDeltaMovement(push.x, 0.35D, push.z);
                });
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
        } else if (speedBlitzTimer == 36) {
            setDeltaMovement(0, 0, 0);
            AABB catchBox = this.getBoundingBox().inflate(2.0D);
            List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class,
                    catchBox, this::isValidPriorityTarget);
            if (!hit.isEmpty()) {
                grabbedEntity = hit.get(0);
                hasGrabbed = true;
                grabTimer = 0;
                grabbedEntity.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false));
                grabbedEntity.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 40, 255, false, false));
                grabbedEntity.addEffect(new MobEffectInstance(
                        MobEffects.JUMP, 40, -10, false, false));
            } else {
                endSpeedBlitz();
            }
        } else if (hasGrabbed && grabbedEntity != null && grabbedEntity.isAlive()) {
            Vec3 holdPos = position().add(getLookAngle().scale(2.0D)).add(0, 1.2D, 0);
            grabbedEntity.teleportTo(holdPos.x, holdPos.y, holdPos.z);
            grabbedEntity.setDeltaMovement(0, 0, 0);
            grabTimer++;

            if (grabTimer == 8 || grabTimer == 16) {
                grabbedEntity.hurt(DamageSource.mobAttack(this), secondPhase ? 16.0F : 10.0F);
            }

            if (grabTimer >= 24) {
                Vec3 throwDir = getLookAngle().scale(2.8D);
                grabbedEntity.setDeltaMovement(throwDir.x, 0.8D, throwDir.z);
                grabbedEntity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                grabbedEntity.removeEffect(MobEffects.WEAKNESS);
                grabbedEntity.removeEffect(MobEffects.JUMP);
                grabbedEntity = null;
                endSpeedBlitz();
            }
        } else if (hasGrabbed) {
            endSpeedBlitz();
        }
    }

    private void endSpeedBlitz() {
        isSpeedBlitzing = false;
        hasGrabbed = false;
        grabbedEntity = null;
        grabTimer = 0;
        speedBlitzTimer = 0;
        speedBlitzCooldown = secondPhase ? BLITZ_CD_P2 : BLITZ_CD_P1;
        setDeltaMovement(0, 0, 0);
    }

    // ============================================================
    // МИЛИ-АТАКА
    // ============================================================
    private void updateMeleeAttack() {
        if (!isAttackingMelee) {
            if (meleeAttackTimer > 0) meleeAttackTimer--;
            return;
        }
        meleeAttackTimer--;

        int damageTick = currentMeleeAnim.equals("double") ? 16 : 10;
        if (meleeAttackTimer == damageTick && meleeTarget != null
                && meleeTarget.isAlive() && isValidPriorityTarget(meleeTarget)) {
            float damage = currentMeleeAnim.equals("double")
                    ? (secondPhase ? 40.0F : 25.0F)
                    : (secondPhase ? 24.0F : 15.0F);
            meleeTarget.hurt(DamageSource.mobAttack(this), damage);
            consecutiveMeleeHits++;

            if (currentMeleeAnim.equals("double")) {
                meleeTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION,
                            meleeTarget.getX(), meleeTarget.getY() + 1, meleeTarget.getZ(),
                            3, 0.3D, 0.3D, 0.3D, 0.0D);
                }
            }

            LOGGER.info("[Mahoraga] Melee {} dealt to {} (combo #{})",
                    damage, meleeTarget.getDisplayName().getString(), consecutiveMeleeHits);
        }

        if (meleeTarget != null && !meleeTarget.isAlive()) {
            clearTargetPriority(meleeTarget.getUUID());
            consecutiveMeleeHits = 0;
            meleeStaleTicks = 0;
        }

        if (meleeAttackTimer <= 0) {
            isAttackingMelee = false;
            meleeTarget = null;
            meleeAttackTimer = MELEE_COOLDOWN;
        }
    }

    private void startMeleeAttack(LivingEntity target) {
        if (meleeAttackTimer > 0 || isAttackingMelee || target == null
                || !target.isAlive() || !isValidPriorityTarget(target)) {
            return;
        }
        double r = random.nextDouble();
        int animId;
        if (r < 0.4D) {
            currentMeleeAnim = "left";
            animId = ANIM_MELEE_LEFT;
        } else if (r < 0.8D) {
            currentMeleeAnim = "right";
            animId = ANIM_MELEE_RIGHT;
        } else {
            currentMeleeAnim = "double";
            animId = ANIM_MELEE_DOUBLE;
        }

        isAttackingMelee = true;
        meleeTarget = target;
        meleeAttackTimer = currentMeleeAnim.equals("double") ? 18 : 12;
        startSyncedAttackAnimation(animId, meleeAttackTimer);
        playSound(ModSounds.MAHORAGA_ATTACK.get(), 1.5F, 1.0F);
    }

    // ============================================================
    // ВЛАДЕЛЕЦ И ЦЕЛИ
    // ============================================================
    public void setOwner(UUID uuid) {
        this.ownerUUID = uuid;
        LOGGER.info("[Mahoraga] Owner set to {}", uuid);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setSummonToken(UUID uuid) {
        this.getPersistentData().putUUID(SUMMON_TOKEN_KEY, uuid);
    }

    @Nullable
    public UUID getSummonToken() {
        if (this.getPersistentData().hasUUID(SUMMON_TOKEN_KEY)) {
            return this.getPersistentData().getUUID(SUMMON_TOKEN_KEY);
        }
        return null;
    }

    public void addHostileByOwner(LivingEntity target) {
        if (ownerUUID == null || target == null || target == this
                || target.getUUID().equals(ownerUUID)) {
            return;
        }
        int newPriority = targetPriority.getOrDefault(target.getUUID(), 0) + 1;
        targetPriority.put(target.getUUID(), newPriority);
        targetOrder.putIfAbsent(target.getUUID(), nextTargetOrder++);
        LOGGER.info("[Mahoraga] Priority updated: {} -> {}",
                target.getDisplayName().getString(), newPriority);
        updatePriorityTargetSelection();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == null || target == this) return false;
        if (ownerUUID != null && target.getUUID().equals(ownerUUID)) return false;
        return super.canAttack(target);
    }

    // ============================================================
    // ЗВУКИ
    // ============================================================
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MAHORAGA_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.MAHORAGA_DEATH.get();
    }

    // ============================================================
    // АДАПТАЦИЯ И УРОН
    // ============================================================
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Чигур не становится категорией адаптации: ни колесо, ни immunity,
        // ни переход фазы Махораги не срабатывают от его дробовика/пневматики.
        if (source.getEntity() instanceof AntonChigurEntity
                || source.getDirectEntity() instanceof AntonChigurEntity) {
            return super.hurt(source, amount);
        }

        String category = DamageTypeClassifier.classify(source);

        if (!this.level.isClientSide && source.getEntity() instanceof ServerPlayer sp) {
            damagedPlayers.add(sp.getUUID());
        }

        if (secondPhase) {
            return super.hurt(source, amount);
        }

        if (adaptedDamageTypes.contains(category)) {
            return false;
        }

        float healthBefore = getHealth();
        float maxAllowed = Math.max(0.1F, healthBefore - currentHealthThreshold);
        float clamped = Math.min(amount, maxAllowed);

        boolean forceSecondPhase = adaptationStage >= 9 && healthBefore - clamped <= 0.5F;
        if (forceSecondPhase) {
            clamped = Math.max(0.0F, healthBefore - 1.0F);
        }

        boolean result = super.hurt(source, clamped);

        if (!level.isClientSide) {
            if (forceSecondPhase) {
                triggerAdaptation(category);
                return result;
            }
            if (getHealth() <= currentHealthThreshold + 0.5F) {
                triggerAdaptation(category);
            }
        }

        return result;
    }

    private void triggerAdaptation(String category) {
        if (secondPhase) {
            return;
        }
        if (adaptedDamageTypes.contains(category) && adaptationStage < 10) {
            return;
        }

        adaptedDamageTypes.add(category);
        adaptationStage++;
        currentHealthThreshold -= SEGMENT;

        if (adaptationStage >= 10) {
            enterSecondPhase();
            return;
        }

        isAdaptingWheel = true;
        adaptWheelTimer = ADAPT_WHEEL_DURATION;

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 2.0D, getZ(), 50, 0.8D, 0.8D, 0.8D, 0.15D);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    getX(), getY() + 1.0D, getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
        }
        playSound(ModSounds.MAHORAGA_ADAPT.get(), 2.0F, 0.5F);

        String russian = DamageTypeClassifier.russianName(category);
        for (ServerPlayer p : bossEvent.getPlayers()) {
            p.displayClientMessage(Component.literal(
                    "§6[Махорага] §cАдаптировался: §e" + russian
                            + " §7(" + adaptationStage + "/10)"), false);
        }
    }

    private void enterSecondPhase() {
        if (secondPhase) {
            return;
        }
        secondPhase = true;
        adaptedDamageTypes.clear();
        currentHealthThreshold = 0.0F;
        isAdaptingWheel = false;
        adaptWheelTimer = 0;

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(PHASE_TWO_MAX_HP);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(PHASE_TWO_MOVE_SPEED);
        }
        this.setHealth(PHASE_TWO_MAX_HP);

        consumeOwnerSummonScroll();

        for (ServerPlayer p : bossEvent.getPlayers()) {
            p.displayClientMessage(Component.literal(
                    "§4§l[Махорага] Вошёл во 2 фазу! §c100 HP, скорость x2, больше не адаптируется."), false);
        }
        LOGGER.info("[Mahoraga] SECOND PHASE ACTIVATED");
    }

    // ============================================================
    // СМЕРТЬ И УДАЛЕНИЕ
    // ============================================================
    @Override
    public void die(DamageSource source) {
        musicTimer = Integer.MAX_VALUE;
        stopAmbientMusic();
        consumeOwnerSummonScroll();

        if (!level.isClientSide && level.getServer() != null) {
            if (ownerUUID != null) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
                if (owner != null) {
                    ZitraksMode.MAHORAGA_KILLED.trigger(owner);
                }
            }
            for (UUID uuid : damagedPlayers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    ZitraksMode.MAHORAGA_KILLED.trigger(player);
                }
            }
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level.isClientSide) {
            stopAmbientMusic();
        }
        super.remove(reason);
    }

    private void stopAmbientMusic() {
        if (ambientStopped) {
            return;
        }
        ambientStopped = true;
        if (this.level.getServer() == null) {
            return;
        }
        for (ServerPlayer serverPlayer : this.level.getServer().getPlayerList().getPlayers()) {
            serverPlayer.connection.send(new ClientboundStopSoundPacket(
                    ModSounds.MAHORAGA_AMBIENT.get().getLocation(), SoundSource.MUSIC));
        }
    }

    private void consumeOwnerSummonScroll() {
        if (ownerScrollConsumed || this.level.isClientSide
                || ownerUUID == null || this.level.getServer() == null) {
            return;
        }
        ServerPlayer owner = this.level.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) {
            return;
        }
        UUID token = getSummonToken();
        if (token == null) {
            return;
        }
        if (consumeMatchingScroll(owner.getInventory().items, ownerUUID, token)
                || consumeMatchingScroll(owner.getInventory().offhand, ownerUUID, token)) {
            owner.getInventory().setChanged();
            owner.containerMenu.broadcastChanges();
            ownerScrollConsumed = true;
        }
    }

    private boolean consumeMatchingScroll(List<ItemStack> stacks, UUID ownerUUID, UUID token) {
        for (ItemStack stack : stacks) {
            if (MahoragaSummonItem.matchesSummonScroll(stack, ownerUUID, token)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // РЕГИСТРАЦИЯ ЦЕЛЕЙ (Goals)
    // ============================================================
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 1.05D));
        this.goalSelector.addGoal(2, new ScreamAttackGoal(this));
        this.goalSelector.addGoal(3, new LaserAttackGoal(this));
        this.goalSelector.addGoal(4, new LeapAttackGoal(this));
        this.goalSelector.addGoal(5, new SpeedBlitzGoal(this));
        this.goalSelector.addGoal(6, new MahoragaMeleeAttackGoal(this, 1.15D, true));
        // BreakObstacles УБРАН — больше не ломаем блоки во время обычной ходьбы
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    // ============================================================
    // GOAL: Следование за владельцем
    // ============================================================
    private static class FollowOwnerGoal extends Goal {
        private final MahoragaEntity mob;
        private final double speed;
        private Player owner;

        FollowOwnerGoal(MahoragaEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            owner = mob.getOwnerPlayer();
            return owner != null && owner.isAlive() && mob.getTarget() == null
                    && !mob.isLeaping && !mob.isSpeedBlitzing && !mob.isLasering;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && mob.getTarget() == null
                    && !mob.isLeaping && !mob.isSpeedBlitzing && !mob.isLasering;
        }

        @Override
        public void tick() {
            Vec3 look = owner.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0D, look.z);
            if (flat.lengthSqr() < 0.0001D) {
                flat = new Vec3(0.0D, 0.0D, 1.0D);
            }
            flat = flat.normalize();
            Vec3 behind = owner.position().subtract(flat.scale(1.7D));
            double distSq = mob.distanceToSqr(behind.x, behind.y, behind.z);
            mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
            if (distSq > 1.5D) {
                mob.getNavigation().moveTo(behind.x, owner.getY(), behind.z, speed);
            } else {
                mob.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    // ============================================================
    // GOAL: Мили-атака (без ломки блоков!)
    // ============================================================
    private static class MahoragaMeleeAttackGoal extends MeleeAttackGoal {
        MahoragaMeleeAttackGoal(PathfinderMob mob, double speed, boolean followTarget) {
            super(mob, speed, followTarget);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distSq) {
            MahoragaEntity mahoraga = (MahoragaEntity) this.mob;
            if (mahoraga.isRetreating) {
                return;
            }
            double reach = this.getAttackReachSqr(target);
            if (distSq <= reach
                    && this.getTicksUntilNextAttack() <= 0
                    && !mahoraga.isAttackingMelee
                    && !mahoraga.isLasering
                    && !mahoraga.isLeaping
                    && !mahoraga.isSpeedBlitzing) {
                this.resetAttackCooldown();
                mahoraga.startMeleeAttack(target);
            }
        }
    }

    // ============================================================
    // GOAL: Спидблиц
    // ============================================================
    private static class SpeedBlitzGoal extends Goal {
        private final MahoragaEntity mob;

        SpeedBlitzGoal(MahoragaEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.shouldUseSpeedBlitz(mob.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isSpeedBlitzing;
        }

        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            mob.forceSpeedBlitzToTarget(target);
            LOGGER.info("[Mahoraga] Speed Blitz start -> {}",
                    target.getDisplayName().getString());
        }

        @Override
        public void stop() {
            if (mob.isSpeedBlitzing) mob.endSpeedBlitz();
        }
    }

    // ============================================================
    // GOAL: Прыжок
    // ============================================================
    private static class LeapAttackGoal extends Goal {
        private final MahoragaEntity mob;

        LeapAttackGoal(MahoragaEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.shouldUseLeap(mob.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return mob.isLeaping;
        }

        @Override
        public void start() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.forceLeapToTarget(t);
            LOGGER.info("[Mahoraga] Leap start -> {}", t.getDisplayName().getString());
        }

        @Override
        public void stop() {
            if (mob.isLeaping) mob.endLeap();
        }
    }

    // ============================================================
    // GOAL: Крик
    // ============================================================
    private static class ScreamAttackGoal extends Goal {
        private final MahoragaEntity mob;

        ScreamAttackGoal(MahoragaEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.screamCooldown <= 0
                    && !mob.isLeaping
                    && !mob.isSpeedBlitzing
                    && !mob.isLasering
                    && !mob.isAttackingMelee
                    && !mob.isRetreating
                    && mob.countNearbyEnemies(Scream_RADIUS) >= SCREAM_MIN_TARGETS;
        }

        @Override
        public void start() {
            LOGGER.info("[Mahoraga] Scream!");
            mob.isScreaming = true;
            mob.startSyncedAttackAnimation(ANIM_SCREAM, 30);
            mob.playSound(ModSounds.MAHORAGA_SCREAM.get(), 2.5F, 0.8F);
            mob.level.getEntitiesOfClass(LivingEntity.class,
                    mob.getBoundingBox().inflate(Scream_RADIUS),
                    mob::isValidPriorityTarget).forEach(e -> {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            });
            mob.screamCooldown = mob.secondPhase ? SCREAM_CD_P2 : SCREAM_CD_P1;
        }

        @Override
        public void stop() {
            mob.isScreaming = false;
        }
    }

    // ============================================================
    // GOAL: Лазер
    // ============================================================
    private static class LaserAttackGoal extends Goal {
        private final MahoragaEntity mob;
        private int delayTimer = 0;
        private boolean waitingToFire = false;
        private LivingEntity savedTarget = null;

        LaserAttackGoal(MahoragaEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.shouldUseLaser(mob.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return waitingToFire;
        }

        @Override
        public void start() {
            savedTarget = mob.getTarget();
            if (savedTarget == null) return;

            LOGGER.info("[Mahoraga] Laser winding up -> {}",
                    savedTarget.getDisplayName().getString());
            mob.isLasering = true;
            waitingToFire = true;
            delayTimer = mob.secondPhase ? 10 : 16;
            mob.consecutiveMeleeHits = 0;
            mob.meleeStaleTicks = 0;
            mob.startSyncedAttackAnimation(ANIM_LASER, mob.secondPhase ? 24 : 32);
            mob.playSound(ModSounds.MAHORAGA_LASER.get(), 2.0F, 1.0F);
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(savedTarget, 30, 30);
        }

        @Override
        public void tick() {
            if (savedTarget != null && savedTarget.isAlive()) {
                mob.getLookControl().setLookAt(savedTarget, 30, 30);
                mob.getNavigation().stop();
            }
            if (waitingToFire) {
                delayTimer--;
                if (delayTimer <= 0) {
                    fireLaser();
                    waitingToFire = false;
                }
            }
        }

        private void fireLaser() {
            if (mob.level.isClientSide || savedTarget == null || !savedTarget.isAlive()) {
                mob.isLasering = false;
                return;
            }
            LOGGER.info("[Mahoraga] Laser FIRE -> {}",
                    savedTarget.getDisplayName().getString());

            Vec3 origin = new Vec3(mob.getX(), mob.getY() + 2.0D, mob.getZ());
            Vec3 flatDir = new Vec3(
                    savedTarget.getX() - mob.getX(), 0.0D,
                    savedTarget.getZ() - mob.getZ());
            if (flatDir.lengthSqr() < 0.0001D) {
                flatDir = mob.getLookAngle();
            }
            flatDir = new Vec3(flatDir.x, 0.0D, flatDir.z).normalize();
            Vec3 targetPos = origin.add(flatDir.scale(20.0D));

            LaserBeamEntity beam = ModEntities.LASER_BEAM.get().create(mob.level);
            if (beam != null) {
                beam.init(origin, targetPos, mob);
                beam.setMaxLength(20.0F);
                beam.setDamage(mob.secondPhase ? 28.0F : 18.0F);
                beam.setLifeTime(mob.secondPhase ? 28 : 22);
                mob.level.addFreshEntity(beam);
            }

            if (mob.level instanceof ServerLevel serverLevel) {
                int steps = 40;
                for (int i = 0; i <= steps; i++) {
                    double t = i / (double) steps;
                    Vec3 p = origin.lerp(targetPos, t);
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            p.x, p.y, p.z, 3, 0.05D, 0.05D, 0.05D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            p.x, p.y, p.z, 2, 0.03D, 0.03D, 0.03D, 0.0D);
                }
            }

            mob.laserCooldown = mob.secondPhase ? LASER_CD_P2 : LASER_CD_P1;
            mob.isLasering = false;
        }

        @Override
        public void stop() {
            mob.isLasering = false;
            waitingToFire = false;
            savedTarget = null;
        }
    }

    // ============================================================
    // КЛАССИФИКАТОР УРОНА
    // ============================================================
    public static class DamageTypeClassifier {
        public static String classify(DamageSource source) {
            if (source == DamageSource.IN_FIRE
                    || source == DamageSource.ON_FIRE
                    || source == DamageSource.LAVA
                    || source == DamageSource.HOT_FLOOR) return "fire";
            if (source == DamageSource.LIGHTNING_BOLT) return "lightning";
            if (source == DamageSource.WITHER) return "wither";
            if (source == DamageSource.DROWN) return "drown";
            if (source == DamageSource.IN_WALL) return "suffocation";
            if (source == DamageSource.STARVE) return "starve";
            if (source == DamageSource.FREEZE) return "freeze";
            if (source == DamageSource.CACTUS) return "cactus";
            if (source == DamageSource.SWEET_BERRY_BUSH) return "berry";
            if (source.isFire()) return "fire";
            if (source.isFall()) return "fall";
            if (source.isExplosion()) return "explosion";
            if (source.isMagic()) return "magic";
            if (source.isProjectile()) return "projectile";
            if ("fallingBlock".equals(source.getMsgId())) return "falling_block";
            if (source.getDirectEntity() instanceof LivingEntity) return "melee";
            return "generic";
        }

        public static String russianName(String category) {
            return switch (category) {
                case "melee" -> "Ближний бой";
                case "lightning" -> "Молния";
                case "wither" -> "Иссушение";
                case "drown" -> "Утопление";
                case "suffocation" -> "Удушение";
                case "starve" -> "Голод";
                case "freeze" -> "Замерзание";
                case "cactus" -> "Кактус";
                case "berry" -> "Куст ягод";
                case "fire" -> "Огонь";
                case "fall" -> "Падение";
                case "explosion" -> "Взрыв";
                case "magic" -> "Магия";
                case "projectile" -> "Снаряд";
                case "falling_block" -> "Падающий блок";
                default -> "Прочее";
            };
        }
    }

    // ============================================================
    // СОХРАНЕНИЕ / ЗАГРУЗКА NBT
    // ============================================================
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (getSummonToken() != null) tag.putUUID("SummonToken", getSummonToken());
        tag.putInt("AdaptationStage", adaptationStage);
        tag.putBoolean("SecondPhase", secondPhase);
        tag.putBoolean("OwnerScrollConsumed", ownerScrollConsumed);
        tag.putBoolean("SummonLandingArmed", summonLandingArmed);
        tag.putBoolean("SummonLandingProcessed", summonLandingProcessed);
        tag.putFloat("HealthThreshold", currentHealthThreshold);
        tag.putString("AdaptedTypes", String.join(",", adaptedDamageTypes));
        tag.putFloat("WheelRotation", wheelRotation);
        tag.putInt("LaserCooldown", laserCooldown);
        tag.putInt("ScreamCooldown", screamCooldown);
        tag.putLong("NextTargetOrder", nextTargetOrder);

        CompoundTag prioTag = new CompoundTag();
        targetPriority.forEach((uuid, prio) -> prioTag.putInt(uuid.toString(), prio));
        tag.put("TargetPriority", prioTag);

        CompoundTag orderTag = new CompoundTag();
        targetOrder.forEach((uuid, order) -> orderTag.putLong(uuid.toString(), order));
        tag.put("TargetOrder", orderTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        if (tag.hasUUID("SummonToken")) setSummonToken(tag.getUUID("SummonToken"));
        adaptationStage = tag.getInt("AdaptationStage");
        secondPhase = tag.getBoolean("SecondPhase");
        ownerScrollConsumed = tag.getBoolean("OwnerScrollConsumed");
        summonLandingArmed = tag.getBoolean("SummonLandingArmed");
        summonLandingProcessed = tag.getBoolean("SummonLandingProcessed");
        currentHealthThreshold = tag.getFloat("HealthThreshold");
        laserCooldown = tag.getInt("LaserCooldown");
        screamCooldown = tag.getInt("ScreamCooldown");
        nextTargetOrder = tag.getLong("NextTargetOrder");
        if (nextTargetOrder <= 0L) nextTargetOrder = 1L;

        String types = tag.getString("AdaptedTypes");
        adaptedDamageTypes.clear();
        if (!types.isEmpty()) {
            adaptedDamageTypes.addAll(Arrays.asList(types.split(",")));
        }
        wheelRotation = tag.getFloat("WheelRotation");

        targetPriority.clear();
        targetOrder.clear();
        if (tag.contains("TargetPriority")) {
            CompoundTag prioTag = tag.getCompound("TargetPriority");
            prioTag.getAllKeys().forEach(key ->
                    targetPriority.put(UUID.fromString(key), prioTag.getInt(key)));
        }
        if (tag.contains("TargetOrder")) {
            CompoundTag orderTag = tag.getCompound("TargetOrder");
            orderTag.getAllKeys().forEach(key ->
                    targetOrder.put(UUID.fromString(key), orderTag.getLong(key)));
        }

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                    secondPhase ? PHASE_TWO_MAX_HP : BASE_MAX_HP);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                    secondPhase ? PHASE_TWO_MOVE_SPEED : BASE_MOVE_SPEED);
        }
    }

    // ============================================================
    // ЛУТ
    // ============================================================
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        RandomSource rand = level.random;
        try {
            ItemStack guaranteedScroll = new ItemStack(ModItems.MAHORAGA_SCROLL.get());
            MahoragaSummonItem.setStackCooldown(guaranteedScroll, this.level,
                    MahoragaSummonItem.COOLDOWN_SEAL_TICKS);
            spawnAtLocation(guaranteedScroll);
            spawnAtLocation(new ItemStack(Items.DIAMOND, 64));
            spawnAtLocation(new ItemStack(Items.NETHERITE_INGOT, 64));
            spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 42));
            spawnAtLocation(new ItemStack(ModItems.STEEL_BALL.get(), 2));
            spawnAtLocation(new ItemStack(Items.FEATHER, 4));
            spawnAtLocation(new ItemStack(Items.EMERALD, 128));

            if (rand.nextFloat() < 0.5F)
                spawnAtLocation(new ItemStack(Items.SCULK_SHRIEKER, 32));
            if (rand.nextFloat() < 0.5F) {
                List<Item> eggs = ForgeRegistries.ITEMS.getValues().stream()
                        .filter(i -> i instanceof ForgeSpawnEggItem)
                        .collect(Collectors.toList());
                if (!eggs.isEmpty())
                    spawnAtLocation(new ItemStack(eggs.get(rand.nextInt(eggs.size())), 64));
            }
            if (rand.nextFloat() < 0.5F)
                spawnAtLocation(new ItemStack(Items.OBSIDIAN, 64));
            if (rand.nextFloat() < 0.5F)
                spawnAtLocation(new ItemStack(Items.BEDROCK, 1));

            if (rand.nextFloat() < 0.3F) {
                spawnAtLocation(createEnchantedItem(Items.DIAMOND_HELMET));
                spawnAtLocation(createEnchantedItem(Items.DIAMOND_CHESTPLATE));
                spawnAtLocation(createEnchantedItem(Items.DIAMOND_LEGGINGS));
                spawnAtLocation(createEnchantedItem(Items.DIAMOND_BOOTS));
            }
            if (rand.nextFloat() < 0.3F)
                spawnAtLocation(createEnchantedItem(Items.NETHERITE_HOE));
            if (rand.nextFloat() < 0.3F)
                spawnAtLocation(new ItemStack(ModItems.KORAN.get()));
            if (rand.nextFloat() < 0.3F) {
                spawnAtLocation(buildDyeShulker());
            }

            if (rand.nextFloat() < 0.05F) {
                spawnAtLocation(createEnchantedItem(Items.NETHERITE_HELMET));
                spawnAtLocation(createEnchantedItem(Items.NETHERITE_CHESTPLATE));
                spawnAtLocation(createEnchantedItem(Items.NETHERITE_LEGGINGS));
                spawnAtLocation(createEnchantedItem(Items.NETHERITE_BOOTS));
            }
            if (rand.nextFloat() < 0.05F)
                spawnAtLocation(new ItemStack(Items.NETHER_STAR, 64));
            if (rand.nextFloat() < 0.05F)
                spawnAtLocation(new ItemStack(ModItems.DEATH_NOTE_REQUIEM.get()));
            if (rand.nextFloat() < 0.05F)
                spawnAtLocation(new ItemStack(Items.BEACON, 12));
            if (rand.nextFloat() < 0.05F)
                spawnAtLocation(buildConcreteShulker());
            if (rand.nextFloat() < 0.05F)
                spawnAtLocation(new ItemStack(ModItems.MAHORAGA_SCROLL.get()));
        } catch (Exception e) {
            LOGGER.error("[Mahoraga] ERROR in dropCustomDeathLoot: {}", e.getMessage(), e);
        }
    }

    private ItemStack buildDyeShulker() {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        CompoundTag tag = shulker.getOrCreateTag();
        ListTag items = new ListTag();
        int slot = 0;
        for (DyeColor color : DyeColor.values()) {
            ItemStack dyeStack = new ItemStack(DyeItem.byColor(color), 64);
            CompoundTag itemTag = new CompoundTag();
            dyeStack.save(itemTag);
            itemTag.putByte("Slot", (byte) slot);
            items.add(itemTag);
            slot++;
        }
        ItemStack loomStack = new ItemStack(Items.LOOM, 1);
        CompoundTag loomTag = new CompoundTag();
        loomStack.save(loomTag);
        loomTag.putByte("Slot", (byte) slot);
        items.add(loomTag);
        slot++;
        ItemStack bannerStack = new ItemStack(Items.WHITE_BANNER, 64);
        CompoundTag bannerTag = new CompoundTag();
        bannerStack.save(bannerTag);
        bannerTag.putByte("Slot", (byte) slot);
        items.add(bannerTag);

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("Items", items);
        int colorId = this.random.nextInt(15) + 1;
        blockEntityTag.putInt("Color", colorId);
        tag.put("BlockEntityTag", blockEntityTag);
        return shulker;
    }

    private ItemStack buildConcreteShulker() {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        CompoundTag tag = shulker.getOrCreateTag();
        ListTag items = new ListTag();
        for (int i = 0; i < 27; i++) {
            ItemStack concrete = new ItemStack(Items.WHITE_CONCRETE, 64);
            CompoundTag itemTag = new CompoundTag();
            concrete.save(itemTag);
            itemTag.putByte("Slot", (byte) i);
            items.add(itemTag);
        }
        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("Items", items);
        blockEntityTag.putInt("Color", DyeColor.WHITE.getId());
        tag.put("BlockEntityTag", blockEntityTag);
        return shulker;
    }

    private ItemStack createEnchantedItem(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.UNBREAKING, 3);
        stack.enchant(Enchantments.MENDING, 1);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        return stack;
    }
}
