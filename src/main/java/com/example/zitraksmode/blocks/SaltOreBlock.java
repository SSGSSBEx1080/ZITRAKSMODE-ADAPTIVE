package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Salt ore always drops four salt when actually removed from the world.
 *
 * playerDestroy is used only for XP on manual player mining.
 * onRemove covers TNT, katana ultimate and every direct destroyBlock call.
 */
public final class SaltOreBlock extends Block {
    public SaltOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Salt itself was already dropped by onRemove().
        // XP stays a reward for normal player mining only.
        ExperienceOrb.award(
                serverLevel,
                Vec3.atCenterOf(pos),
                2 + serverLevel.random.nextInt(4)
        );
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        /*
         * Do not react to an in-place state update of this same block.
         * Do not drop items for theoretical piston movement.
         */
        if (!level.isClientSide
                && !isMoving
                && state.getBlock() != newState.getBlock()) {
            Block.popResource(
                    level,
                    pos,
                    new ItemStack(ModItems.SALT.get(), 4)
            );
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}