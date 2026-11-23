package com.example.zitraksmode;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.PressurePlateBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.example.zitraksmode.blocks.*;
import com.example.zitraksmode.items.LabubuType;
import com.example.zitraksmode.items.DildoMaterial;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ZitraksMode.MODID);
    
    public static final List<RegistryObject<Block>> NUMBER_BLOCKS = new ArrayList<>();
    public static final Map<LabubuType, RegistryObject<Block>> LABUBU_FIGURINE_BLOCKS = new HashMap<>();

    

    // === НОВЫЕ ЭНДЕР-БЛОКИ ===

    public static final RegistryObject<Block> REINFORCED_ENDER_GLASS = BLOCKS.register("reinforced_ender_glass",
        () -> new GlassBlock(BlockBehaviour.Properties.of(Material.GLASS)
            .strength(25.0f, 1200.0f)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ENDER_FURNACE = BLOCKS.register("ender_furnace",
        () -> new EnderFurnaceBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(26.75f, 1200.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(EnderFurnaceBlock.LIT) ? 13 : 0)));

    public static final RegistryObject<Block> ENDER_SAFE = BLOCKS.register("ender_safe",
            () -> new EnderSafeBlock(BlockBehaviour.Properties.of(Material.STONE)
                .strength(36.25f, 600.0f) // Крепкий как обсидиан
                .sound(SoundType.STONE)));

    // Добавляем .noOcclusion() - это критически важно для дверей и прозрачных блоков!
    public static final RegistryObject<Block> ENDER_DOOR = BLOCKS.register("ender_door",
        () -> new EnderDoorBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(27.5f, 1000.0f)
            .sound(SoundType.METAL)
            .noOcclusion() // Делает блок прозрачным для движка отрисовки (фиксит невидимость)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ENDER_TRAPDOOR = BLOCKS.register("ender_trapdoor",
        () -> new EnderTrapdoorBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(27.5f, 1000.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ENDER_BUTTON = BLOCKS.register("ender_button",
        () -> new EnderButtonBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(25.25f, 1000.0f)
            .noCollission()
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ENDER_PRESSURE_PLATE = BLOCKS.register("ender_pressure_plate",
        () -> new EnderPressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, 
            BlockBehaviour.Properties.of(Material.METAL).strength(25.25f, 1000.0f).noCollission().requiresCorrectToolForDrops()));;

    public static final RegistryObject<Block> COPPER_WOOL = BLOCKS.register("copper_wool", CopperWoolBlock::new);


    // === СУЩЕСТВУЮЩИЕ БЛОКИ ===
    public static final RegistryObject<Block> ONE = registerNumberBlock("one");
    public static final RegistryObject<Block> TWO = registerNumberBlock("two");
    public static final RegistryObject<Block> THREE = registerNumberBlock("three");
    public static final RegistryObject<Block> FOUR = registerNumberBlock("four");
    public static final RegistryObject<Block> FIVE = registerNumberBlock("five");
    public static final RegistryObject<Block> SIX = registerNumberBlock("six");
    public static final RegistryObject<Block> SEVEN = registerNumberBlock("seven");
    public static final RegistryObject<Block> EIGHT = registerNumberBlock("eight");
    public static final RegistryObject<Block> NINE = registerNumberBlock("nine");
    public static final RegistryObject<Block> ZERO = registerNumberBlock("zero");

        // ===== DILDO BLOCKS =====

    public static final RegistryObject<Block> MUD_DILDO = BLOCKS.register("mud_dildo", 
        () -> new DildoBlock(DildoMaterial.MUD));

    public static final RegistryObject<Block> STONE_DILDO = BLOCKS.register("stone_dildo", 
        () -> new DildoBlock(DildoMaterial.STONE));

    public static final RegistryObject<Block> IRON_DILDO = BLOCKS.register("iron_dildo", 
        () -> new DildoBlock(DildoMaterial.IRON));

    public static final RegistryObject<Block> GOLD_DILDO = BLOCKS.register("gold_dildo", 
        () -> new DildoBlock(DildoMaterial.GOLD));

    public static final RegistryObject<Block> DIAMOND_DILDO = BLOCKS.register("diamond_dildo", 
        () -> new DildoBlock(DildoMaterial.DIAMOND));

    public static final RegistryObject<Block> EMERALD_DILDO = BLOCKS.register("emerald_dildo", 
        () -> new DildoBlock(DildoMaterial.EMERALD));

    public static final RegistryObject<Block> OBSIDIAN_DILDO = BLOCKS.register("obsidian_dildo", 
        () -> new DildoBlock(DildoMaterial.OBSIDIAN));

    public static final RegistryObject<Block> NETHERITE_DILDO = BLOCKS.register("netherite_dildo", 
        () -> new DildoBlock(DildoMaterial.NETHERITE));

    public static final RegistryObject<Block> TNT_DILDO = BLOCKS.register("tnt_dildo", 
        () -> new TNTDildoBlock());


    public static final RegistryObject<Block> LABUBU_BOX = BLOCKS.register("labubu_box", 
        () -> new LabubuBoxBlock(BlockBehaviour.Properties.of(Material.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .requiresCorrectToolForDrops()));

    // ===== CREEPER CASTRATION BLOCKS =====

    // 1. Статуя крипера (2 блока высотой)
   public static final RegistryObject<Block> CREEPER_STATUE = BLOCKS.register("creeper_statue",
    () -> new CreeperStatueBlock(BlockBehaviour.Properties.of(Material.STONE)
        .strength(2.0f)
        .sound(SoundType.STONE)
        .noOcclusion()));

    // 2. Супер TNT
    public static final RegistryObject<Block> SUPER_TNT = BLOCKS.register("super_tnt",
        () -> new SuperTNTBlock());

    static {
        for (LabubuType type : LabubuType.values()) {
            String blockName = "labubu_figurine_" + type.name;
            RegistryObject<Block> figurineBlock = BLOCKS.register(blockName,
                () -> new LabubuFigurineBlock(BlockBehaviour.Properties.of(Material.DECORATION)
                    .strength(0.8f, 1.0f)
                    .sound(SoundType.GLASS)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));
            LABUBU_FIGURINE_BLOCKS.put(type, figurineBlock);
        }
    }
                // ===== 3D ПРИНТЕР (Пункт №13) =====
    public static final RegistryObject<Block> PRINTER_3D = BLOCKS.register("printer_3d",
        () -> new Printer3DBlock(BlockBehaviour.Properties.of(Material.METAL)
            .strength(3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> HENTAI_BLOCK = BLOCKS.register("hentai_block",
        () -> new HentaiBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(1.5f)
            .noOcclusion() 
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE)));

    private static RegistryObject<Block> registerNumberBlock(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, 
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                .strength(2.9f, 3.4f)
                .sound(SoundType.STONE)
            ));
        NUMBER_BLOCKS.add(block);
        return block;
    }

    public static final RegistryObject<Block> CHARGING_PORT = BLOCKS.register("charging_port", ChargingPortBlock::new);
    
    // === ГОСТЕВОЙ ПУНКТ ОТ ПРАБАБУШКИ НИНЫ ===
    public static final RegistryObject<Block> SALT_ORE = BLOCKS.register(
            "salt_ore",
            () -> new SaltOreBlock(
                    BlockBehaviour.Properties.of(Material.STONE)
                            .strength(3.0F, 3.0F)
                            .sound(SoundType.STONE)
            )
    );

    public static final RegistryObject<Block> SALT_TRAIL = BLOCKS.register(
            "salt_trail",
            () -> new SaltTrailBlock(BlockBehaviour.Properties.of(Material.DECORATION)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.SAND))
    );

    public static final RegistryObject<Block> RADISH_CROP = BLOCKS.register(
            "radish_crop",
            () -> new RadishCropBlock(BlockBehaviour.Properties.of(Material.PLANT)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()
                    .sound(SoundType.CROP)
                    .instabreak())
    );
    

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}