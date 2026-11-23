package com.example.zitraksmode.items;

import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;

/** The raw radish is both food and the item used to plant a radish crop. */
public final class RadishItem extends ItemNameBlockItem {
    public RadishItem(Block cropBlock, Properties properties) {
        super(cropBlock, properties);
    }
}
