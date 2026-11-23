package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.DildoMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class DildoSeatEntity extends Entity {

    private static final EntityDataAccessor<Integer> DIRECTION =
            SynchedEntityData.defineId(
                    DildoSeatEntity.class,
                    EntityDataSerializers.INT
            );

    private BlockPos blockPos;
    private Direction facing;
    private DildoMaterial material;

    private int sittingTime = 0;
    private int morichekLevel = 0;
    private int bedWarsLevel = 0;
    private int nextBedWarsDropTicks = -1;

    @Nullable
    private UUID lastPassengerUuid = null;

    public DildoSeatEntity(
            EntityType<?> type,
            Level level
    ) {
        this(
                level,
                BlockPos.ZERO,
                Direction.UP,
                DildoMaterial.STONE,
                0,
                0
        );
    }

    public DildoSeatEntity(
            Level level,
            BlockPos pos,
            Direction facing,
            DildoMaterial material,
            int morichekLevel,
            int bedWarsLevel
    ) {
        super(
                ModEntities.DILDO_SEAT.get(),
                level
        );

        this.blockPos = pos;
        this.facing = facing;
        this.material = material;
        this.morichekLevel = morichekLevel;
        this.bedWarsLevel = bedWarsLevel;
        this.noPhysics = true;

        Vec3 seatPos = new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );

        this.setPos(
                seatPos.x,
                seatPos.y,
                seatPos.z
        );

        this.entityData.set(
                DIRECTION,
                facing.get3DDataValue()
        );
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(
                DIRECTION,
                Direction.UP.get3DDataValue()
        );
    }

    public boolean hasMorichekBuffActive() {
        return morichekLevel > 0
                && this.isVehicle()
                && sittingTime >= 100;
    }

    public int getBedWarsLevel() {
        return bedWarsLevel;
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (level.getBlockState(blockPos).is(Blocks.AIR)) {
            this.ejectPassengers();
            this.discard();
            return;
        }

        /*
         * Если место свободно, ближайший моб садится автоматически.
         */
        if (!this.isVehicle()) {
            forceNearbyMobToSit();
        }

        UUID passengerUuid =
                getMainPassengerUuid();

        if (passengerUuid == null) {
            sittingTime = 0;
            nextBedWarsDropTicks = -1;
            lastPassengerUuid = null;
            return;
        }

        if (!passengerUuid.equals(lastPassengerUuid)) {
            sittingTime = 0;
            nextBedWarsDropTicks = -1;
            lastPassengerUuid = passengerUuid;
        }

        sittingTime++;

        if (sittingTime >= 200) {
            spawnParticlesAndSound();
        }

        if (sittingTime == 200
                && !this.getPassengers().isEmpty()) {

            Entity passenger =
                    this.getPassengers().get(0);

            if (passenger instanceof ServerPlayer serverPlayer) {
                ZitraksMode.DILDO_SAT_LONG.trigger(
                        serverPlayer
                );
            }
        }

        if (bedWarsLevel > 0
                && sittingTime >= 100) {

            if (nextBedWarsDropTicks < 0) {
                nextBedWarsDropTicks =
                        getNextBedWarsDropTicks();
            }

            nextBedWarsDropTicks--;

            if (nextBedWarsDropTicks <= 0) {
                spawnBedWarsReward();

                nextBedWarsDropTicks =
                        getNextBedWarsDropTicks();
            }
        }
    }

    private void forceNearbyMobToSit() {
        AABB searchBox =
                new AABB(blockPos).inflate(
                        0.9D,
                        0.7D,
                        0.9D
                );

        List<Mob> nearbyMobs =
                level.getEntitiesOfClass(
                        Mob.class,
                        searchBox,
                        mob ->
                                mob.isAlive()
                                        && !mob.isPassenger()
                );

        if (nearbyMobs.isEmpty()) {
            return;
        }

        Mob closestMob =
                nearbyMobs.get(0);

        double closestDistance =
                closestMob.distanceToSqr(this);

        for (Mob mob : nearbyMobs) {
            double distance =
                    mob.distanceToSqr(this);

            if (distance < closestDistance) {
                closestMob = mob;
                closestDistance = distance;
            }
        }

        /*
         * Второй параметр true принудительно сажает моба.
         */
        closestMob.startRiding(
                this,
                true
        );
    }

    @Nullable
    private UUID getMainPassengerUuid() {
        if (this.getPassengers().isEmpty()) {
            return null;
        }

        return this.getPassengers()
                .get(0)
                .getUUID();
    }

    private int getNextBedWarsDropTicks() {
        int baseTicks =
                1200 + level.random.nextInt(401);

        double multiplier =
                getProductionSpeedMultiplier();

        return Math.max(
                1,
                (int) Math.round(
                        baseTicks * multiplier
                )
        );
    }

    private double getProductionSpeedMultiplier() {
        if (this.getPassengers().isEmpty()) {
            return 1.0D;
        }

        Entity passenger =
                this.getPassengers().get(0);

        /*
         * Игроки и агрессивные мобы работают быстро.
         * Creeper является Enemy.
         */
        if (passenger instanceof Player
                || passenger instanceof Enemy) {
            return 1.0D;
        }

        /*
         * Овцы, коровы, свиньи и остальные Animal
         * работают в 1.2 раза медленнее.
         */
        if (passenger instanceof Animal) {
            return 1.2D;
        }

        /*
         * Нейтральные мобы работают в 1.1 раза медленнее.
         */
        if (passenger instanceof Mob) {
            return 1.1D;
        }

        return 1.0D;
    }

    private void spawnBedWarsReward() {
        ItemStack reward =
                new ItemStack(
                        material.getBedWarsDrop()
                );

        if (!reward.isEmpty()) {
            this.spawnAtLocation(reward);
        }
    }

    private void spawnParticlesAndSound() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (sittingTime % 60 == 0) {
            level.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.ENDERMAN_SCREAM,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
        }

        if (sittingTime % 2 == 0) {
            for (int i = 0; i < 5; i++) {
                double offsetX =
                        (level.random.nextDouble() - 0.5D)
                                * 0.3D;

                double offsetZ =
                        (level.random.nextDouble() - 0.5D)
                                * 0.3D;

                serverLevel.sendParticles(
                        ParticleTypes.FALLING_LAVA,
                        getX() + offsetX,
                        getY() + 0.5D,
                        getZ() + offsetZ,
                        1,
                        0.0D,
                        -0.1D,
                        0.0D,
                        0.05D
                );

                serverLevel.sendParticles(
                        ParticleTypes.CRIMSON_SPORE,
                        getX() + offsetX,
                        getY() + 0.3D,
                        getZ() + offsetZ,
                        1,
                        0.0D,
                        -0.05D,
                        0.0D,
                        0.02D
                );
            }
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(
            LivingEntity passenger
    ) {
        Direction[] directions = {
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST
        };

        for (Direction direction : directions) {
            Vec3 exitPos =
                    new Vec3(
                            blockPos.getX()
                                    + 0.5D
                                    + direction.getStepX() * 1.5D,
                            blockPos.getY(),
                            blockPos.getZ()
                                    + 0.5D
                                    + direction.getStepZ() * 1.5D
                    );

            AABB checkBox =
                    new AABB(
                            exitPos.x - 0.3D,
                            exitPos.y,
                            exitPos.z - 0.3D,
                            exitPos.x + 0.3D,
                            exitPos.y + 2.0D,
                            exitPos.z + 0.3D
                    );

            if (level.noCollision(
                    passenger,
                    checkBox
            )) {
                return exitPos;
            }
        }

        return new Vec3(
                blockPos.getX() + 0.5D,
                blockPos.getY() + 2.0D,
                blockPos.getZ() + 0.5D
        );
    }

    @Override
    protected boolean canRide(Entity entity) {
        return true;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        sittingTime =
                tag.getInt("SittingTime");

        blockPos =
                new BlockPos(
                        tag.getInt("BlockX"),
                        tag.getInt("BlockY"),
                        tag.getInt("BlockZ")
                );

        facing =
                Direction.from3DDataValue(
                        tag.getInt("Facing")
                );

        try {
            material =
                    DildoMaterial.valueOf(
                            tag.getString("Material")
                    );
        } catch (Exception ignored) {
            material =
                    DildoMaterial.STONE;
        }

        morichekLevel =
                tag.getInt("MorichekLevel");

        bedWarsLevel =
                tag.getInt("BedWarsLevel");

        nextBedWarsDropTicks =
                tag.getInt("NextBedWarsDropTicks");

        if (tag.hasUUID("LastPassengerUuid")) {
            lastPassengerUuid =
                    tag.getUUID("LastPassengerUuid");
        } else {
            lastPassengerUuid = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putInt(
                "SittingTime",
                sittingTime
        );

        tag.putInt(
                "BlockX",
                blockPos.getX()
        );

        tag.putInt(
                "BlockY",
                blockPos.getY()
        );

        tag.putInt(
                "BlockZ",
                blockPos.getZ()
        );

        tag.putInt(
                "Facing",
                facing.get3DDataValue()
        );

        tag.putString(
                "Material",
                material.name()
        );

        tag.putInt(
                "MorichekLevel",
                morichekLevel
        );

        tag.putInt(
                "BedWarsLevel",
                bedWarsLevel
        );

        tag.putInt(
                "NextBedWarsDropTicks",
                nextBedWarsDropTicks
        );

        if (lastPassengerUuid != null) {
            tag.putUUID(
                    "LastPassengerUuid",
                    lastPassengerUuid
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return (Packet<ClientGamePacketListener>)
                NetworkHooks.getEntitySpawningPacket(this);
    }
}