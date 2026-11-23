package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModBlockEntities;
import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Printer3DBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    private static final String STORED_ITEM_TAG = "ZitraksStoredPrinterItem";

    public Printer3DBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new Printer3DBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.PRINTER_3D.get(),
                Printer3DBlockEntity::serverTick
        );
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType
                ? (BlockEntityTicker<A>) ticker
                : null;
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

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof Printer3DBlockEntity printer)) {
            return InteractionResult.PASS;
        }

        if (!printer.canOpenGui(player)) {
            String occupant =
                    printer.getCurrentUserName();

            if (occupant == null) {
                occupant = "другим игроком";
            }

            player.displayClientMessage(
                    Component.literal(
                            "§cПринтер занят игроком §e"
                                    + occupant
                    ),
                    true
            );

            return InteractionResult.SUCCESS;
        }

        printer.setCurrentUser(player);

        NetworkHooks.openScreen(
                (ServerPlayer) player,
                printer,
                pos
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

        if (!(blockEntity instanceof Printer3DBlockEntity printer)) {
            return;
        }

        /*
         * Сохраняем полный NBT предмета принтера.
         *
         * В нём находятся:
         * - все зачарования;
         * - нестандартные данные предмета;
         * - дополнительные настройки, если они есть.
         *
         * getPersistentData() автоматически сохраняется Forge
         * вместе с BlockEntity.
         */
        CompoundTag persistentData =
                printer.getPersistentData();

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

        int baryga =
                EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.BARYGA.get(),
                        stack
                );

        int nahuya =
                EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.NAHUYA.get(),
                        stack
                );

        int zaebis =
                EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.ZAEBIS.get(),
                        stack
                );

        /*
         * Всегда записываем значения.
         * Так старые значения не останутся при установке
         * обычного незачарованного принтера.
         */
        printer.setBarygaLevel(baryga);
        printer.setNahuyaLevel(nahuya);
        printer.setZaebisLevel(zaebis);

        printer.setChanged();
    }

    /**
     * Выпадение принтера с сохранёнными чарами.
     *
     * Работает при обычном ломании и Шёлковом касании,
     * если Minecraft использует стандартную loot-систему.
     */
    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootContext.Builder builder
    ) {
        BlockEntity blockEntity =
                builder.getOptionalParameter(
                        LootContextParams.BLOCK_ENTITY
                );

        if (blockEntity instanceof Printer3DBlockEntity printer) {
            ItemStack result =
                    new ItemStack(this.asItem());

            CompoundTag persistentData =
                    printer.getPersistentData();

            if (persistentData.contains(STORED_ITEM_TAG)) {
                CompoundTag storedItemTag =
                        persistentData
                                .getCompound(STORED_ITEM_TAG)
                                .copy();

                result.setTag(storedItemTag);
            }

            return List.of(result);
        }

        return super.getDrops(
                state,
                builder
        );
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            /*
             * Внутренние предметы принтера выбрасываются отдельно.
             * Сам предмет принтера обрабатывается через getDrops().
             */
            if (blockEntity instanceof Printer3DBlockEntity printer) {
                printer.drops();
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                isMoving
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return this.defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    net.minecraft.world.level.block.Block,
                    BlockState
                    > builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {
        return state.setValue(
                FACING,
                rotation.rotate(
                        state.getValue(FACING)
                )
        );
    }

    @Override
    public BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {
        return state.rotate(
                mirror.getRotation(
                        state.getValue(FACING)
                )
        );
    }
}