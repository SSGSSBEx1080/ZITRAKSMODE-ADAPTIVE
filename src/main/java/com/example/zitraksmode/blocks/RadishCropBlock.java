package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A normal eight-stage farmland crop; its edible radish item is also the seed. */
public final class RadishCropBlock extends CropBlock {
    public RadishCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected Item getBaseSeedId() {
        return ModItems.RADISH.get();
    }
}
