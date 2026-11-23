package com.example.zitraksmode.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Эндер-люк.
 * - Открывается ТОЛЬКО рукой (игнорирует материал METAL).
 * - Открывается ТОЛЬКО владельцем.
 * - Игнорирует редстоун.
 */
public class EnderTrapdoorBlock extends TrapDoorBlock implements EntityBlock {
    public EnderTrapdoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 1. ПРОВЕРКА ВЛАДЕЛЬЦА
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnderSecurityBlockEntity security) {
            if (security.getOwner() != null && !security.getOwner().equals(player.getUUID())) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cТы похож на пидораса! Тебя не звали уёбище!"), true);
                }
                return InteractionResult.FAIL;
            }
        }

        // 2. ЛОГИКА ОТКРЫТИЯ (ВРУЧНУЮ)
        // Мы НЕ вызываем super.use(), потому что он запрещает открытие для Material.METAL
        state = state.cycle(OPEN);
        level.setBlock(pos, state, 2);
        
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        this.playSound(player, level, pos, state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // 3. ОТКЛЮЧЕНИЕ РЕДСТОУНА
    // Оставляем метод пустым, чтобы люк не реагировал на рычаги и кнопки рядом
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // Игнорируем редстоун
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnderSecurityBlockEntity security) {
                security.setOwner(player.getUUID());
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderSecurityBlockEntity(pos, state);
    }
}