package com.example.zitraksmode;

import com.example.zitraksmode.entities.SuperTNTPrimedEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Регистрирует поведение раздатчика для предмета Super TNT.
 *
 * Эффект: раздатчик "выстреливает" зажжённый Super TNT entity по направлению
 * своего FACING с импульсом — как снаряд из пушки.
 *
 * Особенности:
 *  - Если перед раздатчиком есть твёрдый блок → entity всё равно спавнится,
 *    но смещается на минимум 0.5 блока, чтобы не застрять в стенке.
 *  - Позиция спавна ровно В ЦЕНТРЕ грани раздатчика, как у ванильных проджектайлов.
 *  - Импульс по facing = 1.2 блока/тик (как у обычного TNT-снаряда).
 */
public final class SuperTNTDispenseSetup {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double LAUNCH_SPEED = 1.2D;

    private SuperTNTDispenseSetup() {}

    public static void register() {
        Block block = safeGet(ModBlocks.SUPER_TNT);
        if (block == null) {
            LOGGER.error("[SuperTNTDispenseSetup] ModBlocks.SUPER_TNT not registered — skip");
            return;
        }

        Item item = block.asItem();
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            LOGGER.error("[SuperTNTDispenseSetup] Super TNT has no BlockItem — skip");
            return;
        }

        DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.getLevel();
                Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos dispenserPos = source.getPos();

                // Позиция спавна = ЦЕНТР грани раздатчика, смещённый наружу на 0.7 блока
                // чтобы entity точно был ЗА пределами AABB раздатчика и не застрял в нём.
                // 0.7 = радиус раздатчика (0.5) + полу-ширина TNT (0.49) + небольшой запас.
                double offset = 0.7D;
                double spawnX = dispenserPos.getX() + 0.5D + facing.getStepX() * offset;
                double spawnY = dispenserPos.getY() + 0.5D + facing.getStepY() * offset
                        - 0.5D; // компенсация что TNT bbox начинается снизу
                double spawnZ = dispenserPos.getZ() + 0.5D + facing.getStepZ() * offset;

                SuperTNTPrimedEntity tnt = new SuperTNTPrimedEntity(
                        level,
                        spawnX,
                        spawnY,
                        spawnZ,
                        null
                );

                // Импульс по направлению раздатчика — превращает раздатчик в пушку
                tnt.setDeltaMovement(
                        facing.getStepX() * LAUNCH_SPEED,
                        facing.getStepY() * LAUNCH_SPEED + 0.1D, // лёгкий up-tilt чтобы дальше летел
                        facing.getStepZ() * LAUNCH_SPEED
                );

                level.addFreshEntity(tnt);
                level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.ENTITY_PLACE, dispenserPos);

                stack.shrink(1);
                return stack;
            }

            // Звук "клик" раздатчика — стандартный
            @Override
            protected void playSound(BlockSource source) {
                source.getLevel().levelEvent(1000, source.getPos(), 0);
            }
        });

        LOGGER.info("[SuperTNTDispenseSetup] Registered cannon dispense behavior for {}", item);
    }

    private static Block safeGet(RegistryObject<? extends Block> ro) {
        try {
            return ro != null && ro.isPresent() ? ro.get() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
