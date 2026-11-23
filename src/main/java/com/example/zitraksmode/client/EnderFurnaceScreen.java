package com.example.zitraksmode.client;

import com.example.zitraksmode.blocks.EnderFurnaceMenu;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Мы создаем свой экран, который умеет работать именно с EnderFurnaceMenu
public class EnderFurnaceScreen extends AbstractFurnaceScreen<EnderFurnaceMenu> {
    // Используем текстуру обычной печки
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/furnace.png");

    public EnderFurnaceScreen(EnderFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, new SmeltingRecipeBookComponent(), inventory, title, TEXTURE);
    }
}