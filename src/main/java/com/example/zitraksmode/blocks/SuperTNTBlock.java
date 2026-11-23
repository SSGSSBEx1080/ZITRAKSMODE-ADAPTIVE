package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.SuperTNTPrimedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class SuperTNTBlock extends Block {

    public SuperTNTBlock() {
        super(BlockBehaviour.Properties.of(Material.EXPLOSIVE)
                .strength(0.0F)
                .sound(SoundType.GRASS));
    }

    /**
     * Обычный поджёг — использует ванильный конструктор (без noclip, без прыжка вверх).
     */
    public static void prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (level.isClientSide) {
            return;
        }

        ServerPlayer playerForAchievement = null;
        if (igniter instanceof ServerPlayer sp) {
            playerForAchievement = sp;
        } else if (igniter == null) {
            Player nearest = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16.0, false);
            if (nearest instanceof ServerPlayer sp) {
                playerForAchievement = sp;
            }
        }

        if (playerForAchievement != null) {
            ZitraksMode.SUPER_TNT_ACTIVATED.trigger(playerForAchievement);
        }

        SuperTNTPrimedEntity tnt = new SuperTNTPrimedEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.01D,
                pos.getZ() + 0.5D,
                igniter
        );

        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(),
                SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.removeBlock(pos, false);
    }

    /**
     * Поджёг от взрыва соседа — ванильное поведение: обычная entity со случайным
     * сокращённым фитилём, без noclip, без лишних импульсов.
     */
    public static void primeFromExplosion(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (level.isClientSide) {
            return;
        }

        SuperTNTPrimedEntity tnt = new SuperTNTPrimedEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.01D,
                pos.getZ() + 0.5D,
                igniter
        );
        // Случайный сокращённый фитиль — как ваниль делает при цепной детонации
        int currentFuse = tnt.getFuse();
        tnt.setFuse(level.random.nextInt(currentFuse / 4) + currentFuse / 8);

        level.addFreshEntity(tnt);
        level.removeBlock(pos, false);
    }

    private static boolean isHeatSource(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock || state.getFluidState().is(FluidTags.LAVA);
    }

    private static boolean shouldPrimeFromHeat(Level level, BlockPos pos) {
        if (isHeatSource(level.getBlockState(pos.above()))) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (isHeatSource(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.hasNeighborSignal(pos) || shouldPrimeFromHeat(level, pos)) {
            prime(level, pos, null);
            return;
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack item = player.getItemInHand(hand);

        if (item.is(Items.FLINT_AND_STEEL) || item.is(Items.FIRE_CHARGE)) {
            prime(level, pos, player);

            if (!player.isCreative()) {
                if (item.is(Items.FLINT_AND_STEEL)) {
                    item.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                } else {
                    item.shrink(1);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        prime(level, pos, igniter);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        // ВАЖНО: цепная детонация — здесь нужен noclip
        primeFromExplosion(level, pos, explosion.getSourceMob());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos) || shouldPrimeFromHeat(level, pos)) {
            prime(level, pos, null);
        }
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && projectile.isOnFire()) {
            prime(level, hit.getBlockPos(), projectile.getOwner() instanceof LivingEntity living ? living : null);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
