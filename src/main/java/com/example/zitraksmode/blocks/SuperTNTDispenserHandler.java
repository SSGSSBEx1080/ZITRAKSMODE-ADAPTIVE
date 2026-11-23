package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.entities.SuperTNTPrimedEntity;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = "zitraksmode", bus = Mod.EventBusSubscriber.Bus.MOD)
public class SuperTNTDispenserHandler {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenseItemBehavior behavior = new DefaultDispenseItemBehavior() {
                @Override
                protected ItemStack execute(BlockSource source, ItemStack stack) {
                    ServerLevel level = source.getLevel();
                    Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                    Position position = DispenserBlock.getDispensePosition(source);

                    SuperTNTPrimedEntity tnt = new SuperTNTPrimedEntity(
                            level,
                            position.x(),
                            position.y(),
                            position.z(),
                            null
                    );

                    level.addFreshEntity(tnt);
                    level.playSound(
                            null,
                            tnt.getX(),
                            tnt.getY(),
                            tnt.getZ(),
                            SoundEvents.TNT_PRIMED,
                            SoundSource.BLOCKS,
                            1.0F,
                            1.0F
                    );

                    stack.shrink(1);
                    return stack;
                }
            };

            DispenserBlock.registerBehavior(ModBlocks.SUPER_TNT.get().asItem(), behavior);
        });
    }
}