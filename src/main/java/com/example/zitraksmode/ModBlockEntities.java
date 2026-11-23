package com.example.zitraksmode;

import com.example.zitraksmode.blockentities.DildoBlockEntity;
import com.example.zitraksmode.blockentities.HentaiBlockEntity;
import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.blocks.EnderFurnaceBlockEntity;
import com.example.zitraksmode.blocks.EnderSafeBlockEntity;
import com.example.zitraksmode.blocks.EnderSecurityBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ZitraksMode.MODID);

    public static final RegistryObject<BlockEntityType<EnderSafeBlockEntity>> ENDER_SAFE =
            BLOCK_ENTITIES.register("ender_safe", () ->
                    BlockEntityType.Builder.of(EnderSafeBlockEntity::new,
                            ModBlocks.ENDER_SAFE.get()).build(null));

    public static final RegistryObject<BlockEntityType<EnderFurnaceBlockEntity>> ENDER_FURNACE =
            BLOCK_ENTITIES.register("ender_furnace",
                    () -> BlockEntityType.Builder.of(EnderFurnaceBlockEntity::new,
                            ModBlocks.ENDER_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<Printer3DBlockEntity>> PRINTER_3D =
            BLOCK_ENTITIES.register("printer_3d", () ->
                    BlockEntityType.Builder.of(Printer3DBlockEntity::new, ModBlocks.PRINTER_3D.get()).build(null));

    public static final RegistryObject<BlockEntityType<EnderSecurityBlockEntity>> ENDER_SECURITY =
            BLOCK_ENTITIES.register("ender_security",
                    () -> BlockEntityType.Builder.of(EnderSecurityBlockEntity::new,
                            ModBlocks.ENDER_BUTTON.get(),
                            ModBlocks.ENDER_PRESSURE_PLATE.get(),
                            ModBlocks.ENDER_TRAPDOOR.get(),
                            ModBlocks.ENDER_DOOR.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<HentaiBlockEntity>> HENTAI_BLOCK =
            BLOCK_ENTITIES.register("hentai_block",
                    () -> BlockEntityType.Builder.of(HentaiBlockEntity::new, ModBlocks.HENTAI_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<DildoBlockEntity>> DILDO_BLOCK =
            BLOCK_ENTITIES.register("dildo_block",
                    () -> BlockEntityType.Builder.of(DildoBlockEntity::new,
                            ModBlocks.MUD_DILDO.get(),
                            ModBlocks.STONE_DILDO.get(),
                            ModBlocks.IRON_DILDO.get(),
                            ModBlocks.GOLD_DILDO.get(),
                            ModBlocks.DIAMOND_DILDO.get(),
                            ModBlocks.EMERALD_DILDO.get(),
                            ModBlocks.OBSIDIAN_DILDO.get(),
                            ModBlocks.NETHERITE_DILDO.get(),
                            ModBlocks.TNT_DILDO.get()
                    ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}