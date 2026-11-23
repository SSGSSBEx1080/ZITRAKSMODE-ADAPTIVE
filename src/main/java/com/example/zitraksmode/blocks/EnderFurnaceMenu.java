package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

public class EnderFurnaceMenu extends AbstractFurnaceMenu {

    // Конструктор для клиента
    public EnderFurnaceMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(ModMenuTypes.ENDER_FURNACE_MENU.get(), RecipeType.SMELTING, RecipeBookType.FURNACE, id, playerInventory);
    }

    // Конструктор для сервера
    public EnderFurnaceMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.ENDER_FURNACE_MENU.get(), RecipeType.SMELTING, RecipeBookType.FURNACE, id, playerInventory, container, data);
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        return stack.is(Items.ENDER_PEARL) || stack.is(Items.ENDER_EYE);
    }
}