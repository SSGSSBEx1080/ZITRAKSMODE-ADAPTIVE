package com.example.zitraksmode.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class EnderPressurePlateBlock extends PressurePlateBlock implements EntityBlock {
    // Зона нажатия плиты (стандартная для ванили)
    private static final AABB TOUCH_AABB = new AABB(-0.5, 0.0, -0.5, 1.5, 0.1, 1.5);

    public EnderPressurePlateBlock(Sensitivity sensitivity, Properties properties) {
        super(sensitivity, properties);
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

    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnderSecurityBlockEntity security) {
            UUID owner = security.getOwner();
            if (owner == null) return 0;
            
            // Реагирует ТОЛЬКО если на плите стоит владелец
            return level.getEntitiesOfClass(Player.class, TOUCH_AABB.move(pos), 
                p -> p.getUUID().equals(owner)).isEmpty() ? 0 : 15;
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderSecurityBlockEntity(pos, state);
    }
}