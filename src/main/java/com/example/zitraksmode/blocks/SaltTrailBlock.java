package com.example.zitraksmode.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * White one-use redstone fuse with vanilla redstone-style side/up connections.
 * It can run down a one-block slope exactly like redstone wire, then burns away.
 */
public final class SaltTrailBlock extends Block {
    public static final EnumProperty<RedstoneSide> NORTH = EnumProperty.create("north", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> EAST = EnumProperty.create("east", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> SOUTH = EnumProperty.create("south", RedstoneSide.class);
    public static final EnumProperty<RedstoneSide> WEST = EnumProperty.create("west", RedstoneSide.class);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape DOT = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 1.0D, 11.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 1.0D, 5.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0D, 0.0D, 7.0D, 16.0D, 1.0D, 9.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(7.0D, 0.0D, 11.0D, 9.0D, 1.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 0.0D, 7.0D, 5.0D, 1.0D, 9.0D);
    private static final VoxelShape NORTH_UP = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 1.0D);
    private static final VoxelShape EAST_UP = Block.box(15.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);
    private static final VoxelShape SOUTH_UP = Block.box(7.0D, 0.0D, 15.0D, 9.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_UP = Block.box(0.0D, 0.0D, 7.0D, 1.0D, 16.0D, 9.0D);
    private static final int BURN_TICKS = 2;

    public SaltTrailBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = updateConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = DOT;
        shape = addConnectionShape(shape, state.getValue(NORTH), NORTH_SHAPE, NORTH_UP);
        shape = addConnectionShape(shape, state.getValue(EAST), EAST_SHAPE, EAST_UP);
        shape = addConnectionShape(shape, state.getValue(SOUTH), SOUTH_SHAPE, SOUTH_UP);
        shape = addConnectionShape(shape, state.getValue(WEST), WEST_SHAPE, WEST_UP);
        return shape;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return direction.getAxis().isHorizontal() ? updateConnections(level, pos, state) : state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                BlockPos neighbourPos, boolean movedByPiston) {
        BlockState connectedState = updateConnections(level, pos, state);
        if (!connectedState.equals(state)) {
            level.setBlock(pos, connectedState, Block.UPDATE_CLIENTS);
            state = connectedState;
        }

        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && !state.getValue(LIT)
                && level.hasNeighborSignal(pos)) {
            ignite(serverLevel, pos, state);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos nextPos = pos.relative(direction);
            BlockState nextState = level.getBlockState(nextPos);
            if (nextState.getBlock() instanceof SaltTrailBlock && !nextState.getValue(LIT)) {
                ignite(level, nextPos, nextState);
            }

            // Propagate down and up one block, matching redstone slope connections.
            BlockPos lowerPos = nextPos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            if (lowerState.getBlock() instanceof SaltTrailBlock && !lowerState.getValue(LIT)) {
                ignite(level, lowerPos, lowerState);
            }

            BlockPos upperPos = nextPos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.getBlock() instanceof SaltTrailBlock && !upperState.getValue(LIT)) {
                ignite(level, upperPos, upperState);
            }
        }

        level.removeBlock(pos, false);
    }

    private static VoxelShape addConnectionShape(VoxelShape base, RedstoneSide side,
                                                  VoxelShape flat, VoxelShape upward) {
        if (side == RedstoneSide.NONE) return base;
        return side == RedstoneSide.UP
                ? Shapes.or(base, flat, upward)
                : Shapes.or(base, flat);
    }

    private static BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, connectionTo(level, pos, Direction.NORTH))
                .setValue(EAST, connectionTo(level, pos, Direction.EAST))
                .setValue(SOUTH, connectionTo(level, pos, Direction.SOUTH))
                .setValue(WEST, connectionTo(level, pos, Direction.WEST));
    }

    private static RedstoneSide connectionTo(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockPos nextPos = pos.relative(direction);
        BlockState nextState = level.getBlockState(nextPos);
        if (isWire(nextState) || nextState.canRedstoneConnectTo(level, nextPos, direction)) {
            return RedstoneSide.SIDE;
        }

        if (nextState.isRedstoneConductor(level, nextPos)) {
            BlockState aboveState = level.getBlockState(nextPos.above());
            if (isWire(aboveState) || aboveState.canRedstoneConnectTo(level, nextPos.above(), direction)) {
                return RedstoneSide.UP;
            }
        } else {
            BlockState belowState = level.getBlockState(nextPos.below());
            if (isWire(belowState) || belowState.canRedstoneConnectTo(level, nextPos.below(), direction)) {
                return RedstoneSide.SIDE;
            }
        }
        return RedstoneSide.NONE;
    }

    private static boolean isWire(BlockState state) {
        return state.getBlock() instanceof SaltTrailBlock || state.getBlock() instanceof RedStoneWireBlock;
    }

    private static void ignite(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(LIT)) return;
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
        level.scheduleTick(pos, state.getBlock(), BURN_TICKS);
    }
}
