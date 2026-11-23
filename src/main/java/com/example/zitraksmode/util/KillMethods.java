package com.example.zitraksmode.util;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.MahoragaEntity;
import com.example.zitraksmode.items.DeathNoteRequiemItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class KillMethods {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean executeKill(Player player, String targetName, byte killMode, ItemStack note) {
        Level level = player.level;
        boolean interDimensional = note.getItem() instanceof DeathNoteRequiemItem;
        Entity target = findTarget(level, player, targetName, interDimensional);
        if (target == null) return false;

        switch (killMode) {
            case 0 -> target.kill();

            case 1 -> {
                if (target instanceof LivingEntity living && level instanceof ServerLevel serverLevel) {
                    spawnAnvilExecution(serverLevel, living);
                }
            }

            case 2 -> {
                if (target instanceof LivingEntity living) {
                    spawnLavaCube(level, living);
                    living.setSecondsOnFire(12);
                }
            }

            case 3 -> {
                target.teleportTo(target.getX(), target.getY() + 67.0D, target.getZ());
                if (target instanceof LivingEntity living) {
                    living.fallDistance = 0.0F;
                    living.setDeltaMovement(0.0D, 0.0D, 0.0D);
                }
            }

            case 4 -> {
                if (target instanceof LivingEntity living && level instanceof ServerLevel serverLevel) {
                    lightningExecution(serverLevel, living);
                }
            }

            case 5 -> {
                if (target instanceof LivingEntity living) {
                    explosionExecution(level, living);
                }
            }
        }

        recordMethodAndCheck(player, killMode);
        return true;
    }

    private static void spawnAnvilExecution(ServerLevel level, LivingEntity living) {
        BlockPos base = living.blockPosition();

        int centerX = base.getX();
        int centerY = base.getY() + 28;
        int centerZ = base.getZ();

        spawnAnvil(level, new BlockPos(centerX, centerY, centerZ));
        spawnAnvil(level, new BlockPos(centerX, centerY + 3, centerZ));
        spawnAnvil(level, new BlockPos(centerX, centerY + 6, centerZ));

        spawnAnvil(level, new BlockPos(centerX + 1, centerY + 6, centerZ));
        spawnAnvil(level, new BlockPos(centerX - 1, centerY + 6, centerZ));
    }

    private static void spawnAnvil(ServerLevel level, BlockPos pos) {
        FallingBlockEntity anvil = FallingBlockEntity.fall(level, pos, Blocks.ANVIL.defaultBlockState());
        anvil.dropItem = false;
        anvil.setHurtsEntities(40.0F, 200);
        anvil.setDeltaMovement(0.0D, -0.25D, 0.0D);
    }

    private static void spawnLavaCube(Level level, LivingEntity living) {
        BlockPos center = living.blockPosition();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void lightningExecution(ServerLevel level, LivingEntity living) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(living.getX(), living.getY(), living.getZ());

            // Для крипера молния только визуально/заряжает, без урона
            if (living instanceof Creeper creeper) {
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
                creeper.thunderHit(level, bolt);
                return;
            }

            // Для всех остальных это настоящая молния
            bolt.setVisualOnly(false);
            level.addFreshEntity(bolt);
        }

        dealFullMaxHealthDamage(living, DamageSource.LIGHTNING_BOLT);
    }

    private static void explosionExecution(Level level, LivingEntity living) {
        level.explode(
                null,
                living.getX(),
                living.getY(),
                living.getZ(),
                6.0F,
                false,
                Explosion.BlockInteraction.DESTROY
        );

        dealFullMaxHealthDamage(living, DamageSource.explosion((LivingEntity) null));
    }

    private static void dealFullMaxHealthDamage(LivingEntity living, DamageSource source) {
        float maxHealth = living.getMaxHealth();

        if (living instanceof MahoragaEntity) {
            living.hurt(source, maxHealth);
            return;
        }

        living.hurt(source, maxHealth);

        if (living.isAlive()) {
            living.setHealth(Math.max(0.0F, living.getHealth() - maxHealth));
        }
    }

    private static void recordMethodAndCheck(Player player, int method) {
        if (player.level.isClientSide || !(player instanceof ServerPlayer sp)) return;

        var data = sp.getPersistentData();
        int mask = data.getInt("DeathNoteMethodsMask");

        mask |= (1 << method);
        data.putInt("DeathNoteMethodsMask", mask);

        if (mask == 63) {
            ZitraksMode.DEATH_NOTE_ALL_METHODS.trigger(sp);
            LOGGER.info("📓 Achievement 'Master of Execution' triggered for {}", sp.getName().getString());
        }
    }

    public static Entity findTarget(Level level, Player player, String name, boolean interDimensional) {
        Player targetPlayer = null;

        if (level instanceof ServerLevel serverLevel) {
            targetPlayer = serverLevel.getServer().getPlayerList().getPlayerByName(name);
        }

        if (targetPlayer == null) {
            targetPlayer = level.players().stream()
                    .filter(p -> p.getGameProfile().getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }

        if (targetPlayer != null) {
            LOGGER.info("[DeathNote] Found player: {}", targetPlayer.getName().getString());
            return targetPlayer;
        }

        Entity mob = findMob(level, name, player);
        if (mob != null) {
            LOGGER.info("[DeathNote] Found mob: {} (type: {})", mob.getName().getString(), EntityType.getKey(mob.getType()).getPath());
            return mob;
        }

        LOGGER.info("[DeathNote] Target not found: {}", name);
        return null;
    }

    public static Entity findTargetByUuid(ServerPlayer player, UUID uuid) {
        if (player.getServer() == null) {
            return null;
        }

        ServerPlayer targetPlayer = player.getServer().getPlayerList().getPlayer(uuid);
        if (targetPlayer != null) {
            return targetPlayer;
        }

        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    private static Entity findMob(Level level, String name, Player player) {
        AABB searchArea = new AABB(player.blockPosition()).inflate(256);
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                e -> !isBoss(e) && matchesName(e, name)
        );

        if (!entities.isEmpty()) {
            entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
            return entities.get(0);
        }

        return null;
    }

    private static boolean matchesName(LivingEntity entity, String name) {
        if (entity.getDisplayName().getString().equalsIgnoreCase(name)) return true;

        String typeName = EntityType.getKey(entity.getType()).getPath();
        if (typeName.equalsIgnoreCase(name)) return true;

        String russian = getRussianName(entity);
        return russian != null && russian.equalsIgnoreCase(name);
    }

    private static String getRussianName(LivingEntity entity) {
        String type = EntityType.getKey(entity.getType()).getPath();

        return switch (type) {
            case "creeper" -> "Крипер";
            case "zombie" -> "Зомби";
            case "skeleton" -> "Скелет";
            case "spider" -> "Паук";
            case "cave_spider" -> "Пещерный паук";
            case "enderman" -> "Эндермен";
            case "witch" -> "Ведьма";
            case "wolf" -> "Волк";
            case "pig" -> "Свинья";
            case "cow" -> "Корова";
            case "chicken" -> "Курица";
            case "sheep" -> "Овца";
            case "horse" -> "Лошадь";
            case "villager" -> "Житель";
            default -> null;
        };
    }

    public static boolean isBoss(Entity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof Warden
                || entity instanceof ElderGuardian;
    }
}