package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The rabbit sequence begins as soon as both carrot-fed rabbits are in love.
 * It does not wait for BabyEntitySpawnEvent, so the train pose is visible
 * immediately rather than after vanilla's breeding delay.
 */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RabbitRecoilHandler {
    private static final int TRAIN_AND_SHAKE_TICKS = 20 * 3;
    private static final double TRAIN_CLOSE_DISTANCE = 0.45D;
    private static final double RECOIL_DISTANCE = 2.0D;
    private static final double SHAKE_AMPLITUDE = 0.06D;
    private static final int MIN_STUN_TICKS = 20 * 5;
    private static final int MAX_STUN_TICKS = 20 * 10;
    private static final float UPSIDE_DOWN_PITCH = 180.0F;

    private static final Map<UUID, TrainPose> TRAIN_POSES = new HashMap<>();
    private static final Map<UUID, RecoilData> RECOILING_RABBITS = new HashMap<>();

    private RabbitRecoilHandler() {
    }

    /**
     * On the tick after the second carrot has made both rabbits in-love, starts
     * the 3-second A-front/B-back pose immediately.
     */
    @SubscribeEvent
    public static void startTrainPoseWhenFed(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Rabbit rabbit) || rabbit.level.isClientSide) return;
        if (!rabbit.isInLove()) return;
        if (TRAIN_POSES.containsKey(rabbit.getUUID())) return;
        if (RECOILING_RABBITS.containsKey(rabbit.getUUID())) return;

        Rabbit partner = findInLovePartner(rabbit);
        if (partner == null) return;
        if (TRAIN_POSES.containsKey(partner.getUUID())) return;
        if (RECOILING_RABBITS.containsKey(partner.getUUID())) return;

        // Only one of the two LivingTick events creates the shared sequence.
        if (rabbit.getUUID().compareTo(partner.getUUID()) > 0) return;
        startTrainAndRecoil(rabbit, partner);
    }

    @SubscribeEvent
    public static void tickRabbitScene(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Rabbit rabbit) || rabbit.level.isClientSide) return;

        long now = rabbit.level.getGameTime();
        tickTrainPose(rabbit, now);
        tickRecoilAndStun(rabbit, now);
    }

    private static Rabbit findInLovePartner(Rabbit rabbit) {
        Rabbit closest = null;
        double closestDistance = 3.0D * 3.0D;

        for (Rabbit candidate : rabbit.level.getEntitiesOfClass(
                Rabbit.class,
                rabbit.getBoundingBox().inflate(3.0D),
                Rabbit::isInLove
        )) {
            if (candidate == rabbit) continue;
            double distance = rabbit.distanceToSqr(candidate);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * A is placed in front. B is directly behind. Both face the same direction,
     * meaning A has its back to B: the requested "паровозик" pose.
     */
    private static void startTrainAndRecoil(Rabbit rabbitA, Rabbit rabbitB) {
        Vec3 forward = rabbitA.position().subtract(rabbitB.position());
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 0.001D) {
            double angle = rabbitA.getRandom().nextDouble() * Math.PI * 2.0D;
            forward = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        } else {
            forward = forward.normalize();
        }

        rabbitA.teleportTo(
                rabbitB.getX() + forward.x * TRAIN_CLOSE_DISTANCE,
                rabbitB.getY(),
                rabbitB.getZ() + forward.z * TRAIN_CLOSE_DISTANCE
        );
        float trainYaw = yawForDirection(forward);
        setRabbitYaw(rabbitA, trainYaw);
        setRabbitYaw(rabbitB, trainYaw);

        long now = rabbitA.level.getGameTime();
        long recoilStartsAt = now + TRAIN_AND_SHAKE_TICKS;

        // No setNoAi here. Vanilla mating AI continues, so the baby still appears.
        TRAIN_POSES.put(rabbitA.getUUID(), new TrainPose(
                recoilStartsAt,
                rabbitA.getX(), rabbitA.getY(), rabbitA.getZ(),
                trainYaw,
                false,
                forward.x, forward.z
        ));
        TRAIN_POSES.put(rabbitB.getUUID(), new TrainPose(
                recoilStartsAt,
                rabbitB.getX(), rabbitB.getY(), rabbitB.getZ(),
                trainYaw,
                true,
                forward.x, forward.z
        ));

        int stunTicks = MIN_STUN_TICKS
                + rabbitB.getRandom().nextInt(MAX_STUN_TICKS - MIN_STUN_TICKS + 1);
        RECOILING_RABBITS.put(rabbitB.getUUID(), new RecoilData(
                rabbitA.getUUID(),
                recoilStartsAt,
                stunTicks,
                rabbitB.isNoAi(),
                rabbitB.getXRot(),
                -forward.x,
                -forward.z,
                rabbitB.getX(),
                rabbitB.getY(),
                rabbitB.getZ()
        ));
    }

    private static void tickTrainPose(Rabbit rabbit, long now) {
        TrainPose pose = TRAIN_POSES.get(rabbit.getUUID());
        if (pose == null) return;

        if (!rabbit.isAlive() || now >= pose.endsAt()) {
            TRAIN_POSES.remove(rabbit.getUUID());
            return;
        }

        // Keep the visual train pose, but do not disable AI: vanilla breeding
        // can still complete while the pair is held close together.
        double shake = 0.0D;
        if (pose.isRearRabbit()) {
            long localTick = now - (pose.endsAt() - TRAIN_AND_SHAKE_TICKS);
            double sign = (localTick & 1L) == 0L ? 1.0D : -1.0D;
            shake = SHAKE_AMPLITUDE * sign;
        }

        rabbit.teleportTo(
                pose.x() + pose.forwardX() * shake,
                pose.y(),
                pose.z() + pose.forwardZ() * shake
        );
        rabbit.setDeltaMovement(Vec3.ZERO);
        setRabbitYaw(rabbit, pose.yaw());
    }

    private static void tickRecoilAndStun(Rabbit rabbitB, long now) {
        RecoilData recoil = RECOILING_RABBITS.get(rabbitB.getUUID());
        if (recoil == null) return;
        if (!rabbitB.isAlive()) {
            RECOILING_RABBITS.remove(rabbitB.getUUID());
            return;
        }

        if (!recoil.recoilStarted) {
            if (now < recoil.recoilStartsAt) return;

            recoil.recoilStarted = true;
            TRAIN_POSES.remove(rabbitB.getUUID());
            TRAIN_POSES.remove(recoil.frontRabbitId);

            // Exactly after 3 seconds of the train + tick-by-tick shake: B flies
            // precisely two blocks horizontally from its original rear position.
            rabbitB.teleportTo(
                    recoil.startX + recoil.directionX * RECOIL_DISTANCE,
                    recoil.startY,
                    recoil.startZ + recoil.directionZ * RECOIL_DISTANCE
            );
            rabbitB.setNoAi(true);
            rabbitB.getNavigation().stop();
            rabbitB.setDeltaMovement(Vec3.ZERO);
            rabbitB.setXRot(UPSIDE_DOWN_PITCH);
            rabbitB.hurtMarked = true;
            recoil.stunEndsAt = now + recoil.stunTicks;
            return;
        }

        if (now >= recoil.stunEndsAt) {
            rabbitB.setNoAi(recoil.hadNoAiBefore);
            rabbitB.setXRot(recoil.originalXRot);
            rabbitB.setDeltaMovement(Vec3.ZERO);
            RECOILING_RABBITS.remove(rabbitB.getUUID());
            return;
        }

        rabbitB.setNoAi(true);
        rabbitB.getNavigation().stop();
        rabbitB.setDeltaMovement(Vec3.ZERO);
        rabbitB.setXRot(UPSIDE_DOWN_PITCH);
        rabbitB.hurtMarked = true;
    }

    /** Used by the client renderer; vanilla rotation packets synchronize XRot. */
    public static boolean isUpsideDown(Rabbit rabbit) {
        return Math.abs(Mth.wrapDegrees(rabbit.getXRot() - UPSIDE_DOWN_PITCH)) < 1.0F;
    }

    private static float yawForDirection(Vec3 direction) {
        return (float) (Mth.atan2(direction.z, direction.x) * (180.0D / Math.PI)) - 90.0F;
    }

    private static void setRabbitYaw(Rabbit rabbit, float yaw) {
        rabbit.setYRot(yaw);
        rabbit.setYHeadRot(yaw);
        rabbit.setYBodyRot(yaw);
    }

    private record TrainPose(long endsAt, double x, double y, double z,
                             float yaw, boolean isRearRabbit,
                             double forwardX, double forwardZ) {
    }

    private static final class RecoilData {
        private final UUID frontRabbitId;
        private final long recoilStartsAt;
        private final int stunTicks;
        private final boolean hadNoAiBefore;
        private final float originalXRot;
        private final double directionX;
        private final double directionZ;
        private final double startX;
        private final double startY;
        private final double startZ;

        private boolean recoilStarted;
        private long stunEndsAt;

        private RecoilData(UUID frontRabbitId, long recoilStartsAt, int stunTicks,
                           boolean hadNoAiBefore, float originalXRot,
                           double directionX, double directionZ,
                           double startX, double startY, double startZ) {
            this.frontRabbitId = frontRabbitId;
            this.recoilStartsAt = recoilStartsAt;
            this.stunTicks = stunTicks;
            this.hadNoAiBefore = hadNoAiBefore;
            this.originalXRot = originalXRot;
            this.directionX = directionX;
            this.directionZ = directionZ;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
        }
    }
}
