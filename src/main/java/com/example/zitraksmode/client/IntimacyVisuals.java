package com.example.zitraksmode.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Random;

public final class IntimacyVisuals {
    private static final Random RANDOM = new Random();

    private IntimacyVisuals() {}

    public static void spawnHeartParticles(Entity entity, int count) {
        Level level = entity.level;
        for (int i = 0; i < count; i++) {
            level.addParticle(
                    ParticleTypes.HEART,
                    entity.getX() + (RANDOM.nextDouble() - 0.5) * 1.2,
                    entity.getY() + RANDOM.nextDouble() * 1.8 + 0.2,
                    entity.getZ() + (RANDOM.nextDouble() - 0.5) * 1.2,
                    0,
                    0.1,
                    0
            );
        }
    }

    public static void spawnSparkParticles(Entity entity, int count) {
        Level level = entity.level;
        for (int i = 0; i < count; i++) {
            level.addParticle(
                    ParticleTypes.WITCH,
                    entity.getX() + (RANDOM.nextDouble() - 0.5) * 1.2,
                    entity.getY() + RANDOM.nextDouble() * 1.8 + 0.2,
                    entity.getZ() + (RANDOM.nextDouble() - 0.5) * 1.2,
                    0.5,
                    0.3,
                    0.8
            );
        }
    }

    public static void spawnFireworks(Entity entity) {
        Level level = entity.level;
        for (int i = 0; i < 8; i++) {
            level.addParticle(
                    ParticleTypes.FIREWORK,
                    entity.getX() + (RANDOM.nextDouble() - 0.5) * 1.0,
                    entity.getY() + 1.5 + RANDOM.nextDouble() * 0.5,
                    entity.getZ() + (RANDOM.nextDouble() - 0.5) * 1.0,
                    0,
                    0.5,
                    0
            );
        }
    }
}