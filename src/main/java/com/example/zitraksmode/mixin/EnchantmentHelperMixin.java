package com.example.zitraksmode.mixin;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.events.EnchantingTableHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    
    private static final Logger LOGGER = LogManager.getLogger("ZitraksMode/EnchantmentHelper");
    private static final Random RANDOM = new Random();

    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
    private static void zitraksmode$addBombardier(
            int pLevel,
            ItemStack pStack,
            boolean pAllowTreasure,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        
        // ПРОВЕРКА: есть ли TNT
        int tntCount = EnchantingTableHandler.CURRENT_TNT_COUNT;
        
        if (tntCount <= 0) {
            return; // TNT нет — зачарование не добавляем
        }
        
        // ПРОВЕРКА: предмет должен быть ТНТ-бронёй ИЛИ книгой
        boolean isTntArmor = pStack.getItem() == ModItems.ZITRAKS_HELMET.get() ||
                             pStack.getItem() == ModItems.ZITRAKS_CHESTPLATE.get() ||
                             pStack.getItem() == ModItems.ZITRAKS_LEGGINGS.get() ||
                             pStack.getItem() == ModItems.ZITRAKS_BOOTS.get();
        
        boolean isBook = pStack.getItem() == Items.BOOK || 
                         pStack.getItem() == Items.ENCHANTED_BOOK;
        
        if (!isTntArmor && !isBook) {
            return;
        }
        
        // ШАНС: 10% за каждый TNT
        double chance = Math.min(0.9, tntCount * 0.10);
        if (RANDOM.nextDouble() >= chance) {
            return;
        }
        
        // ОПРЕДЕЛЯЕМ УРОВЕНЬ (pLevel — сколько опыта потрачено)
        int level = 1;
        if (pLevel >= 28) {
            level = 3;
        } else if (pLevel >= 19) {
            level = 2;
        }
        
        // ДОБАВЛЯЕМ зачарование в список
        List<EnchantmentInstance> list = cir.getReturnValue();
        Enchantment bombardier = ModEnchantments.BOMBARDIER.get();
        
        // Проверяем, нет ли уже
        boolean alreadyHas = list.stream().anyMatch(i -> i.enchantment == bombardier);
        if (!alreadyHas) {
            list.add(new EnchantmentInstance(bombardier, level));
            LOGGER.info("✅ Добавлено зачарование Бомбардир {} уровня (TNT={}, pLevel={})", 
                level, tntCount, pLevel);
        }
    }
}