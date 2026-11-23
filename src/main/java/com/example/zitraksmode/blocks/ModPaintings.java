package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPaintings {

    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
            DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, ZitraksMode.MODID);

    // 2x1
    public static final RegistryObject<PaintingVariant> HOLDIK = PAINTING_VARIANTS.register("holdik",
            () -> new PaintingVariant(32, 16));

    public static final RegistryObject<PaintingVariant> POZOR = PAINTING_VARIANTS.register("pozor",
            () -> new PaintingVariant(32, 16));

    // 1x1
    public static final RegistryObject<PaintingVariant> CAT = PAINTING_VARIANTS.register("cat",
            () -> new PaintingVariant(16, 16));

    public static final RegistryObject<PaintingVariant> HOMELANDER = PAINTING_VARIANTS.register("homelander",
            () -> new PaintingVariant(16, 16));

    public static final RegistryObject<PaintingVariant> MCLOVIN = PAINTING_VARIANTS.register("mclovin",
            () -> new PaintingVariant(16, 16));

    public static final RegistryObject<PaintingVariant> CHERV = PAINTING_VARIANTS.register("small_cherv",
            () -> new PaintingVariant(16, 16));

    // 2x2
    public static final RegistryObject<PaintingVariant> DAGESTAN = PAINTING_VARIANTS.register("dagestan",
            () -> new PaintingVariant(32, 32));

    public static final RegistryObject<PaintingVariant> SMILE = PAINTING_VARIANTS.register("smile",
            () -> new PaintingVariant(32, 32));

    public static final RegistryObject<PaintingVariant> BELKA = PAINTING_VARIANTS.register("belka",
            () -> new PaintingVariant(32, 32));

    // 1x2 (высокая)
    public static final RegistryObject<PaintingVariant> SHLIKOV = PAINTING_VARIANTS.register("shlikov",
            () -> new PaintingVariant(16, 32));

    public static final RegistryObject<PaintingVariant> DYRALEI = PAINTING_VARIANTS.register("dyralei",
            () -> new PaintingVariant(16, 32));

    // 2x1 (горизонтальная)
    public static final RegistryObject<PaintingVariant> TURNIK = PAINTING_VARIANTS.register("turnik",
            () -> new PaintingVariant(32, 16));

    // 3x2
    public static final RegistryObject<PaintingVariant> CLIMBER = PAINTING_VARIANTS.register("climber",
            () -> new PaintingVariant(48, 32));

    // 4x3
    public static final RegistryObject<PaintingVariant> BOOBS = PAINTING_VARIANTS.register("boobs",
            () -> new PaintingVariant(64, 48));

    public static final RegistryObject<PaintingVariant> BOYS = PAINTING_VARIANTS.register("20th_century_boys",
            () -> new PaintingVariant(64, 48));

    public static final RegistryObject<PaintingVariant> FROG = PAINTING_VARIANTS.register("frog",
            () -> new PaintingVariant(64, 48));

    // 4x4
    public static final RegistryObject<PaintingVariant> VAGABOND = PAINTING_VARIANTS.register("vagabond",
            () -> new PaintingVariant(64, 64));

    public static final RegistryObject<PaintingVariant> ZLOBA = PAINTING_VARIANTS.register("zloba",
            () -> new PaintingVariant(64, 64));

        public static final RegistryObject<PaintingVariant> EPIC = PAINTING_VARIANTS.register("epic",
            () -> new PaintingVariant(256, 256));

    public static void register(IEventBus bus) {
        PAINTING_VARIANTS.register(bus);
    }
}