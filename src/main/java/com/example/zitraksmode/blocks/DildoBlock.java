package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModBlockEntities;
import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.blockentities.DildoBlockEntity;
import com.example.zitraksmode.entities.DildoSeatEntity;
import com.example.zitraksmode.items.DildoMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DildoBlock
        extends BaseEntityBlock
        implements EntityBlock {

    private static final String STORED_ITEM_TAG =
            "ZitraksStoredDildoItem";

    protected final DildoMaterial material;

    protected static final VoxelShape SHAPE =
            Block.box(
                    5.0D,
                    0.0D,
                    5.0D,
                    11.0D,
                    12.0D,
                    11.0D
            );

    public DildoBlock(DildoMaterial material) {
        super(material.getProperties());

        this.material = material;

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(
                                HorizontalDirectionalBlock.FACING,
                                Direction.NORTH
                        )
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new DildoBlockEntity(
                pos,
                state,
                material
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
    BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.DILDO_BLOCK.get(),
                DildoBlock::serverTick
        );
    }



    private static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DildoBlockEntity blockEntity
    ) {
        if (level.getGameTime() % 5L != 0L) {
            return;
        }

        DildoSeatEntity existingSeat =
                findSeat(level, pos);

        if (existingSeat != null) {
            return;
        }

        DildoSeatEntity seat =
                new DildoSeatEntity(
                        level,
                        pos,
                        Direction.UP,
                        blockEntity.getMaterial(),
                        blockEntity.getMorichekLevel(),
                        blockEntity.getBedWarsLevel()
                );

        level.addFreshEntity(seat);
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
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }

        DildoSeatEntity seat =
                findSeat(level, pos);

        if (seat == null) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            int morichek = 0;
            int bedWars = 0;
            DildoMaterial seatMaterial = material;

            if (blockEntity instanceof DildoBlockEntity dildoBe) {
                morichek =
                        dildoBe.getMorichekLevel();

                bedWars =
                        dildoBe.getBedWarsLevel();

                seatMaterial =
                        dildoBe.getMaterial();
            }

            seat =
                    new DildoSeatEntity(
                            level,
                            pos,
                            Direction.UP,
                            seatMaterial,
                            morichek,
                            bedWars
                    );

            level.addFreshEntity(seat);
        }

        if (player.getVehicle()
                instanceof DildoSeatEntity oldSeat
                && oldSeat != seat) {
            player.stopRiding();
        }

        if (!player.isPassenger()
                || player.getVehicle() != seat) {
            player.startRiding(
                    seat,
                    true
            );
        }

        level.playSound(
                null,
                pos,
                SoundEvents.HONEY_BLOCK_SLIDE,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (level.isClientSide) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof DildoBlockEntity dildoBe)) {
            return;
        }

        /*
         * Сохраняем полный NBT предмета,
         * включая все зачарования.
         */
        CompoundTag persistentData =
                dildoBe.getPersistentData();

        if (stack.hasTag()) {
            persistentData.put(
                    STORED_ITEM_TAG,
                    stack.getTag().copy()
            );
        } else {
            persistentData.remove(
                    STORED_ITEM_TAG
            );
        }

        int morichek =
                EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.MORICHEK.get(),
                        stack
                );

        int bedWars =
                EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.BED_WARS.get(),
                        stack
                );

        dildoBe.setMorichekLevel(morichek);
        dildoBe.setBedWarsLevel(bedWars);
        dildoBe.setMaterial(material);
        dildoBe.setChanged();

        DildoSeatEntity seat =
                new DildoSeatEntity(
                        level,
                        pos,
                        Direction.UP,
                        material,
                        morichek,
                        bedWars
                );

        level.addFreshEntity(seat);
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootContext.Builder builder
    ) {
        BlockEntity blockEntity =
                builder.getOptionalParameter(
                        LootContextParams.BLOCK_ENTITY
                );

        if (!(blockEntity instanceof DildoBlockEntity dildoBe)) {
            return super.getDrops(
                    state,
                    builder
            );
        }

        ItemStack result =
                new ItemStack(
                        this.asItem()
                );

        CompoundTag persistentData =
                dildoBe.getPersistentData();

        if (persistentData.contains(STORED_ITEM_TAG)) {
            CompoundTag storedTag =
                    persistentData
                            .getCompound(STORED_ITEM_TAG)
                            .copy();

            result.setTag(storedTag);
        } else {
            /*
             * Восстановление чар у старых дилдаков,
             * поставленных до этого обновления.
             */
            Map<Enchantment, Integer> enchantments =
                    new HashMap<>();

            if (dildoBe.getMorichekLevel() > 0) {
                enchantments.put(
                        ModEnchantments.MORICHEK.get(),
                        dildoBe.getMorichekLevel()
                );
            }

            if (dildoBe.getBedWarsLevel() > 0) {
                enchantments.put(
                        ModEnchantments.BED_WARS.get(),
                        dildoBe.getBedWarsLevel()
                );
            }

            if (!enchantments.isEmpty()) {
                EnchantmentHelper.setEnchantments(
                        enchantments,
                        result
                );
            }
        }

        return List.of(result);
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                HorizontalDirectionalBlock.FACING
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction direction =
                context.getHorizontalDirection()
                        .getOpposite();

        return this.defaultBlockState().setValue(
                HorizontalDirectionalBlock.FACING,
                direction
        );
    }

    @Nullable
    private static DildoSeatEntity findSeat(
            Level level,
            BlockPos pos
    ) {
        List<DildoSeatEntity> seats =
                level.getEntitiesOfClass(
                        DildoSeatEntity.class,
                        new AABB(pos).inflate(0.7D)
                );

        for (DildoSeatEntity seat : seats) {
            if (seat.blockPosition().equals(pos)) {
                return seat;
            }
        }

        return null;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide) {

            level.getEntitiesOfClass(
                    DildoSeatEntity.class,
                    new AABB(pos).inflate(0.7D)
            ).forEach(entity -> {
                if (entity.blockPosition().equals(pos)) {
                    entity.ejectPassengers();
                    entity.discard();
                }
            });
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                isMoving
        );
    }
}