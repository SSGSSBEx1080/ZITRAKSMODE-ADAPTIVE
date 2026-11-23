package com.example.zitraksmode.villager;

import com.example.zitraksmode.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "zitraksmode")
public class DemolitionistTradeRegistration {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.DEMOLITIONIST.get()) {
            return;
        }

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

        // ================================================================
        // УРОВЕНЬ 1 — НОВИЧОК
        // ================================================================
        trades.get(1).add(new DemolitionistTrades.ItemsForItems(
                // Игрок даёт: 10 песка + 3 изумруда → получает 4 ТНТ
                new ItemStack(Items.SAND, 10),
                new ItemStack(Items.EMERALD, 3),
                new ItemStack(Items.TNT, 4),
                16, 2
        ));

        trades.get(1).add(new DemolitionistTrades.ItemsForItems(
                // Игрок даёт 12 пороха → получает 1 изумруд
                new ItemStack(Items.GUNPOWDER, 12),
                new ItemStack(Items.EMERALD, 1),
                16, 2
        ));

        trades.get(1).add(new DemolitionistTrades.ItemsForItems(
                // Игрок даёт 8 редстоуна → получает 1 изумруд
                new ItemStack(Items.REDSTONE, 8),
                new ItemStack(Items.EMERALD, 1),
                16, 2
        ));

        trades.get(1).add(new DemolitionistTrades.ItemsForItems(
                // Продажа огнива за 3 изумруда
                new ItemStack(Items.EMERALD, 3),
                new ItemStack(Items.FLINT_AND_STEEL, 1),
                12, 3
        ));

        // ================================================================
        // УРОВЕНЬ 2 — ПОДМАСТЕРЬЕ
        // ================================================================
        trades.get(2).add(new DemolitionistTrades.ItemsForItems(
                // Игрок даёт 16 железа → получает 1 изумруд
                new ItemStack(Items.IRON_INGOT, 16),
                new ItemStack(Items.EMERALD, 1),
                12, 5
        ));

        trades.get(2).add(new DemolitionistTrades.ItemsForItems(
                // Игрок даёт 4 фейерверка → получает 1 изумруд
                new ItemStack(Items.FIREWORK_ROCKET, 4),
                new ItemStack(Items.EMERALD, 1),
                12, 5
        ));

        trades.get(2).add(new DemolitionistTrades.ItemsForItems(
                // Продажа 8 ТНТ за 5 изумрудов
                new ItemStack(Items.EMERALD, 5),
                new ItemStack(Items.TNT, 8),
                8, 8
        ));

        // Случайная кнопка
        trades.get(2).add(new DemolitionistTrades.RandomButtonTrade());

        // Случайная нажимная плита
        trades.get(2).add(new DemolitionistTrades.RandomPressurePlateTrade());

        // ================================================================
        // УРОВЕНЬ 3 — МАСТЕР
        // ================================================================
        trades.get(3).add(new DemolitionistTrades.ItemsForItems(
                // Покупка: 24 редстоуна → 2 изумруда
                new ItemStack(Items.REDSTONE, 24),
                new ItemStack(Items.EMERALD, 2),
                12, 10
        ));

        trades.get(3).add(new DemolitionistTrades.ItemsForItems(
                // Продажа: рычаг за 1 изумруд
                new ItemStack(Items.EMERALD, 1),
                new ItemStack(Items.LEVER, 4),
                12, 8
        ));

        trades.get(3).add(new DemolitionistTrades.ItemsForItems(
                // Продажа: 4 редстоун-факела за 2 изумруда
                new ItemStack(Items.EMERALD, 2),
                new ItemStack(Items.REDSTONE_TORCH, 4),
                12, 8
        ));

        trades.get(3).add(new DemolitionistTrades.ItemsForItems(
                // Продажа: 1 редстоун-блок за 4 изумруда
                new ItemStack(Items.EMERALD, 4),
                new ItemStack(Items.REDSTONE_BLOCK, 1),
                8, 10
        ));

        trades.get(3).add(new DemolitionistTrades.ItemsForItems(
                // Продажа: Ремонтный ТНТ набор за 12 изумрудов
                new ItemStack(Items.EMERALD, 12),
                new ItemStack(ModItems.TNT_REPAIR_KIT.get(), 1),
                4, 15
        ));

        // ================================================================
        // УРОВЕНЬ 4 — ЭКСПЕРТ
        // ================================================================
        trades.get(4).add(new DemolitionistTrades.ItemsForItems(
                // Шлем за 32 изумруда
                new ItemStack(Items.EMERALD, 32),
                new ItemStack(ModItems.ZITRAKS_HELMET.get(), 1),
                2, 25
        ));

        trades.get(4).add(new DemolitionistTrades.ItemsForItems(
                // Нагрудник за 32 изумруда
                new ItemStack(Items.EMERALD, 32),
                new ItemStack(ModItems.ZITRAKS_CHESTPLATE.get(), 1),
                2, 25
        ));

        trades.get(4).add(new DemolitionistTrades.ItemsForItems(
                // Поножи за 28 изумрудов
                new ItemStack(Items.EMERALD, 28),
                new ItemStack(ModItems.ZITRAKS_LEGGINGS.get(), 1),
                2, 25
        ));

        trades.get(4).add(new DemolitionistTrades.ItemsForItems(
                // Ботинки за 24 изумруда
                new ItemStack(Items.EMERALD, 24),
                new ItemStack(ModItems.ZITRAKS_BOOTS.get(), 1),
                2, 25
        ));

        // Книга Бомбардир I
        trades.get(4).add(new DemolitionistTrades.BombardierBookTrade(1, 10));

        // Книга Бомбардир II
        trades.get(4).add(new DemolitionistTrades.BombardierBookTrade(2, 20));

        // ================================================================
        // УРОВЕНЬ 5 — ГРАНДМАСТЕР
        // ================================================================
        // Книга Бомбардир III
        trades.get(5).add(new DemolitionistTrades.BombardierBookTrade(3, 35));

        // Случайная зачарованная броня (Бомбардир I-II) — дорогая
        trades.get(5).add(new DemolitionistTrades.RandomEnchantedArmorTrade());

        // Супер-ТНТ (редкий, шансовый)
        trades.get(5).add(new DemolitionistTrades.SuperTNTTrade());
    }
}