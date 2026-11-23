package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class EnderFurnaceBlock extends AbstractFurnaceBlock {
    public EnderFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EnderFurnaceBlockEntity furnace) {
            UUID ownerID = furnace.getOwner();
            UUID playerID = player.getUUID();

            if (ownerID != null && !ownerID.equals(playerID)) {
                // ⛔ ВЗЛОМ
                player.sendSystemMessage(Component.literal("§cЭто не твоя печка! Пошёл нахуй отсюда!!!"));
                
                if (level instanceof ServerLevel sl) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(player.getX(), player.getY(), player.getZ());
                        sl.addFreshEntity(bolt);
                    }
                }
                player.hurt(DamageSource.MAGIC, player.getMaxHealth() * 0.25f);

                // 🏆 Ачивка ХОЗЯИНУ
                if (level instanceof ServerLevel sl) {
                    ServerPlayer owner = (ServerPlayer) sl.getPlayerByUUID(ownerID);
                    if (owner != null) {
                        ZitraksMode.ENDER_INTRUDER_CAUGHT.trigger(owner);
                    }
                }
                
                // 🏆 Ачивка ВЗЛОМЩИКУ ("Хуй тебе")
                if (player instanceof ServerPlayer intruder) {
                    ZitraksMode.INTRUDER_FUCKED.trigger(intruder);
                }
                
                return;
            }

            if (player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, furnace, pos);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnderFurnaceBlockEntity furnace) {
                furnace.setOwner(player.getUUID());
            }
        }
        super.setPlacedBy(level, pos, state, entity, stack);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ENDER_FURNACE.get(), 
                (lvl, pos, st, be) -> EnderFurnaceBlockEntity.serverTick(lvl, pos, st, be));
    }
}