package com.example.zitraksmode.items;

import com.example.zitraksmode.entities.SuperTNTMinecartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public class SuperTNTMinecartItem extends Item {

    public SuperTNTMinecartItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof BaseRailBlock)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            RailShape railshape = state.getValue(((BaseRailBlock) state.getBlock()).getShapeProperty());
            double yOffset = railshape.isAscending() ? 0.5D : 0.0D;

            SuperTNTMinecartEntity minecart = new SuperTNTMinecartEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.0625D + yOffset,
                    pos.getZ() + 0.5D
            );

            ItemStack itemstack = context.getItemInHand();
            if (itemstack.hasCustomHoverName()) {
                minecart.setCustomName(itemstack.getHoverName());
            }

            level.addFreshEntity(minecart);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}