package com.example.zitraksmode.items;

import com.example.zitraksmode.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TntRepairKitItem extends Item {
    
    public TntRepairKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };
        
        boolean hasFullTntSet = true;
        int repairedCount = 0;

        // Проверяем полный комплект ТНТ-брони и ремонтируем
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            
            // Проверка: каждый слот должен быть ТНТ-бронёй
            if (armor.getItem() != ModItems.ZITRAKS_HELMET.get() &&
                armor.getItem() != ModItems.ZITRAKS_CHESTPLATE.get() &&
                armor.getItem() != ModItems.ZITRAKS_LEGGINGS.get() &&
                armor.getItem() != ModItems.ZITRAKS_BOOTS.get()) {
                hasFullTntSet = false;
                break;
            }
            
            // Ремонтируем если есть повреждения
            if (armor.getDamageValue() > 0) {
                armor.setDamageValue(0);
                repairedCount++;
            }
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // Успешный ремонт
        if (hasFullTntSet && repairedCount > 0) {
            stack.shrink(1);
            
            // Взрывные частицы
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0.5, 0.5, 0.5, 0.0);
            
            // Огненные частицы
            serverLevel.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1.5, player.getZ(),
                15, 0.3, 0.3, 0.3, 0.1);
            
            // Звук
            level.playSound(null, player.blockPosition(),
                SoundEvents.TNT_PRIMED, SoundSource.PLAYERS,
                1.0f, 0.8f);
            
            player.displayClientMessage(
                Component.literal("§a⚡ ТНТ-броня полностью восстановлена!"), true);
            
            return InteractionResultHolder.success(stack);
        }
        
        // Нет полного комплекта
        if (!hasFullTntSet) {
            player.displayClientMessage(
                Component.literal("§cНадень полный комплект ТНТ-брони!"), true);
            return InteractionResultHolder.fail(stack);
        }
        
        // Броня уже целая
        player.displayClientMessage(
            Component.literal("§cТНТ-броня уже полностью исправна!"), true);
        return InteractionResultHolder.pass(stack);
    }
}