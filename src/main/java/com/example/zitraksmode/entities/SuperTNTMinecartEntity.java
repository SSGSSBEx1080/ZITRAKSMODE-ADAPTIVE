package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SuperTNTMinecartEntity extends MinecartTNT {

    private static final float EXPLOSION_POWER = 20.0F;

    public SuperTNTMinecartEntity(EntityType<? extends MinecartTNT> type, Level level) {
        super(type, level);
    }

    public SuperTNTMinecartEntity(Level level, double x, double y, double z) {
        this(ModEntities.SUPER_TNT_MINECART.get(), level);
        this.setPos(x, y, z);
    }

    // === Дроп предмета при сломе ===
    @Override
    protected Item getDropItem() {
        return ModItems.SUPER_TNT_MINECART.get();
    }

    // === ВАЖНО: визуально внутри вагонетки лежит НАШ Super TNT блок ===
    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.SUPER_TNT.get().defaultBlockState();
    }

    // (Опционально) — приподнять/опустить блок внутри вагонетки.
    // Ванильный TNT-кар возвращает 6 (стандарт). Можно оставить дефолт.
    // @Override
    // public int getDefaultDisplayOffset() { return 6; }

    @Override
    protected void explode(double motionSq) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        double boost = Math.min(Math.sqrt(motionSq), 5.0D);
        float power = (float) (EXPLOSION_POWER + this.random.nextDouble() * 2.0D * boost);

        this.level.explode(
                this,
                this.getX(),
                this.getY(),
                this.getZ(),
                power,
                Explosion.BlockInteraction.BREAK
        );

        dryFluids(serverLevel);
        scheduleLightningStrikes(serverLevel);
        this.discard();
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
                                LivingEntity::isAlive
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
}
