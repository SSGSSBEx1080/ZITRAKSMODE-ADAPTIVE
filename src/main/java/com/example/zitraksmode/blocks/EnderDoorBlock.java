package com.example.zitraksmode.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EnderDoorBlock
        extends DoorBlock
        implements EntityBlock {

    public EnderDoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        BlockEntity securityEntity =
                getSecurityEntity(level, pos);

        if (securityEntity instanceof IOwnable ownable) {
            UUID owner =
                    ownable.getOwner();

            if (owner != null
                    && !owner.equals(player.getUUID())) {

                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.literal(
                                    "§cДоступ запрещён! Ты не владелец этой двери."
                            ),
                            true
                    );
                }

                return InteractionResult.FAIL;
            }
        }

        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }

        boolean open =
                !state.getValue(OPEN);

        setDoorOpen(
                level,
                pos,
                state,
                open,
                player
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Дверь реагирует только на свою кнопку или свою плиту.
     * Обычный редстоун дверь не открывает.
     */
    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        if (level.isClientSide) {
            return;
        }

        if (block == this) {
            return;
        }

        boolean authorizedSignal =
                hasAuthorizedControlSignal(
                        level,
                        pos
                );

        boolean currentlyOpen =
                state.getValue(OPEN);

        if (authorizedSignal != currentlyOpen) {
            setDoorOpen(
                    level,
                    pos,
                    state,
                    authorizedSignal,
                    null
            );
        }
    }

    private boolean hasAuthorizedControlSignal(
            Level level,
            BlockPos doorPos
    ) {
        BlockEntity doorEntity =
                getSecurityEntity(level, doorPos);

        if (!(doorEntity instanceof IOwnable doorOwnable)) {
            return false;
        }

        UUID doorOwner =
                doorOwnable.getOwner();

        if (doorOwner == null) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            BlockPos controlPos =
                    doorPos.relative(direction);

            BlockState controlState =
                    level.getBlockState(controlPos);

            boolean isEnderControl =
                    controlState.getBlock()
                            instanceof EnderButtonBlock
                            || controlState.getBlock()
                            instanceof EnderPressurePlateBlock;

            if (!isEnderControl) {
                continue;
            }

            BlockEntity controlEntity =
                    level.getBlockEntity(controlPos);

            if (!(controlEntity instanceof IOwnable controlOwnable)) {
                continue;
            }

            UUID controlOwner =
                    controlOwnable.getOwner();

            if (controlOwner == null
                    || !controlOwner.equals(doorOwner)) {
                continue;
            }

            Direction signalDirection =
                    direction.getOpposite();

            int signal =
                    controlState.getSignal(
                            level,
                            controlPos,
                            signalDirection
                    );

            if (signal > 0) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private BlockEntity getSecurityEntity(
            Level level,
            BlockPos pos
    ) {
        BlockEntity entity =
                level.getBlockEntity(pos);

        if (entity instanceof IOwnable) {
            return entity;
        }

        BlockState state =
                level.getBlockState(pos);

        if (state.getBlock() != this
                || !state.hasProperty(HALF)) {
            return null;
        }

        BlockPos otherPos =
                state.getValue(HALF)
                        == DoubleBlockHalf.UPPER
                        ? pos.below()
                        : pos.above();

        BlockEntity otherEntity =
                level.getBlockEntity(otherPos);

        if (otherEntity instanceof IOwnable) {
            return otherEntity;
        }

        return null;
    }

    private void setDoorOpen(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean open,
            @Nullable Player player
    ) {
        if (state.getValue(OPEN) != open) {
            level.setBlock(
                    pos,
                    state.setValue(OPEN, open),
                    10
            );
        }

        DoubleBlockHalf half =
                state.getValue(HALF);

        BlockPos otherPos =
                half == DoubleBlockHalf.LOWER
                        ? pos.above()
                        : pos.below();

        BlockState otherState =
                level.getBlockState(otherPos);

        if (otherState.getBlock() == this
                && otherState.getValue(HALF) != half
                && otherState.getValue(OPEN) != open) {

            level.setBlock(
                    otherPos,
                    otherState.setValue(OPEN, open),
                    10
            );
        }

        level.gameEvent(
                player,
                open
                        ? GameEvent.BLOCK_OPEN
                        : GameEvent.BLOCK_CLOSE,
                pos
        );

        level.playSound(
                player,
                pos,
                open
                        ? SoundEvents.IRON_DOOR_OPEN
                        : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat()
                        * 0.1F
                        + 0.9F
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity entity,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                entity,
                stack
        );

        if (level.isClientSide
                || !(entity instanceof Player player)) {
            return;
        }

        BlockEntity bottomEntity =
                level.getBlockEntity(pos);

        if (bottomEntity instanceof IOwnable ownable) {
            ownable.setOwner(
                    player.getUUID()
            );
        }

        BlockPos topPos =
                pos.above();

        BlockEntity topEntity =
                level.getBlockEntity(topPos);

        if (topEntity instanceof IOwnable ownable) {
            ownable.setOwner(
                    player.getUUID()
            );
        }
    }

    /**
     * Дроп создаётся только для нижней половины.
     * Верхняя половина не выбрасывает второй предмет.
     * Ванильная логика DoorBlock при разрушении верхней
     * половины сама обрабатывает нижнюю.
     */
    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootContext.Builder builder
    ) {
        if (state.getValue(HALF)
                == DoubleBlockHalf.UPPER) {
            return List.of();
        }

        return List.of(
                new ItemStack(
                        this.asItem()
                )
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new EnderSecurityBlockEntity(
                pos,
                state
        );
    }
}