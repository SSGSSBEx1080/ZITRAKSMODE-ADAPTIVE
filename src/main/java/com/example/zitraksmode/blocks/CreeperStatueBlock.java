package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreeperStatueBlock extends Block {
    
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Хитбоксы как у крипера (не полный блок!)
    // Нижняя часть: от пола до середины
    protected static final VoxelShape SHAPE_LOWER = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    // Верхняя часть: от середины до головы
    protected static final VoxelShape SHAPE_UPPER = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    public CreeperStatueBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        
        // Проверяем место сверху
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && 
            level.getBlockState(blockpos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // Ставим верхнюю половину с тем же направлением
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    // ===== ХИТБОКС КРИПЕРА! =====
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? SHAPE_LOWER : SHAPE_UPPER;
    }

    // ===== ПРАВИЛЬНЫЙ ДРОП! =====
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos mainPos = half == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState mainState = level.getBlockState(mainPos);

            // Логика дропа (только если блок реально наш)
            if (mainState.is(this) && !player.isCreative()) {
                ItemStack tool = player.getMainHandItem();
                
                // Проверка на кирку: TieredItem (кирки, топоры и т.д.) + проверка тега или класса
                boolean isPickaxe = tool.getItem() instanceof TieredItem && tool.isCorrectToolForDrops(state);

                if (isPickaxe) {
                    // Дроп самой статуи (только один раз!)
                    if (half == DoubleBlockHalf.LOWER) {
                        popResource(level, mainPos, new ItemStack(this.asItem()));
                    }
                } else {
                    // Дроп яиц
                    if (half == DoubleBlockHalf.LOWER) {
                        popResource(level, mainPos, new ItemStack(ModItems.CREEPER_EGGS.get(), 9));
                    }
                }
            }

            // Удаление второй части
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
