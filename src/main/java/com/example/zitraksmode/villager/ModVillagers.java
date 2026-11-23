package com.example.zitraksmode.villager;

import com.example.zitraksmode.ZitraksMode;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModVillagers {

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, ZitraksMode.MODID);

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, ZitraksMode.MODID);

    // POI — блок ТНТ
    public static final RegistryObject<PoiType> DEMOLITIONIST_POI = POI_TYPES.register(
            "demolitionist_poi",
            () -> new PoiType(
                    ImmutableSet.copyOf(Blocks.TNT.getStateDefinition().getPossibleStates()),
                    1, 1
            )
    );

    // Профессия
    public static final RegistryObject<VillagerProfession> DEMOLITIONIST = VILLAGER_PROFESSIONS.register(
            "demolitionist",
            () -> new VillagerProfession(
                    "demolitionist",
                    holder -> holder.get() == DEMOLITIONIST_POI.get(),
                    holder -> holder.get() == DEMOLITIONIST_POI.get(),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH
            )
    );

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        VILLAGER_PROFESSIONS.register(bus);
    }
}