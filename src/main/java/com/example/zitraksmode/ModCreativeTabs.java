package com.example.zitraksmode;

import com.example.zitraksmode.items.LabubuType;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;

public class ModCreativeTabs {
    public static final CreativeModeTab ZITRAKS_TAB = new CreativeModeTab("zitraksmode") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.ONE_BLOCK_ITEM.get());
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            items.add(new ItemStack(ModItems.ONE_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.TWO_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.THREE_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.FOUR_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.FIVE_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.SIX_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.SEVEN_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.EIGHT_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.NINE_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.ZERO_BLOCK_ITEM.get()));
            items.add(new ItemStack(ModItems.ZITRAKS_HELMET.get()));
            items.add(new ItemStack(ModItems.ZITRAKS_CHESTPLATE.get()));
            items.add(new ItemStack(ModItems.ZITRAKS_LEGGINGS.get()));
            items.add(new ItemStack(ModItems.ZITRAKS_BOOTS.get()));
            items.add(new ItemStack(ModItems.TNT_REPAIR_KIT.get()));
            items.add(new ItemStack(ModItems.BULLET.get()));
            items.add(new ItemStack(ModItems.IRON_SNIPER.get()));
            items.add(new ItemStack(ModItems.GOLDEN_SNIPER.get()));
            items.add(new ItemStack(ModItems.DIAMOND_SNIPER.get()));
            items.add(new ItemStack(ModItems.NETHERITE_SNIPER.get()));
            items.add(new ItemStack(ModItems.MINER_HELMET.get()));
            items.add(new ItemStack(ModItems.ADUN_VILLAGER_SPAWN_EGG.get()));
            items.add(new ItemStack(ModItems.LABUBU_BOX_ITEM.get()));

            List<String> orderedLabubuKeys = List.of(
                    "red", "blue", "yellow", "white", "black", "green",
                    "purple", "black_white", "striped",
                    "kazakh", "ukrainian", "japanese", "top_hat", "faseless",
                    "russian", "neon", "litvin", "anonymous", "demon", "angel",
                    "galactic", "rainbow", "golden", "buddha", "booba", "anime"
            );

            for (String key : orderedLabubuKeys) {
                try {
                    LabubuType type = LabubuType.valueOf(key.toUpperCase());
                    var item = ModItems.LABUBU_FIGURINES.get(type);
                    if (item != null) {
                        items.add(new ItemStack(item.get()));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            items.add(new ItemStack(ModItems.REINFORCED_ENDER_GLASS_ITEM.get()));
            items.add(new ItemStack(ModItems.ENDER_FURNACE_ITEM.get()));
            items.add(new ItemStack(ModItems.ENDER_SAFE_ITEM.get()));
            items.add(new ItemStack(ModItems.ENDER_DOOR_ITEM.get()));
            items.add(new ItemStack(ModItems.ENDER_TRAPDOOR_ITEM.get()));
            items.add(new ItemStack(ModItems.ENDER_BUTTON_ITEM.get()));
            items.add(new ItemStack(ModItems.ENDER_PRESSURE_PLATE_ITEM.get()));
            items.add(new ItemStack(ModItems.MADE_IN_HEAVEN.get()));
            items.add(new ItemStack(ModItems.MUD_DILDO.get()));
            items.add(new ItemStack(ModItems.STONE_DILDO.get()));
            items.add(new ItemStack(ModItems.IRON_DILDO.get()));
            items.add(new ItemStack(ModItems.GOLD_DILDO.get()));
            items.add(new ItemStack(ModItems.DIAMOND_DILDO.get()));
            items.add(new ItemStack(ModItems.EMERALD_DILDO.get()));
            items.add(new ItemStack(ModItems.OBSIDIAN_DILDO.get()));
            items.add(new ItemStack(ModItems.NETHERITE_DILDO.get()));
            items.add(new ItemStack(ModItems.TNT_DILDO.get()));
            items.add(new ItemStack(ModItems.CREEPER_EGGS.get()));
            items.add(new ItemStack(ModItems.COOKED_CREEPER_EGGS.get()));
            items.add(new ItemStack(ModItems.CHARGED_CREEPER_EGGS.get()));
            items.add(new ItemStack(ModItems.CREEPER_STATUE.get()));
            items.add(new ItemStack(ModItems.SUPER_TNT.get()));
            items.add(new ItemStack(ModItems.SUPER_TNT_MINECART.get()));
            items.add(new ItemStack(ModItems.JBL_SPEAKER.get()));
            items.add(new ItemStack(ModItems.KATANA.get()));
            items.add(new ItemStack(ModItems.AQUALUNG.get()));
            items.add(new ItemStack(ModItems.PRINTER_3D_ITEM.get()));
            items.add(new ItemStack(ModItems.HENTAI_BLOCK.get()));
            items.add(new ItemStack(ModItems.VETKHIY_MINET.get()));
            items.add(new ItemStack(ModItems.NOVIY_OMLET.get()));
            items.add(new ItemStack(ModItems.KORAN.get()));
            items.add(new ItemStack(ModItems.STEEL_BALL.get()));
            items.add(new ItemStack(ModItems.DEATH_NOTE.get()));
            items.add(new ItemStack(ModItems.DEATH_NOTE_REQUIEM.get()));
            items.add(new ItemStack(ModItems.GAZAN_SPAWN_EGG.get()));
            items.add(new ItemStack(ModItems.MAHORAGA_SCROLL.get()));
            items.add(new ItemStack(ModItems.SPERM_BUCKET.get()));
            items.add(new ItemStack(ModItems.COPPER_WOOL.get()));
            items.add(new ItemStack(ModItems.SPANISH_BOOTS.get()));
            items.add(new ItemStack(ModItems.MAX_MESSENGER.get()));
            items.add(new ItemStack(ModItems.SCOOTER.get()));
            items.add(new ItemStack(ModItems.CHARGING_PORT.get()));
            items.add(new ItemStack(ModItems.CLOAK_CROCODILE.get()));
            items.add(new ItemStack(ModItems.CLOAK_AKAINU.get()));
            items.add(new ItemStack(ModItems.CLOAK_AOKIJI.get()));
            items.add(new ItemStack(ModItems.CLOAK_KIZARU.get()));
            items.add(new ItemStack(ModItems.CLOAK_SENGOKU.get()));
            items.add(new ItemStack(ModItems.CLOAK_WHITEBEARD.get()));
            items.add(new ItemStack(ModItems.VACUUM.get()));
            items.add(new ItemStack(ModItems.ANTON_CHIGUR_SPAWN_EGG.get()));
            items.add(new ItemStack(ModItems.CHIGUR_SHOTGUN.get()));
            items.add(new ItemStack(ModItems.CHIGUR_COIN.get()));
            items.add(new ItemStack(ModItems.GOST_DOCTORSKAYA_SAUSAGE.get()));
            items.add(new ItemStack(ModItems.SALT.get()));
            items.add(new ItemStack(ModItems.SALT_ORE_ITEM.get()));
            items.add(new ItemStack(ModItems.RADISH.get()));
            items.add(new ItemStack(ModItems.FRIED_RADISH.get()));
            items.add(new ItemStack(ModItems.BORSCHT.get()));
            items.add(new ItemStack(ModItems.NINA_KNIFE.get()));
            items.add(new ItemStack(ModItems.MEDUSA.get()));
            items.add(new ItemStack(ModItems.IMMUNOSUPPRESSANT.get()));
            items.add(new ItemStack(ModItems.SANDEVISTAN.get()));

            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BOMBARDIER.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BOMBARDIER.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BOMBARDIER.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.PEACEMAKER.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.PEACEMAKER.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.PEACEMAKER.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.HOW_HE_PIERCED_ME.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.DOUBLE_PENETRATION.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.NIMBLE_FINGERS.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.NIMBLE_FINGERS.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.NIMBLE_FINGERS.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.PERESVET.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.PERESVET.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.PERESVET.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.WORLD_SLASH.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.WORLD_SLASH.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.WORLD_SLASH.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.WORLD_SLASH.get(), 4)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.WORLD_SLASH.get(), 5)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.MINING_INDUSTRY.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.VODOHLEBUS.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.ZHIRNY_PIDOR.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BARYGA.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BARYGA.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.NAHUYA.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.ZAEBIS.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BALL_BREAKER.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.MORICHEK.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.BED_WARS.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.YOBYR_MAMOK.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.HOCHU_LABUBU.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.AIRUHAN.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.AIRUHAN.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.AIRUHAN.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.SCIENCE_POWER.get(), 1)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.SCIENCE_POWER.get(), 2)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.SCIENCE_POWER.get(), 3)));
            items.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.SCIENCE_POWER.get(), 4)));
        }
    };
}
