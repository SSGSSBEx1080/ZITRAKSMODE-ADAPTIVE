package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class SteelBallEntity extends ThrowableItemProjectile implements IEntityAdditionalSpawnData {

    private static final int MAX_LIFE_TICKS = 200;
    private static final int OWNER_HIT_DELAY_TICKS = 10;

    private static final double GRAVITY = 0.075D;
    private static final double AIR_DRAG = 0.992D;
    private static final double MAGNUS_FORCE = 0.0035D;

    private static final double GROUND_FRICTION_WEAK = 0.84D;
    private static final double GROUND_FRICTION_CHARGED = 0.92D;

    private static final double WALL_BOUNCE_WEAK = 0.60D;
    private static final double WALL_BOUNCE_CHARGED = 0.74D;

    private static final double FLOOR_BOUNCE_WEAK = 0.38D;
    private static final double FLOOR_BOUNCE_CHARGED = 0.52D;

    private static final double SLIDE_AFTER_FLOOR_BOUNCE = 0.96D;
    private static final double MIN_SPEED_TO_BOUNCE = 0.10D;
    private static final double STOP_SPEED = 0.045D;

    private static final double OWNER_PICKUP_DISTANCE = 1.5D;
    private static final int DRILL_DURATION_TICKS = 30;
    private static final int DRILL_DURATION_TICKS_BREAKER = 60;

    private boolean isCharged;
    private boolean ballBreaker;
    private int lifeTicks = 0;
    public boolean isReturning = false;
    private int drillTicks = 0;
    private LivingEntity stuckEntity = null;

    private int hitOwnerDelay = OWNER_HIT_DELAY_TICKS;
    private boolean retrieved = false;
    public float spinAngle = 0f;

    private int groundTicks = 0;
    private int spinDir = 1;

    private ItemStack storedStack = ItemStack.EMPTY;

    public SteelBallEntity(EntityType<? extends SteelBallEntity> type, Level level) {
        super(type, level);
        this.spinDir = level.random.nextBoolean() ? 1 : -1;
    }

    public SteelBallEntity(Level level, Player player, boolean isCharged) {
        super(ModEntities.STEEL_BALL.get(), player, level);
        this.isCharged = isCharged;
        this.spinDir = level.random.nextBoolean() ? 1 : -1;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.STEEL_BALL.get();
    }

    @Override
    protected float getGravity() {
        return (float) GRAVITY;
    }

    public void setBallBreaker(boolean ballBreaker) {
        this.ballBreaker = ballBreaker;
        if (ballBreaker) {
            this.isCharged = true;
        }
    }

    public boolean isBallBreaker() {
        return ballBreaker;
    }

    public void setStoredStack(ItemStack stack) {
        this.storedStack = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!this.storedStack.isEmpty()) {
            this.storedStack.setCount(1);
        }
    }

    private ItemStack createReturnedStack() {
        if (!storedStack.isEmpty()) {
            ItemStack copy = storedStack.copy();
            copy.setCount(1);
            return copy;
        }
        return new ItemStack(getDefaultItem());
    }

    @Override
    public void tick() {
        super.tick();

        if (retrieved) {
            discard();
            return;
        }

        lifeTicks++;
        if (hitOwnerDelay > 0) {
            hitOwnerDelay--;
        }

        updateSpinVisual();

        if (!this.level.isClientSide && lifeTicks >= MAX_LIFE_TICKS) {
            if (ballBreaker) {
                isReturning = true;
            } else {
                breakAndDiscard();
                return;
            }
        }

        if (stuckEntity != null) {
            tickDrillState();
            return;
        }

        if (isReturning && getOwner() != null) {
            returnToOwner();
            return;
        }

        if (!this.onGround && !isReturning) {
            applyAirPhysics();
            groundTicks = 0;
        } else if (this.onGround && !isReturning) {
            applyGroundPhysics();
        } else {
            groundTicks = 0;
        }

        if (!retrieved
                && !isReturning
                && hitOwnerDelay <= 0
                && getOwner() != null
                && this.distanceToSqr(getOwner()) <= OWNER_PICKUP_DISTANCE * OWNER_PICKUP_DISTANCE) {
            pickupByOwner();
        }
    }

    private void updateSpinVisual() {
        double speed = this.getDeltaMovement().length();
        float spinSpeed = (float) (speed * 95.0D);

        if (isReturning) {
            spinSpeed += 18.0F;
        }
        if (stuckEntity != null) {
            spinSpeed += 24.0F;
        }
        if (spinSpeed < 2.0F) {
            spinSpeed = 2.0F;
        }

        spinAngle += spinSpeed * spinDir;
        if (spinAngle >= 360.0F) spinAngle -= 360.0F;
        if (spinAngle < 0.0F) spinAngle += 360.0F;
    }

    private void applyAirPhysics() {
        Vec3 motion = this.getDeltaMovement();

        if (motion.lengthSqr() > 1.0E-5D) {
            motion = motion.scale(AIR_DRAG);

            if (lifeTicks > 2 && motion.lengthSqr() > 0.04D) {
                Vec3 side = motion.cross(new Vec3(0.0D, 1.0D, 0.0D));
                if (side.lengthSqr() > 1.0E-5D) {
                    double magnus = ballBreaker ? MAGNUS_FORCE * 1.8D : MAGNUS_FORCE;
                    side = side.normalize().scale(magnus * spinDir);
                    motion = motion.add(side);
                }
            }

            this.setDeltaMovement(motion);
        }
    }

    private void applyGroundPhysics() {
        groundTicks++;

        Vec3 motion = this.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        double horizontalSpeed = horizontal.length();

        if (horizontalSpeed > STOP_SPEED) {
            double friction = isCharged ? GROUND_FRICTION_CHARGED : GROUND_FRICTION_WEAK;
            if (ballBreaker) {
                friction = Math.min(0.97D, friction + 0.03D);
            }
            horizontal = horizontal.scale(friction);
        } else {
            horizontal = Vec3.ZERO;
        }

        this.setDeltaMovement(horizontal.x, 0.0D, horizontal.z);

        if (isCharged) {
            if (horizontal.lengthSqr() > 0.04D) {
                if (!this.level.isClientSide && lifeTicks % 6 == 0) {
                    float damage = (float) Math.min(
                            ballBreaker ? 9.0D : 5.0D,
                            (ballBreaker ? 2.8D : 1.5D) + horizontal.length() * (ballBreaker ? 3.5D : 2.5D)
                    );
                    this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.55D)).forEach(entity -> {
                        if (entity != getOwner()) {
                            entity.hurt(DamageSource.thrown(this, this.getOwner()), damage);
                        }
                    });
                }

                if (!this.level.isClientSide && lifeTicks % 10 == 0) {
                    this.level.playSound(
                            null,
                            this.getX(), this.getY(), this.getZ(),
                            ModSounds.STEEL_BALL_SPIN.get(),
                            SoundSource.NEUTRAL,
                            0.45F,
                            0.9F + this.level.random.nextFloat() * 0.25F
                    );
                }
            }

            if (groundTicks >= (ballBreaker ? 20 : 30) && horizontal.lengthSqr() < 0.02D) {
                isReturning = true;
            }
        }
    }

    private void tickDrillState() {
        if (stuckEntity == null) {
            return;
        }

        if (!stuckEntity.isAlive()) {
            stuckEntity = null;
            drillTicks = 0;
            isReturning = true;
            return;
        }

        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(
                stuckEntity.getX(),
                stuckEntity.getY() + stuckEntity.getBbHeight() * 0.5D,
                stuckEntity.getZ()
        );

        if (drillTicks > 0) {
            if (drillTicks % 4 == 0) {
                float drillDamage = ballBreaker ? 4.5F : 2.5F;
                stuckEntity.hurt(DamageSource.thrown(this, this.getOwner()), drillDamage);

                if (!this.level.isClientSide) {
                    this.level.playSound(
                            null,
                            this.getX(), this.getY(), this.getZ(),
                            ModSounds.STEEL_BALL_SPIN.get(),
                            SoundSource.NEUTRAL,
                            0.9F,
                            ballBreaker ? 1.5F : 1.35F
                    );
                }
            }

            drillTicks--;
        }

        if (drillTicks <= 0) {
            stuckEntity = null;
            isReturning = true;
        }
    }

    private void returnToOwner() {
        if (retrieved || getOwner() == null) {
            if (retrieved) {
                discard();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            return;
        }

        Vec3 currentPos = this.position();
        Vec3 targetPos = owner.position().add(0.0D, owner.getEyeHeight() * 0.8D, 0.0D);
        Vec3 diff = targetPos.subtract(currentPos);
        double distance = diff.length();

        if (distance < OWNER_PICKUP_DISTANCE) {
            pickupByOwner();
            return;
        }

        ClipContext ctx = new ClipContext(currentPos, targetPos, ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this);
        BlockHitResult hit = this.level.clip(ctx);
        this.noPhysics = hit.getType() == HitResult.Type.BLOCK || ballBreaker;

        Vec3 desiredVelocity = diff.normalize().scale(
                distance > 6.0D
                        ? (ballBreaker ? 1.55D : 1.25D)
                        : (ballBreaker ? 1.05D : 0.85D) + distance * 0.08D
        );

        Vec3 smoothed = this.getDeltaMovement()
                .scale(ballBreaker ? 0.62D : 0.72D)
                .add(desiredVelocity.scale(ballBreaker ? 0.38D : 0.28D));

        this.setDeltaMovement(smoothed);
    }

    private void pickupByOwner() {
        if (retrieved) return;

        retrieved = true;

        if (getOwner() instanceof Player player && !this.level.isClientSide) {
            ItemStack stack = createReturnedStack();

            boolean placedInHand = false;

            if (ballBreaker) {
                if (player.getMainHandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    placedInHand = true;
                } else if (player.getOffhandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, stack);
                    placedInHand = true;
                }
            }

            if (!placedInHand) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }

            this.level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEEL_BALL_RETURN.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            if (isCharged && player instanceof ServerPlayer sp) {
                ZitraksMode.STEEL_BALL_GOLDEN_ROTATION.trigger(sp);
            }
        }

        discard();
    }

    private void breakAndDiscard() {
        if (retrieved) {
            discard();
            return;
        }

        if (ballBreaker) {
            isReturning = true;
            return;
        }

        if (!this.level.isClientSide) {
            BlockPos pos = this.blockPosition();
            this.level.levelEvent(2001, pos, Block.getId(Blocks.IRON_BLOCK.defaultBlockState()));
        }

        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();

        if (target == getOwner()) {
            if (!retrieved && hitOwnerDelay <= 0) {
                pickupByOwner();
            }
            return;
        }

        if (isReturning) {
            return;
        }

        float impactDamage = (float) Math.max(
                isCharged ? 8.0D : 6.0D,
                this.getDeltaMovement().length() * (isCharged ? 7.0D : 4.5D)
        );

        if (ballBreaker) {
            impactDamage *= 1.6F;
        }

        if (isCharged && target instanceof LivingEntity living) {
            living.hurt(DamageSource.thrown(this, this.getOwner()), impactDamage);
            stuckEntity = living;
            drillTicks = ballBreaker ? DRILL_DURATION_TICKS_BREAKER : DRILL_DURATION_TICKS;
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            target.hurt(DamageSource.thrown(this, this.getOwner()), impactDamage);
            isReturning = true;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (retrieved || isReturning) return;

        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();

        if (speed < MIN_SPEED_TO_BOUNCE) {
            if (ballBreaker) {
                isReturning = true;
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        Vec3 hitLoc = result.getLocation();
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();

        double dot = motion.dot(normal);
        Vec3 reflected = motion.subtract(normal.scale(2.0D * dot));

        boolean verticalSurface = result.getDirection().getAxis().isVertical();
        double bounceFactor = verticalSurface
                ? (isCharged ? FLOOR_BOUNCE_CHARGED : FLOOR_BOUNCE_WEAK)
                : (isCharged ? WALL_BOUNCE_CHARGED : WALL_BOUNCE_WEAK);

        if (ballBreaker) {
            bounceFactor += 0.10D;
        }

        Vec3 newMotion = reflected.scale(bounceFactor);

        if (verticalSurface) {
            newMotion = new Vec3(
                    newMotion.x * SLIDE_AFTER_FLOOR_BOUNCE,
                    Math.abs(newMotion.y),
                    newMotion.z * SLIDE_AFTER_FLOOR_BOUNCE
            );
        } else {
            newMotion = new Vec3(
                    newMotion.x,
                    newMotion.y * 0.98D,
                    newMotion.z
            );
        }

        if (newMotion.length() < MIN_SPEED_TO_BOUNCE) {
            if (ballBreaker) {
                isReturning = true;
                return;
            }
            newMotion = Vec3.ZERO;
        }

        this.setPos(hitLoc.add(normal.scale(0.06D)));
        this.setDeltaMovement(newMotion);

        spinDir *= -1;

        if (!this.level.isClientSide) {
            this.level.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    ModSounds.STEEL_BALL_SPIN.get(),
                    SoundSource.NEUTRAL,
                    0.8F,
                    0.85F + this.level.random.nextFloat() * 0.25F
            );
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Charged", this.isCharged);
        tag.putBoolean("BallBreaker", this.ballBreaker);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putBoolean("Returning", this.isReturning);
        tag.putInt("DrillTicks", this.drillTicks);
        tag.putInt("HitOwnerDelay", this.hitOwnerDelay);
        tag.putBoolean("Retrieved", this.retrieved);
        tag.putFloat("SpinAngle", this.spinAngle);
        tag.putInt("GroundTicks", this.groundTicks);
        tag.putInt("SpinDir", this.spinDir);

        if (!this.storedStack.isEmpty()) {
            tag.put("StoredStack", this.storedStack.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.isCharged = tag.getBoolean("Charged");
        this.ballBreaker = tag.getBoolean("BallBreaker");
        this.lifeTicks = tag.getInt("LifeTicks");
        this.isReturning = tag.getBoolean("Returning");
        this.drillTicks = tag.getInt("DrillTicks");
        this.hitOwnerDelay = tag.getInt("HitOwnerDelay");
        this.retrieved = tag.getBoolean("Retrieved");
        this.spinAngle = tag.getFloat("SpinAngle");
        this.groundTicks = tag.getInt("GroundTicks");
        this.spinDir = tag.getInt("SpinDir");

        if (tag.contains("StoredStack")) {
            this.storedStack = ItemStack.of(tag.getCompound("StoredStack"));
        } else {
            this.storedStack = ItemStack.EMPTY;
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.isCharged);
        buffer.writeBoolean(this.ballBreaker);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.isCharged = additionalData.readBoolean();
        this.ballBreaker = additionalData.readBoolean();
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean isCharged() {
        return isCharged;
    }
}