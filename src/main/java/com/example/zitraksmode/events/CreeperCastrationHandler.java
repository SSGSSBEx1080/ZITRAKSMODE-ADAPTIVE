package com.example.zitraksmode.events;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.NinaKnifeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Creepers can be castrated by vanilla shears or Nina's knife. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CreeperCastrationHandler {
    private CreeperCastrationHandler() {
    }

    @SubscribeEvent
    public static void onCreeperInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Creeper creeper)) return;

        ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());
        // Nina's knife deliberately has the same creeper interaction as shears.
        if (!heldItem.is(Items.SHEARS) && !(heldItem.getItem() instanceof NinaKnifeItem)) return;

        CompoundTag tag = creeper.getPersistentData();
        if (tag.getBoolean("IsCasted")) return;

        if (!event.getLevel().isClientSide) {
            tag.putBoolean("IsCasted", true);

            if (event.getEntity() instanceof ServerPlayer player) {
                if (creeper.isPowered()) {
                    ZitraksMode.CHARGED_CREEPER_CASTRATED.trigger(player);
                } else {
                    ZitraksMode.CREEPER_CASTRATED.trigger(player);
                }
            }

            ItemStack eggs = creeper.isPowered()
                    ? new ItemStack(ModItems.CHARGED_CREEPER_EGGS.get())
                    : new ItemStack(ModItems.CREEPER_EGGS.get());
            eggs.setCount(1 + event.getLevel().random.nextInt(2));

            ItemEntity itemEntity = new ItemEntity(event.getLevel(),
                    creeper.getX(), creeper.getY() + 0.5D, creeper.getZ(), eggs);
            event.getLevel().addFreshEntity(itemEntity);

            event.getLevel().playSound(null, creeper.blockPosition(), SoundEvents.SHEEP_SHEAR,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            heldItem.hurtAndBreak(1, event.getEntity(),
                    player -> player.broadcastBreakEvent(event.getHand()));

            creeper.setSwellDir(-2);
            removeCreeperExplosionGoal(creeper);
        }

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }

    private static void removeCreeperExplosionGoal(Creeper creeper) {
        creeper.goalSelector.getAvailableGoals().removeIf(goal ->
                goal.getGoal().getClass().getName().contains("SwellGoal")
        );
    }
}
