package com.example.zitraksmode.printer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

public record BlockPlacement(BlockPos pos, BlockState state, Item requiredItem) {}