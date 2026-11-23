package com.example.zitraksmode.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.example.zitraksmode.entities.BulletEntity;

public class BulletItem extends ArrowItem {
    
    public BulletItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        BulletEntity bullet = new BulletEntity(level, shooter);
        
        // Настраиваем базовые параметры пули
        // bullet.setEffectsFromItem(stack); // УБРАТЬ ЭТУ СТРОКУ
        bullet.setBaseDamage(4.0); // Базовый урон
        
        return bullet;
    }
    
    @Override
    public boolean isInfinite(ItemStack stack, ItemStack bow, net.minecraft.world.entity.player.Player player) {
        return false; // Пули не бесконечные
    }
}