package com.example.zitraksmode.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class CopperWoolBlock extends Block {

    public CopperWoolBlock() {
        super(BlockBehaviour.Properties.of(Material.WOOL, MaterialColor.COLOR_ORANGE)
                .strength(0.8F)
                .sound(SoundType.WOOL));
    }
}
