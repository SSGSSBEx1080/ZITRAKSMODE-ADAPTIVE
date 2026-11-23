package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
import java.util.Optional;
import java.util.UUID;

public class MahoragaCinematicEntity extends PathfinderMob implements IAnimatable {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SUMMON_TOKEN_KEY = "MahoragaSummonToken";

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(MahoragaCinematicEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int cinematicTick = 0;
    private static final int CINEMATIC_DURATION = 460; // 23 сек
    private static final int SPAWN_TICK = 420;         // 21 сек
    private boolean hasSpawnedMahoraga = false;

    public MahoragaCinematicEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.of(uuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
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

    public void cancelSummon() {
        if (this.level.isClientSide) {
            return;
        }

        stopMahoragaMusic();
        LOGGER.info("[MahoragaCinematic] Summon cancelled by owner");
        this.discard();
    }

    private void stopMahoragaMusic() {
        if (this.level.getServer() == null) {
            return;
        }

        for (ServerPlayer serverPlayer : this.level.getServer().getPlayerList().getPlayers()) {
            serverPlayer.connection.send(
                    new ClientboundStopSoundPacket(
                            ModSounds.MAHORAGA_MUSIC.get().getLocation(),
                            SoundSource.MASTER
                    )
            );
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.setDeltaMovement(0, 0, 0);
        this.setNoGravity(true);

        if (this.level.isClientSide) return;

        cinematicTick++;

        if (cinematicTick == 1) {
            this.level.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    ModSounds.MAHORAGA_MUSIC.get(),
                    SoundSource.MASTER,
                    1.0f,
                    1.0f
            );
            LOGGER.info("[MahoragaCinematic] Music started");
        }

        if (cinematicTick == SPAWN_TICK && !hasSpawnedMahoraga) {
            spawnMahoraga();
            hasSpawnedMahoraga = true;
        }

        if (cinematicTick >= CINEMATIC_DURATION) {
            LOGGER.info("[MahoragaCinematic] Finished");
            this.discard();
        }
    }

    private void spawnMahoraga() {
        MahoragaEntity mahoraga = ModEntities.MAHORAGA.get().create(this.level);
        if (mahoraga == null) {
            LOGGER.error("[MahoragaCinematic] Failed to create MahoragaEntity");
            return;
        }

        double spawnX = this.getX();
        double spawnY = this.getY() + 67.0D;
        double spawnZ = this.getZ();

        mahoraga.setPos(spawnX, spawnY, spawnZ);
        mahoraga.setOwner(getOwnerUUID());
        mahoraga.setDeltaMovement(0, -2.5, 0);

        UUID token = getSummonToken();
        if (token != null) {
            mahoraga.getPersistentData().putUUID(SUMMON_TOKEN_KEY, token);
        }

        this.level.addFreshEntity(mahoraga);

        if (!this.level.isClientSide && getOwnerUUID() != null && this.level.getServer() != null) {
            ServerPlayer owner = this.level.getServer().getPlayerList().getPlayer(getOwnerUUID());
            if (owner != null) {
                ZitraksMode.MAHORAGA_SUMMONED.trigger(owner);
            }
        }

        LOGGER.info("[MahoragaCinematic] Spawned Mahoraga at {}, {}, {}", spawnX, spawnY, spawnZ);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(
                new AnimationController<>(this, "cinematic_controller", 0, this::predicate)
        );
    }

    private PlayState predicate(AnimationEvent<MahoragaCinematicEntity> event) {
        event.getController().setAnimation(
                new AnimationBuilder().addAnimation(
                        "animation.cinematic.idle",
                        ILoopType.EDefaultLoopTypes.LOOP
                )
        );
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
        if (tag.hasUUID("SummonToken")) {
            setSummonToken(tag.getUUID("SummonToken"));
        }

        cinematicTick = tag.getInt("CinematicTick");
        hasSpawnedMahoraga = tag.getBoolean("HasSpawnedMahoraga");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (getOwnerUUID() != null) {
            tag.putUUID("OwnerUUID", getOwnerUUID());
        }
        if (getSummonToken() != null) {
            tag.putUUID("SummonToken", getSummonToken());
        }

        tag.putInt("CinematicTick", cinematicTick);
        tag.putBoolean("HasSpawnedMahoraga", hasSpawnedMahoraga);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public void die(DamageSource source) {
    }

    @Override
    public void kill() {
    }

    public void push(EntityType<?> entityType) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        if (type == MoverType.SELF || type == MoverType.PLAYER) {
            super.move(type, movement);
        }
    }
}