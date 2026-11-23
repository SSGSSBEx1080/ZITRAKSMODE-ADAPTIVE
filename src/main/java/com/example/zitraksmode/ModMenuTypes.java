package com.example.zitraksmode;

import com.example.zitraksmode.blocks.EnderFurnaceMenu;
import com.example.zitraksmode.blocks.EnderSafeMenu;
import com.example.zitraksmode.printer.Printer3DMenu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, ZitraksMode.MODID);

    // Регистрируем наше меню для Эндер-печи
    public static final RegistryObject<MenuType<EnderFurnaceMenu>> ENDER_FURNACE_MENU = 
        registerMenuType("ender_furnace_menu", EnderFurnaceMenu::new);

    public static final RegistryObject<MenuType<EnderSafeMenu>> ENDER_SAFE_MENU = 
        registerMenuType("ender_safe_menu", EnderSafeMenu::new);
        
    public static final RegistryObject<MenuType<Printer3DMenu>> PRINTER_3D_MENU = 
        MENUS.register("printer_3d", () -> 
            IForgeMenuType.create(Printer3DMenu::new));
        
    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}