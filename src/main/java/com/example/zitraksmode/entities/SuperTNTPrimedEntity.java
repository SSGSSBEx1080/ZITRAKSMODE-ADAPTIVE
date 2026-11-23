package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class SuperTNTPrimedEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_FUSE_ID =
            SynchedEntityData.defineId(SuperTNTPrimedEntity.class, EntityDataSerializers.INT);

    private static final int DEFAULT_FUSE_TIME = 80;
    private static final float EXPLOSION_POWER = 20.0F;

    private static final EntityDimensions SMALL_TNT_SIZE = EntityDimensions.fixed(0.98F, 0.98F);

    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public SuperTNTPrimedEntity(EntityType<? extends SuperTNTPrimedEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
        this.refreshDimensions();
    }

    /**
     * Универсальный спавн — ТОЧНО как ванильный PrimedTnt.
     * Лёгкий горизонтальный разлёт + крошечный прыжок 0.20 вверх. И всё.
     * Никаких "защит от залипания" — ванильные TNT и так друг друга проходят.
     */
    public SuperTNTPrimedEntity(Level level, double x, double y, double z, @Nullable LivingEntity igniter) {
        this(ModEntities.SUPER_TNT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;

        // Ванильный PrimedTnt.java делает РОВНО ЭТО и больше ничего:
        double d0 = level.random.nextDouble() * ((float) Math.PI * 2F);
        this.setDeltaMovement(-Math.sin(d0) * 0.02D, 0.20D, -Math.cos(d0) * 0.02D);

        this.setFuse(DEFAULT_FUSE_TIME);
        this.owner = igniter;
        this.ownerUUID = igniter != null ? igniter.getUUID() : null;
    }

    /**
     * Спавн из взрыва соседнего TNT. Ванильное поведение:
     * обычная физика (никакого noclip!), просто entity с укороченным fuse.
     * Импульс уже задан ванильным конструктором (0.20 вверх + горизонтальный разлёт).
     */
    public static SuperTNTPrimedEntity createFromExplosion(Level level, double x, double y, double z,
                                                           @Nullable LivingEntity igniter) {
        // Никаких изменений — то же что обычный спавн. Ваниль делает ровно так.
        return new SuperTNTPrimedEntity(level, x, y, z, igniter);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return SMALL_TNT_SIZE;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FUSE_ID, DEFAULT_FUSE_TIME);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        // ТОЧНО ванильный PrimedTnt.tick() — байт в байт:
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));

        if (this.onGround) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.70D, -0.50D, 0.70D));
        }

        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);

        if (fuse <= 0) {
            this.discard();
            if (!this.level.isClientSide) {
                this.explode();
            }
        }
    }

    private void explode() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity source = getOwner();

        level.explode(
                source != null ? source : this,
                this.getX(),
                this.getY(0.0625D),
                this.getZ(),
                EXPLOSION_POWER,
                Explosion.BlockInteraction.BREAK
        );

        dryFluids(serverLevel);
        scheduleLightningStrikes(serverLevel);
    }

    private void dryFluids(ServerLevel level) {
        BlockPos center = this.blockPosition();
        int radius = (int) Math.ceil(EXPLOSION_POWER);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double distSq = x * x + y * y + z * z;
                    if (distSq > radius * radius) continue;

                    BlockState state = level.getBlockState(pos);

                    if (!state.getFluidState().isEmpty()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                            && Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void scheduleLightningStrikes(ServerLevel level) {
        BlockPos center = this.blockPosition();
        int radius = 15;
        final LivingEntity savedOwner = getOwner();

        for (int i = 0; i < 10; i++) {
            int delay = i * 3;

            level.getServer().tell(new TickTask(
                    level.getServer().getTickCount() + delay,
                    () -> {
                        List<LivingEntity> targets = level.getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(
                                        center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                                        center.getX() + radius, center.getY() + radius, center.getZ() + radius
                                ),
                                entity -> entity.isAlive() && entity != savedOwner
                        );

                        BlockPos strikePos;

                        if (!targets.isEmpty()) {
                            LivingEntity target = targets.get(level.random.nextInt(targets.size()));
                            strikePos = target.blockPosition();
                        } else {
                            double offsetX = (level.random.nextDouble() - 0.5D) * 2.0D * radius;
                            double offsetZ = (level.random.nextDouble() - 0.5D) * 2.0D * radius;
                            strikePos = center.offset((int) offsetX, 0, (int) offsetZ);
                        }

                        strikePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, strikePos);

                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                        if (lightning != null) {
                            lightning.moveTo(Vec3.atBottomCenterOf(strikePos));
                            lightning.setVisualOnly(false);
                            level.addFreshEntity(lightning);
                        }
                    }
            ));
        }
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE_ID, fuse);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner != null && this.owner.isAlive()) {
            return this.owner;
        }
        if (this.ownerUUID != null && this.level instanceof ServerLevel sl) {
            Entity e = sl.getEntity(this.ownerUUID);
            if (e instanceof LivingEntity living) {
                this.owner = living;
                return living;
            }
        }
        return null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Fuse", (short) this.getFuse());
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setFuse(tag.getShort("Fuse"));
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return (Packet<ClientGamePacketListener>) NetworkHooks.getEntitySpawningPacket(this);
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
