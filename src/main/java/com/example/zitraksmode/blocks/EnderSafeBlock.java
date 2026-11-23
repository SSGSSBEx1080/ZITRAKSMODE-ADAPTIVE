package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class EnderSafeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public EnderSafeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnderSafeBlockEntity safe) {
                UUID ownerID = safe.getOwner();
                UUID playerID = player.getUUID();

                // 1. ПРОВЕРКА ВЛАДЕЛЬЦА
                if (ownerID != null && !ownerID.equals(playerID)) {
                    // ⛔ ВЗЛОМ: Наказание для чужака
                    player.sendSystemMessage(Component.literal("§cПошёл нахуй!! Это не твой сейф!"));
                    
                    if (level instanceof ServerLevel serverLevel) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                        if (lightning != null) {
                            lightning.moveTo(player.getX(), player.getY(), player.getZ());
                            serverLevel.addFreshEntity(lightning);
                        }
                    }
                    player.hurt(DamageSource.MAGIC, player.getMaxHealth() * 0.25f);
                    
                    // 🏆 Ачивка ХОЗЯИНУ (за поимку взломщика)
                    if (level instanceof ServerLevel serverLevel) {
                        Player ownerPlayer = serverLevel.getPlayerByUUID(ownerID);
                        if (ownerPlayer instanceof ServerPlayer owner) {
                            ZitraksMode.ENDER_INTRUDER_CAUGHT.trigger(owner);
                        }
                    }
                    
                    // 🏆 Ачивка ВЗЛОМЩИКУ ("Хуй тебе")
                    if (player instanceof ServerPlayer intruder) {
                        ZitraksMode.INTRUDER_FUCKED.trigger(intruder);
                    }
                    
                    return InteractionResult.CONSUME;
                }

                // 2. ОТКРЫТИЕ (Если владелец)
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer, safe, pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnderSafeBlockEntity safe) {
                safe.setOwner(player.getUUID());
            }
        }
        super.setPlacedBy(level, pos, state, entity, stack);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) { 
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderSafeBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}