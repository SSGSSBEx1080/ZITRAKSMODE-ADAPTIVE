package com.example.zitraksmode.weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class SniperUltimateAbility {

    private static final double RANGE = 200.0D;
    private static final double BLOCK_STEP = 0.30D;
    private static final double PARTICLE_STEP = 0.24D;
    private static final float DAMAGE_MULTIPLIER = 4.0F;
    private static final int FIRE_SECONDS = 8;
    private static final double HITBOX_INFLATE = 0.45D;
    private static final double KNOCKBACK_STRENGTH = 1.2D;
    private static final double BEAM_RADIUS = 0.20D;

    private SniperUltimateAbility() {
    }

    public static void fire(Player player, float baseDamage) {
        if (!(player.level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(RANGE));

        destroyBlocks(serverLevel, start, look);
        damageEntities(serverLevel, player, start, end, look, baseDamage * DAMAGE_MULTIPLIER);
        spawnBeamParticles(serverLevel, start, look, end);
    }

    private static void destroyBlocks(ServerLevel level, Vec3 start, Vec3 look) {
        Set<BlockPos> visited = new HashSet<>();

        for (double distance = 0.0D; distance <= RANGE; distance += BLOCK_STEP) {
            Vec3 point = start.add(look.scale(distance));
            BlockPos pos = new BlockPos(point.x, point.y, point.z);

            if (!visited.add(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            if (state.is(Blocks.BEDROCK)) {
                continue;
            }

            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void damageEntities(ServerLevel level, Player player, Vec3 start, Vec3 end, Vec3 look, float damage) {
        AABB area = new AABB(start, end).inflate(1.5D);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player && entity.isAlive())) {
            if (target.getBoundingBox().inflate(HITBOX_INFLATE).clip(start, end).isEmpty()) {
                continue;
            }

            target.hurt(DamageSource.playerAttack(player), damage);
            target.setSecondsOnFire(FIRE_SECONDS);
            target.push(look.x * KNOCKBACK_STRENGTH, 0.12D, look.z * KNOCKBACK_STRENGTH);
            target.hurtMarked = true;
        }
    }

    private static void spawnBeamParticles(ServerLevel level, Vec3 start, Vec3 look, Vec3 end) {
        Vec3 side = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();

        Vec3 up = look.cross(side).normalize();

        for (double distance = 0.0D; distance <= RANGE; distance += PARTICLE_STEP) {
            Vec3 center = start.add(look.scale(distance));

            sendForcedParticle(level, ParticleTypes.END_ROD, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sendForcedParticle(level, ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);

            Vec3 p1 = center.add(side.scale(BEAM_RADIUS));
            Vec3 p2 = center.add(side.scale(-BEAM_RADIUS));
            Vec3 p3 = center.add(up.scale(BEAM_RADIUS));
            Vec3 p4 = center.add(up.scale(-BEAM_RADIUS));

            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p1.x, p1.y, p1.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p2.x, p2.y, p2.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, p3.x, p3.y, p3.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, p4.x, p4.y, p4.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);

            if (((int) (distance * 10.0D)) % 6 == 0) {
                sendForcedParticle(level, ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 2, 0.03D, 0.03D, 0.03D, 0.0D);
                sendForcedParticle(level, ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 2, 0.06D, 0.06D, 0.06D, 0.01D);
            }
        }

        sendForcedParticle(level, ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 16, 0.20D, 0.20D, 0.20D, 0.05D);
        sendForcedParticle(level, ParticleTypes.DRAGON_BREATH, end.x, end.y, end.z, 12, 0.16D, 0.16D, 0.16D, 0.02D);
        sendForcedParticle(level, ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 12, 0.12D, 0.12D, 0.12D, 0.01D);
    }

    private static void sendForcedParticle(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                           int count, double offsetX, double offsetY, double offsetZ, double speed) {
        for (ServerPlayer serverPlayer : level.players()) {
            level.sendParticles(serverPlayer, particle, true, x, y, z, count, offsetX, offsetY, offsetZ, speed);
        }
    }
}