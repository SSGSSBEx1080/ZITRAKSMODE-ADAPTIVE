package com.example.zitraksmode.blocks;

import com.example.zitraksmode.blockentities.HentaiBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class HentaiBlock extends BaseEntityBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    public HentaiBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HentaiBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HentaiBlockEntity entity) {
                LOGGER.info("🎲 Hentai block placed -> rerolling only unlocked faces");
                entity.randomizeTextures(level.random);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HentaiBlockEntity entity)) {
            return InteractionResult.PASS;
        }

        String face = hit.getDirection().getSerializedName();
        String faceName = entity.getFaceDisplayName(face);
        boolean locked = entity.toggleFaceLock(face);

        player.displayClientMessage(
                Component.literal(locked
                        ? faceName + " сохранена."
                        : faceName + " снова случайная."),
                true
        );

        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        ItemStack stack = new ItemStack(asItem());
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof HentaiBlockEntity hentaiBlockEntity && hentaiBlockEntity.hasAnyLockedFaces()) {
            hentaiBlockEntity.saveToItemTag(stack.getOrCreateTag());
        }

        return List.of(stack);
    }

    public ItemStack getCloneItemStack(LootContext.Builder builder) {
        ItemStack stack = new ItemStack(asItem());
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof HentaiBlockEntity hentaiBlockEntity && hentaiBlockEntity.hasAnyLockedFaces()) {
            hentaiBlockEntity.saveToItemTag(stack.getOrCreateTag());
        }

        return stack;
    }
}