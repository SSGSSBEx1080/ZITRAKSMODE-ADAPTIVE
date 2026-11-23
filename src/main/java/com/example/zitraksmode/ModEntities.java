package com.example.zitraksmode;

import com.example.zitraksmode.entities.AdunVillager;
import com.example.zitraksmode.entities.BulletEntity;
import com.example.zitraksmode.entities.DildoSeatEntity;
import com.example.zitraksmode.entities.ScooterEntity;
import com.example.zitraksmode.entities.GazanEntity;
import com.example.zitraksmode.entities.LaserBeamEntity;
import com.example.zitraksmode.entities.MahoragaCinematicEntity;
import com.example.zitraksmode.entities.MedusaOrbEntity;
import com.example.zitraksmode.entities.MahoragaEntity;
import com.example.zitraksmode.entities.AntonChigurEntity;
import com.example.zitraksmode.entities.SteelBallEntity;
import com.example.zitraksmode.entities.SuperTNTMinecartEntity;
import com.example.zitraksmode.entities.SuperTNTPrimedEntity;
import com.example.zitraksmode.items.katana.SlashAttackEntity;
import com.example.zitraksmode.items.katana.VerticalSlashEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import com.example.zitraksmode.entities.SuperTNTMinecartEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ZitraksMode.MODID);

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("bullet"));

    public static final RegistryObject<EntityType<AdunVillager>> ADUN_VILLAGER =
            ENTITIES.register("adun_villager",
                    () -> EntityType.Builder.of(AdunVillager::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("adun_villager"));

    public static final RegistryObject<EntityType<DildoSeatEntity>> DILDO_SEAT = ENTITIES.register("dildo_seat",
            () -> EntityType.Builder.<DildoSeatEntity>of(DildoSeatEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("dildo_seat"));

    public static final RegistryObject<EntityType<SuperTNTPrimedEntity>> SUPER_TNT = ENTITIES.register("super_tnt",
            () -> EntityType.Builder.<SuperTNTPrimedEntity>of(SuperTNTPrimedEntity::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build("super_tnt"));

    public static final RegistryObject<EntityType<SuperTNTMinecartEntity>> SUPER_TNT_MINECART = ENTITIES.register("super_tnt_minecart",
            () -> EntityType.Builder.<SuperTNTMinecartEntity>of(SuperTNTMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("super_tnt_minecart"));

    public static final RegistryObject<EntityType<SlashAttackEntity>> SLASH_ATTACK =
            ENTITIES.register("slash_attack",
                    () -> EntityType.Builder.<SlashAttackEntity>of(SlashAttackEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("slash_attack"));

    public static final RegistryObject<EntityType<VerticalSlashEntity>> VERTICAL_SLASH = ENTITIES.register("vertical_slash",
            () -> EntityType.Builder.<VerticalSlashEntity>of(VerticalSlashEntity::new, MobCategory.MISC)
                    .sized(1.0F, 20.0F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build("vertical_slash"));

    public static final RegistryObject<EntityType<SteelBallEntity>> STEEL_BALL = ENTITIES.register("steel_ball",
            () -> EntityType.Builder.<SteelBallEntity>of(SteelBallEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("steel_ball"));

    public static final RegistryObject<EntityType<GazanEntity>> GAZAN = ENTITIES.register("gazan",
            () -> EntityType.Builder.of(GazanEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("gazan"));

    public static final RegistryObject<EntityType<MahoragaEntity>> MAHORAGA = ENTITIES.register("mahoraga",
            () -> EntityType.Builder.of(MahoragaEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 3.5F)
                    .clientTrackingRange(64)
                    .build("mahoraga"));

    public static final RegistryObject<EntityType<LaserBeamEntity>> LASER_BEAM = ENTITIES.register("laser_beam",
            () -> EntityType.Builder.<LaserBeamEntity>of(LaserBeamEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSummon()
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("laser_beam"));


   public static final RegistryObject<EntityType<ScooterEntity>> SCOOTER = ENTITIES.register("scooter",
        () -> EntityType.Builder.<ScooterEntity>of(ScooterEntity::new, MobCategory.MISC)
                .sized(1.45F, 1.35F)
                .clientTrackingRange(10)
                .updateInterval(1)
                .build("scooter"));

    public static final RegistryObject<EntityType<MahoragaCinematicEntity>> MAHORAGA_CINEMATIC =
            ENTITIES.register("mahoraga_cinematic",
                    () -> EntityType.Builder.of(MahoragaCinematicEntity::new, MobCategory.MISC)
                            .sized(2.0F, 3.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("mahoraga_cinematic"));

        public static final RegistryObject<EntityType<AntonChigurEntity>> ANTON_CHIGUR = ENTITIES.register("anton_chigur",
                () -> EntityType.Builder.of(AntonChigurEntity::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("anton_chigur"));

        public static final RegistryObject<EntityType<MedusaOrbEntity>> MEDUSA_ORB =
        ENTITIES.register("medusa_orb",
                () -> EntityType.Builder.<MedusaOrbEntity>of(MedusaOrbEntity::new, MobCategory.MISC)
                        .sized(0.5F, 0.5F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("medusa_orb"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}