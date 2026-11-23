package com.example.zitraksmode.villager;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ModEnchantments;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.List;

public class DemolitionistTrades {

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ДЛЯ ТРЕЙДОВ
    // ============================================================

    /**
     * Игрок ПОКУПАЕТ itemB за itemA (игрок даёт itemA, получает itemB)
     */
    public static class ItemsForItems implements VillagerTrades.ItemListing {
        private final ItemStack costA;
        private final ItemStack costB; // может быть пустым
        private final ItemStack result;
        private final int maxUses;
        private final int xp;
        private final float priceMultiplier;

        public ItemsForItems(ItemStack costA, ItemStack result, int maxUses, int xp) {
            this(costA, ItemStack.EMPTY, result, maxUses, xp, 0.05f);
        }

        public ItemsForItems(ItemStack costA, ItemStack costB, ItemStack result, int maxUses, int xp) {
            this(costA, costB, result, maxUses, xp, 0.05f);
        }

        public ItemsForItems(ItemStack costA, ItemStack costB, ItemStack result,
                             int maxUses, int xp, float priceMultiplier) {
            this.costA = costA;
            this.costB = costB;
            this.result = result;
            this.maxUses = maxUses;
            this.xp = xp;
            this.priceMultiplier = priceMultiplier;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(costA.copy(), costB.copy(), result.copy(),
                    maxUses, xp, priceMultiplier);
        }
    }

    /**
     * Случайная кнопка
     */
    public static class RandomButtonTrade implements VillagerTrades.ItemListing {
        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            ItemStack[] buttons = {
                    new ItemStack(Items.OAK_BUTTON),
                    new ItemStack(Items.SPRUCE_BUTTON),
                    new ItemStack(Items.BIRCH_BUTTON),
                    new ItemStack(Items.JUNGLE_BUTTON),
                    new ItemStack(Items.ACACIA_BUTTON),
                    new ItemStack(Items.DARK_OAK_BUTTON),
                    new ItemStack(Items.MANGROVE_BUTTON),
                    new ItemStack(Items.STONE_BUTTON),
                    new ItemStack(Items.POLISHED_BLACKSTONE_BUTTON),
                    new ItemStack(Items.CRIMSON_BUTTON),
                    new ItemStack(Items.WARPED_BUTTON)
            };
            ItemStack button = buttons[random.nextInt(buttons.length)].copy();
            button.setCount(4);
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    button,
                    12, 5, 0.05f
            );
        }
    }

    /**
     * Случайная нажимная плита
     */
    public static class RandomPressurePlateTrade implements VillagerTrades.ItemListing {
        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            ItemStack[] plates = {
                    new ItemStack(Items.OAK_PRESSURE_PLATE),
                    new ItemStack(Items.SPRUCE_PRESSURE_PLATE),
                    new ItemStack(Items.BIRCH_PRESSURE_PLATE),
                    new ItemStack(Items.JUNGLE_PRESSURE_PLATE),
                    new ItemStack(Items.ACACIA_PRESSURE_PLATE),
                    new ItemStack(Items.DARK_OAK_PRESSURE_PLATE),
                    new ItemStack(Items.STONE_PRESSURE_PLATE),
                    new ItemStack(Items.POLISHED_BLACKSTONE_PRESSURE_PLATE),
                    new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE),
                    new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE),
                    new ItemStack(Items.CRIMSON_PRESSURE_PLATE),
                    new ItemStack(Items.WARPED_PRESSURE_PLATE)
            };
            ItemStack plate = plates[random.nextInt(plates.length)].copy();
            plate.setCount(2);
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    plate,
                    12, 5, 0.05f
            );
        }
    }

    /**
     * Зачарованная книга Бомбардир
     */
    public static class BombardierBookTrade implements VillagerTrades.ItemListing {
        private final int level;
        private final int emeraldCost;

        public BombardierBookTrade(int level, int emeraldCost) {
            this.level = level;
            this.emeraldCost = emeraldCost;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(book,
                    new EnchantmentInstance(ModEnchantments.BOMBARDIER.get(), level));
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    book,
                    3, 30, 0.2f
            );
        }
    }

    /**
     * Случайный элемент зачарованной ТНТ-брони (Бомбардир I или II)
     */
    public static class RandomEnchantedArmorTrade implements VillagerTrades.ItemListing {
        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            // Случайный элемент брони
            ItemStack[] armorPieces = {
                    new ItemStack(ModItems.ZITRAKS_HELMET.get()),
                    new ItemStack(ModItems.ZITRAKS_CHESTPLATE.get()),
                    new ItemStack(ModItems.ZITRAKS_LEGGINGS.get()),
                    new ItemStack(ModItems.ZITRAKS_BOOTS.get())
            };
            ItemStack armor = armorPieces[random.nextInt(armorPieces.length)].copy();

            // Случайный уровень зачарования I или II
            int enchLevel = random.nextInt(2) + 1; // 1 или 2
            armor.enchant(ModEnchantments.BOMBARDIER.get(), enchLevel);

            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, 45),
                    new ItemStack(Items.TNT, 16), // Второй слот оплаты
                    armor,
                    1, 40, 0.2f // Только 1 использование!
            );
        }
    }

    /**
     * Супер-ТНТ (редкий трейд)
     */
    public static class SuperTNTTrade implements VillagerTrades.ItemListing {
    @Nullable
    @Override
    public MerchantOffer getOffer(Entity trader, RandomSource random) {
        // 50% шанс что трейд вообще появится
        if (random.nextFloat() > 0.5f) {
            return null;
        }
        // 1-5 обычных ТНТ + 64 изумруда → 1 Супер-ТНТ
        int tntCount = random.nextIntBetweenInclusive(1, 5);
        return new MerchantOffer(
                new ItemStack(Items.EMERALD, 64),
                new ItemStack(Items.TNT, tntCount),
                new ItemStack(ModItems.SUPER_TNT.get()),
                1, 50, 0.2f
        );
    }
}
}