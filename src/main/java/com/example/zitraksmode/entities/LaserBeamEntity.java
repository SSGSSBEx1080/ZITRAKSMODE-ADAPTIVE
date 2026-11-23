package com.example.zitraksmode.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class LaserBeamEntity extends Entity {

    private static final EntityDataAccessor<Float> END_X =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_LENGTH =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DIR_X =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z =
            SynchedEntityData.defineId(LaserBeamEntity.class, EntityDataSerializers.FLOAT);

    private int lifeTime = 22;
    private float maxLength = 20.0F;
    private float damage = 18.0F;
    private float growthPerTick = 1.35F;

    private Vec3 direction = Vec3.ZERO;
    private Vec3 originPos = Vec3.ZERO;

    @Nullable
    private Entity owner;

    private float lastDamagedLength = 0.0F;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    private static final Set<Block> IMMUNE_BLOCKS = Set.of(
            Blocks.BEDROCK,
            Blocks.BARRIER,
            Blocks.END_PORTAL_FRAME,
            Blocks.REINFORCED_DEEPSLATE
    );

    public LaserBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(END_X, 0.0F);
        this.entityData.define(END_Y, 0.0F);
        this.entityData.define(END_Z, 0.0F);
        this.entityData.define(CURRENT_LENGTH, 0.0F);
        this.entityData.define(DIR_X, 0.0F);
        this.entityData.define(DIR_Y, 0.0F);
        this.entityData.define(DIR_Z, 0.0F);
    }

    public void init(Vec3 origin, Vec3 target, @Nullable Entity shooter) {
        this.owner = shooter;
        this.originPos = origin;

        Vec3 diff = target.subtract(origin);
        if (diff.lengthSqr() < 0.0001D) {
            diff = new Vec3(1.0D, 0.0D, 0.0D);
        }

        this.direction = diff.normalize();
        this.maxLength = (float) Math.min(20.0D, diff.length());
        this.lastDamagedLength = 0.0F;
        this.hitEntityIds.clear();

        this.setPos(origin.x, origin.y, origin.z);

        this.entityData.set(DIR_X, (float) this.direction.x);
        this.entityData.set(DIR_Y, (float) this.direction.y);
        this.entityData.set(DIR_Z, (float) this.direction.z);
        this.entityData.set(CURRENT_LENGTH, 0.0F);
        updateEndPos();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setMaxLength(float maxLength) {
        this.maxLength = Math.max(0.0F, Math.min(20.0F, maxLength));
        float current = Math.min(this.entityData.get(CURRENT_LENGTH), this.maxLength);
        this.entityData.set(CURRENT_LENGTH, current);
        updateEndPos();
    }

    public void setLifeTime(int ticks) {
        this.lifeTime = ticks;
    }

    public void setOwner(@Nullable Entity owner) {
        this.owner = owner;
    }

    private void updateEndPos() {
        float curLen = this.entityData.get(CURRENT_LENGTH);
        Vec3 end = originPos.add(direction.scale(curLen));
        this.entityData.set(END_X, (float) end.x);
        this.entityData.set(END_Y, (float) end.y);
        this.entityData.set(END_Z, (float) end.z);
    }

    public Vec3 getStartPos() {
        return this.originPos.equals(Vec3.ZERO) ? this.position() : this.originPos;
    }

    public Vec3 getEndPos() {
        return new Vec3(
                this.entityData.get(END_X),
                this.entityData.get(END_Y),
                this.entityData.get(END_Z)
        );
    }

    public float getCurrentLength() {
        return this.entityData.get(CURRENT_LENGTH);
    }

    public Vec3 getBeamDirection() {
        return new Vec3(
                this.entityData.get(DIR_X),
                this.entityData.get(DIR_Y),
                this.entityData.get(DIR_Z)
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (direction.equals(Vec3.ZERO)) {
            direction = new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z));
            originPos = this.position();
            updateEndPos();
        }

        if (!level.isClientSide) {
            float currentLength = this.entityData.get(CURRENT_LENGTH);
            if (currentLength < maxLength) {
                float nextLength = Math.min(maxLength, currentLength + growthPerTick);
                this.entityData.set(CURRENT_LENGTH, nextLength);
                updateEndPos();
                damageAlongBeam(currentLength, nextLength);
                lastDamagedLength = nextLength;
            }

            if (this.level instanceof ServerLevel serverLevel) {
                spawnBeamParticles(serverLevel);
            }

            lifeTime--;
            if (lifeTime <= 0) {
                this.discard();
            }
        }
    }

    private void damageAlongBeam(float fromLen, float toLen) {
        float start = Math.max(0.0F, fromLen);
        float end = Math.max(start, toLen);

        for (float d = start; d <= end; d += 0.35F) {
            Vec3 pos = originPos.add(direction.scale(d));
            BlockPos bp = new BlockPos(pos.x, pos.y, pos.z);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos current = bp.offset(dx, dy, dz);
                        var state = level.getBlockState(current);
                        if (!state.isAir() && !IMMUNE_BLOCKS.contains(state.getBlock())) {
                            level.destroyBlock(current, false);
                        }
                    }
                }
            }

            AABB hitBox = new AABB(
                    pos.x - 0.9D, pos.y - 1.2D, pos.z - 0.9D,
                    pos.x + 0.9D, pos.y + 1.2D, pos.z + 0.9D
            );

            level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> {
                if (!e.isAlive()) return false;
                if (owner != null && e == owner) return false;
                return true;
            }).forEach(e -> {
                if (hitEntityIds.add(e.getId())) {
                    if (owner instanceof LivingEntity le) {
                        e.hurt(DamageSource.mobAttack(le), damage);
                    } else {
                        e.hurt(DamageSource.MAGIC, damage);
                    }
                }
            });
        }
    }

    private void spawnBeamParticles(ServerLevel level) {
        float curLen = this.entityData.get(CURRENT_LENGTH);
        if (curLen <= 0.05F) {
            return;
        }

        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vec3 up = direction.cross(side).normalize();

        for (float d = 0.0F; d <= curLen; d += 0.45F) {
            Vec3 center = originPos.add(direction.scale(d));
            sendForcedParticle(level, ParticleTypes.END_ROD, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sendForcedParticle(level, ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);

            Vec3 p1 = center.add(side.scale(0.28D));
            Vec3 p2 = center.add(side.scale(-0.28D));
            Vec3 p3 = center.add(up.scale(0.28D));
            Vec3 p4 = center.add(up.scale(-0.28D));

            sendForcedParticle(level, ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sendForcedParticle(level, ParticleTypes.END_ROD, p2.x, p2.y, p2.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sendForcedParticle(level, ParticleTypes.END_ROD, p3.x, p3.y, p3.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sendForcedParticle(level, ParticleTypes.END_ROD, p4.x, p4.y, p4.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        Vec3 end = getEndPos();
        sendForcedParticle(level, ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 8, 0.15D, 0.15D, 0.15D, 0.01D);
        sendForcedParticle(level, ParticleTypes.END_ROD, end.x, end.y, end.z, 10, 0.12D, 0.12D, 0.12D, 0.0D);
    }

    private static void sendForcedParticle(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                           int count, double offsetX, double offsetY, double offsetZ, double speed) {
        for (ServerPlayer serverPlayer : level.players()) {
            level.sendParticles(serverPlayer, particle, true, x, y, z, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifeTime = tag.getInt("LifeTime");
        maxLength = tag.getFloat("MaxLength");
        damage = tag.getFloat("Damage");
        growthPerTick = tag.contains("GrowthPerTick") ? tag.getFloat("GrowthPerTick") : 1.35F;
        lastDamagedLength = tag.getFloat("LastDamagedLength");

        originPos = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));

        this.entityData.set(CURRENT_LENGTH, tag.getFloat("CurrentLength"));
        this.entityData.set(DIR_X, (float) direction.x);
        this.entityData.set(DIR_Y, (float) direction.y);
        this.entityData.set(DIR_Z, (float) direction.z);
        updateEndPos();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTime", lifeTime);
        tag.putFloat("MaxLength", maxLength);
        tag.putFloat("Damage", damage);
        tag.putFloat("GrowthPerTick", growthPerTick);
        tag.putFloat("CurrentLength", this.entityData.get(CURRENT_LENGTH));
        tag.putFloat("LastDamagedLength", lastDamagedLength);

        tag.putDouble("OriginX", originPos.x);
        tag.putDouble("OriginY", originPos.y);
        tag.putDouble("OriginZ", originPos.z);

        tag.putDouble("DirX", direction.x);
        tag.putDouble("DirY", direction.y);
        tag.putDouble("DirZ", direction.z);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
