package com.example.zitraksmode.enchantment;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ModEnchantmentCategories {
    private ModEnchantmentCategories() {
    }

    public static final EnchantmentCategory DILDO = EnchantmentCategory.create(
            "dildo",
            item -> item == ModItems.MUD_DILDO.get()
                    || item == ModItems.STONE_DILDO.get()
                    || item == ModItems.IRON_DILDO.get()
                    || item == ModItems.GOLD_DILDO.get()
                    || item == ModItems.DIAMOND_DILDO.get()
                    || item == ModItems.EMERALD_DILDO.get()
                    || item == ModItems.OBSIDIAN_DILDO.get()
                    || item == ModItems.NETHERITE_DILDO.get()
                    || item == ModItems.TNT_DILDO.get()
    );

    public static final EnchantmentCategory SNIPER = EnchantmentCategory.create(
            "sniper",
            item -> item == ModItems.IRON_SNIPER.get()
                    || item == ModItems.GOLDEN_SNIPER.get()
                    || item == ModItems.DIAMOND_SNIPER.get()
                    || item == ModItems.NETHERITE_SNIPER.get()
    );

    public static final EnchantmentCategory TNT_ARMOR = EnchantmentCategory.create(
            "tnt_armor",
            item -> item == ModItems.ZITRAKS_HELMET.get()
                    || item == ModItems.ZITRAKS_CHESTPLATE.get()
                    || item == ModItems.ZITRAKS_LEGGINGS.get()
                    || item == ModItems.ZITRAKS_BOOTS.get()
    );

    public static final EnchantmentCategory MADE_IN_HEAVEN = EnchantmentCategory.create(
            "made_in_heaven",
            item -> item == ModItems.MADE_IN_HEAVEN.get()
    );

    public static final EnchantmentCategory PRINTER_3D = EnchantmentCategory.create(
            "printer_3d",
            item -> item == ModItems.PRINTER_3D_ITEM.get()
    );

    public static final EnchantmentCategory STEEL_BALL = EnchantmentCategory.create(
            "steel_ball",
            item -> item == ModItems.STEEL_BALL.get()
    );

    public static final EnchantmentCategory AQUALUNG = EnchantmentCategory.create(
            "aqualung",
            item -> item == ModItems.AQUALUNG.get()
    );

    public static final EnchantmentCategory MINER_HELMET = EnchantmentCategory.create(
            "miner_helmet",
            item -> item == ModItems.MINER_HELMET.get()
    );

    public static final EnchantmentCategory KATANA = EnchantmentCategory.create(
            "katana",
            item -> item == ModItems.KATANA.get()
    );

    public static final EnchantmentCategory VACUUM = EnchantmentCategory.create(
            "vacuum",
            item -> item == ModItems.VACUUM.get()
    );

    /** Only Medusa accepts the "Сила науки" enchantment. */
    public static final EnchantmentCategory MEDUSA = EnchantmentCategory.create(
            "medusa",
            item -> item == ModItems.MEDUSA.get()
    );
}
