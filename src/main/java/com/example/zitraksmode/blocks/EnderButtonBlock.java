package com.example.zitraksmode.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StoneButtonBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnderButtonBlock extends StoneButtonBlock implements EntityBlock {
    public EnderButtonBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IOwnable ownable) {
            if (ownable.getOwner() != null && !ownable.getOwner().equals(player.getUUID())) {
                if (!level.isClientSide) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Эта кнопка подчиняется только хозяину!"), true);
                }
                return InteractionResult.FAIL;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IOwnable ownable) {
                ownable.setOwner(player.getUUID());
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderSecurityBlockEntity(pos, state);
    }
}