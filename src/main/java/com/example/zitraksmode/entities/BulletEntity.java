package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class BulletEntity extends AbstractArrow implements IEntityAdditionalSpawnData {

    private float bulletDamage = 20.0f;
    private int remainingBlockPierce = 0;

    private BlockPos ignoredBlockPos = null;
    private int ignoreBlockTicks = 0;

    private Vec3 exactSpawnMotionClient = null;
    private float exactSpawnYRotClient = 0.0F;
    private float exactSpawnXRotClient = 0.0F;
    private boolean exactSpawnAppliedClient = false;

    public BulletEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.pickup = Pickup.DISALLOWED;
    }

    public BulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.BULLET.get(), level);
        this.setOwner(shooter);
        this.setNoGravity(true);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return (Packet<ClientGamePacketListener>) NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        Vec3 motion = this.getDeltaMovement();
        buf.writeDouble(motion.x);
        buf.writeDouble(motion.y);
        buf.writeDouble(motion.z);
        buf.writeFloat(this.getYRot());
        buf.writeFloat(this.getXRot());
        buf.writeFloat(this.bulletDamage);
        buf.writeInt(this.remainingBlockPierce);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        double mx = buf.readDouble();
        double my = buf.readDouble();
        double mz = buf.readDouble();
        float yRot = buf.readFloat();
        float xRot = buf.readFloat();
        this.bulletDamage = buf.readFloat();
        this.remainingBlockPierce = buf.readInt();

        this.exactSpawnMotionClient = new Vec3(mx, my, mz);
        this.exactSpawnYRotClient = yRot;
        this.exactSpawnXRotClient = xRot;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        applyExactClientSpawnState();
    }

    private void applyExactClientSpawnState() {
        if (!this.level.isClientSide) return;
        if (this.exactSpawnAppliedClient) return;
        if (this.exactSpawnMotionClient == null) return;

        this.setDeltaMovement(this.exactSpawnMotionClient);
        this.setYRot(this.exactSpawnYRotClient);
        this.setXRot(this.exactSpawnXRotClient);
        this.yRotO = this.exactSpawnYRotClient;
        this.xRotO = this.exactSpawnXRotClient;
        this.hasImpulse = true;

        this.exactSpawnAppliedClient = true;
        this.exactSpawnMotionClient = null;
    }

    @Override
    public void tick() {
        if (this.level.isClientSide) {
            applyExactClientSpawnState();
        }

        if (ignoreBlockTicks > 0) {
            ignoreBlockTicks--;
            if (ignoreBlockTicks <= 0) {
                ignoredBlockPos = null;
            }
        }

        super.tick();
        this.setNoGravity(true);

        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockPos pos = result.getBlockPos();

        if (ignoredBlockPos != null && ignoreBlockTicks > 0 && ignoredBlockPos.equals(pos)) {
            return;
        }

        BlockState state = this.level.getBlockState(pos);

        if (this.remainingBlockPierce > 0 && canPierceThrough(state, pos)) {
            this.remainingBlockPierce--;

            Vec3 motion = this.getDeltaMovement();
            Vec3 dir = motion.lengthSqr() > 1.0E-6
                    ? motion.normalize()
                    : Vec3.atLowerCornerOf(result.getDirection().getNormal()).normalize();

            // Игнорим этот блок на несколько тиков
            this.ignoredBlockPos = pos.immutable();
            this.ignoreBlockTicks = 4;

            // Жёстко выносим пулю ЗА блок, чтобы не поймать ту же грань
            Vec3 newPos = result.getLocation().add(dir.scale(1.6D));

            this.setPos(newPos.x, newPos.y, newPos.z);
            this.xo = newPos.x;
            this.yo = newPos.y;
            this.zo = newPos.z;

            this.setDeltaMovement(motion);
            this.hasImpulse = true;
            this.hurtMarked = true;
            return;
        }

        super.onHitBlock(result);
        this.discard();
    }

    private boolean canPierceThrough(BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.REINFORCED_DEEPSLATE)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)) {
            return false;
        }

        float destroySpeed = state.getDestroySpeed(this.level, pos);
        if (destroySpeed < 0.0f) {
            return false;
        }

        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        DamageSource source;
        if (owner == null) {
            source = DamageSource.arrow(this, this);
        } else {
            source = DamageSource.arrow(this, owner);
            if (owner instanceof LivingEntity livingOwner) {
                livingOwner.setLastHurtMob(target);
            }
        }

        int prevFire = target.getRemainingFireTicks();
        if (this.isOnFire()) {
            target.setSecondsOnFire(5);
        }

        if (target.hurt(source, this.bulletDamage)) {
            if (target instanceof LivingEntity living) {
                if (!this.level.isClientSide && owner instanceof LivingEntity livingOwner) {
                    living.setLastHurtByMob(livingOwner);
                }
                this.doPostHurtEffects(living);
            }

            this.playSound(
                    this.getDefaultHitGroundSoundEvent(),
                    1.0F,
                    1.2F / (this.random.nextFloat() * 0.2F + 0.9F)
            );
        } else {
            target.setRemainingFireTicks(prevFire);
        }

        this.discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void playerTouch(net.minecraft.world.entity.player.Player player) {
    }

    public void setBulletDamage(float damage) {
        this.bulletDamage = damage;
        this.setBaseDamage(damage);
    }

    public float getBulletDamage() {
        return this.bulletDamage;
    }

    public void setRemainingBlockPierce(int remainingBlockPierce) {
        this.remainingBlockPierce = Math.max(0, remainingBlockPierce);
    }

    public int getRemainingBlockPierce() {
        return remainingBlockPierce;
    }
}