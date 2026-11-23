package com.example.zitraksmode.items;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.access.BullCowAccess;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * Нож Нины:
 *
 * - стрижёт овец;
 * - стрижёт грибных коров;
 * - стрижёт быков;
 * - превращает обычного быка в лысого или медного;
 * - вырезает тыквы;
 * - снимает кору с дерева;
 * - собирает спелые ягоды;
 * - быстро ломает листву, шерсть и паутину;
 * - наносит дополнительный урон со спины и сбоку
 *   через NinaKnifeCombatHandler.
 */
public final class NinaKnifeItem extends SwordItem {

    public NinaKnifeItem(
            Tier tier,
            int attackDamageModifier,
            float attackSpeedModifier,
            Properties properties
    ) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WOOL)
                || state.is(Blocks.COBWEB)) {
            return 100.0F;
        }

        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return state.is(BlockTags.WOOL)
                || state.is(Blocks.COBWEB)
                || state.is(Blocks.GRASS_BLOCK)
                || super.isCorrectToolForDrops(state);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity entity,
            InteractionHand hand
    ) {
        /*
         * Стрижка быка ножом.
         *
         * Работает только на:
         * - быке;
         * - обычном варианте быка;
         * - быке, которого ещё не стригли.
         */
        if (entity instanceof Cow cow
                && cow instanceof BullCowAccess access
                && access.zitraksmode$isBull()
                && access.zitraksmode$getBullVariant()
                        == BullCowAccess.VARIANT_NORMAL) {

            if (!player.level.isClientSide) {
                boolean copper = player.level.random.nextBoolean();

                if (copper) {
                    access.zitraksmode$setBullVariant(
                            BullCowAccess.VARIANT_COPPER
                    );

                    if (player instanceof ServerPlayer serverPlayer) {
                        grantBullAdvancement(
                                serverPlayer,
                                "22_milk/copper_bull"
                        );
                    }
                } else {
                    access.zitraksmode$setBullVariant(
                            BullCowAccess.VARIANT_BALD
                    );

                    dropBullLeather(
                            cow,
                            1 + player.level.random.nextInt(2)
                    );
                }

                damageItem(stack, player, hand);

                player.level.playSound(
                        null,
                        cow.blockPosition(),
                        SoundEvents.SHEEP_SHEAR,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.85F
                                + player.level.random.nextFloat() * 0.3F
                );
            }

            return InteractionResult.sidedSuccess(
                    player.level.isClientSide
            );
        }

        /*
         * Обычная стрижка овцы ножом.
         */
        if (entity instanceof Sheep sheep
                && !player.level.isClientSide
                && sheep.readyForShearing()) {

            sheep.shear(SoundSource.PLAYERS);
            damageItem(stack, player, hand);

            return InteractionResult.SUCCESS;
        }

        /*
         * Стрижка грибной коровы ножом.
         */
        if (entity instanceof MushroomCow mushroomCow
                && !player.level.isClientSide
                && mushroomCow.readyForShearing()) {

            mushroomCow.shear(SoundSource.PLAYERS);
            damageItem(stack, player, hand);

            return InteractionResult.SUCCESS;
        }

        return super.interactLivingEntity(
                stack,
                player,
                entity,
                hand
        );
    }

    /**
     * Наносит ножу урон и показывает анимацию поломки.
     */
    private static void damageItem(
            ItemStack stack,
            Player player,
            InteractionHand hand
    ) {
        stack.hurtAndBreak(
                1,
                player,
                brokenPlayer ->
                        brokenPlayer.broadcastBreakEvent(hand)
        );
    }

    /**
     * Выдаёт достижение за получение медного быка.
     */
    private static void grantBullAdvancement(
            ServerPlayer player,
            String advancementPath
    ) {
        if (player.getServer() == null) {
            return;
        }

        Advancement advancement = player.getServer()
                .getAdvancements()
                .getAdvancement(
                        new ResourceLocation(
                                ZitraksMode.MODID,
                                advancementPath
                        )
                );

        if (advancement == null) {
            return;
        }

        /*
         * Выдаём все критерии достижения.
         */
        for (String criterion : advancement.getCriteria().keySet()) {
            player.getAdvancements().award(
                    advancement,
                    criterion
            );
        }
    }

    /**
     * Выпадение кожи при превращении обычного быка в лысого.
     */
    private static void dropBullLeather(
            Cow cow,
            int count
    ) {
        if (count <= 0) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(
                cow.level,
                cow.getX(),
                cow.getY(0.6D),
                cow.getZ(),
                new ItemStack(Items.LEATHER, count)
        );

        itemEntity.setDefaultPickUpDelay();
        cow.level.addFreshEntity(itemEntity);
    }

    /**
     * Нож умеет выполнять действия топора.
     */
    @Override
    public boolean canPerformAction(
            ItemStack stack,
            ToolAction toolAction
    ) {
        return toolAction == ToolActions.AXE_STRIP
                || super.canPerformAction(stack, toolAction);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        /*
         * Вырезание тыквы.
         */
        if (state.getBlock() instanceof PumpkinBlock) {
            if (!level.isClientSide) {
                Direction facing;

                if (context.getClickedFace().getAxis().isVertical()) {
                    facing = context
                            .getHorizontalDirection()
                            .getOpposite();
                } else {
                    facing = context.getClickedFace();
                }

                level.setBlock(
                        pos,
                        Blocks.CARVED_PUMPKIN.defaultBlockState()
                                .setValue(
                                        CarvedPumpkinBlock.FACING,
                                        facing
                                ),
                        Block.UPDATE_ALL
                );

                level.playSound(
                        null,
                        pos,
                        SoundEvents.PUMPKIN_CARVE,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                );

                damageItem(
                        stack,
                        context.getPlayer(),
                        context.getHand()
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        /*
         * Снятие коры с дерева.
         */
        BlockState strippedState = state.getToolModifiedState(
                context,
                ToolActions.AXE_STRIP,
                false
        );

        if (strippedState != null) {
            if (!level.isClientSide) {
                level.setBlock(
                        pos,
                        strippedState,
                        Block.UPDATE_ALL
                );

                level.playSound(
                        null,
                        pos,
                        SoundEvents.AXE_STRIP,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                );

                damageItem(
                        stack,
                        context.getPlayer(),
                        context.getHand()
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        /*
         * Сбор спелых ягод.
         */
        if (state.getBlock() instanceof SweetBerryBushBlock
                && state.getValue(SweetBerryBushBlock.AGE) > 1) {

            if (!level.isClientSide) {
                Block.popResource(
                        level,
                        pos,
                        new ItemStack(
                                Items.SWEET_BERRIES,
                                1 + level.random.nextInt(2)
                        )
                );

                level.setBlock(
                        pos,
                        state.setValue(
                                SweetBerryBushBlock.AGE,
                                1
                        ),
                        Block.UPDATE_ALL
                );

                damageItem(
                        stack,
                        context.getPlayer(),
                        context.getHand()
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        return super.useOn(context);
    }
}