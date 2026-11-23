package com.example.zitraksmode;

import com.example.zitraksmode.enchantment.BarygaEnchantment;
import com.example.zitraksmode.enchantment.BombardierEnchantment;
import com.example.zitraksmode.enchantment.DoublePenetrationEnchantment;
import com.example.zitraksmode.enchantment.HowHePiercedMeEnchantment;
import com.example.zitraksmode.enchantment.MiningIndustryEnchantment;
import com.example.zitraksmode.enchantment.NimbleFingersEnchantment;
import com.example.zitraksmode.enchantment.PeacemakerEnchantment;
import com.example.zitraksmode.enchantment.AiruhanEnchantment;
import com.example.zitraksmode.enchantment.PeresvetEnchantment;
import com.example.zitraksmode.enchantment.SciencePowerEnchantment;
import com.example.zitraksmode.enchantment.VodohlebusEnchantment;
import com.example.zitraksmode.enchantment.HochuLabubuEnchantment;
import com.example.zitraksmode.enchantment.YobyrMamokEnchantment;
import com.example.zitraksmode.enchantment.WorldSlashEnchantment;
import com.example.zitraksmode.enchantment.BallBreakerEnchantment;
import com.example.zitraksmode.enchantment.ZhirnyPidorEnchantment;
import com.example.zitraksmode.enchantment.BedWarsEnchantment;
import com.example.zitraksmode.enchantment.MorichekEnchantment;
import com.example.zitraksmode.enchantment.NahuyaEnchantment;
import com.example.zitraksmode.enchantment.ZaebisEnchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModEnchantments {

    private static final Logger LOGGER = LogManager.getLogger("ZitraksMode/ModEnchantments");

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "zitraksmode");

    public static final RegistryObject<Enchantment> BOMBARDIER =
            ENCHANTMENTS.register("bombardier", () -> {
                LOGGER.info("Регистрация зачарования Бомбардир через DeferredRegister");
                return new BombardierEnchantment();
            });

    public static final RegistryObject<Enchantment> PEACEMAKER =
            ENCHANTMENTS.register("peacemaker", () -> {
                LOGGER.info("Регистрация зачарования Миротворец через DeferredRegister");
                return new PeacemakerEnchantment();
            });

    public static final RegistryObject<Enchantment> HOW_HE_PIERCED_ME =
            ENCHANTMENTS.register("how_he_pierced_me", () -> {
                LOGGER.info("Регистрация зачарования Как он меня прошил?");
                return new HowHePiercedMeEnchantment();
            });

    public static final RegistryObject<Enchantment> DOUBLE_PENETRATION =
            ENCHANTMENTS.register("double_penetration", () -> {
                LOGGER.info("Регистрация зачарования Двойное проникновение");
                return new DoublePenetrationEnchantment();
            });

    public static final RegistryObject<Enchantment> NIMBLE_FINGERS =
            ENCHANTMENTS.register("nimble_fingers", () -> {
                LOGGER.info("Регистрация зачарования Умелые пальчики");
                return new NimbleFingersEnchantment();
            });

    public static final RegistryObject<Enchantment> PERESVET =
            ENCHANTMENTS.register("peresvet", () -> {
                LOGGER.info("Регистрация зачарования Пересвет");
                return new PeresvetEnchantment();
            });

    public static final RegistryObject<Enchantment> WORLD_SLASH =
            ENCHANTMENTS.register("world_slash", () -> {
                LOGGER.info("Регистрация зачарования Мировой разрез");
                return new WorldSlashEnchantment();
            });

    public static final RegistryObject<Enchantment> MINING_INDUSTRY =
            ENCHANTMENTS.register("mining_industry", () -> {
                LOGGER.info("Регистрация зачарования Горнодобывающая промышленность");
                return new MiningIndustryEnchantment();
            });

    public static final RegistryObject<Enchantment> VODOHLEBUS =
            ENCHANTMENTS.register("vodohlebus", () -> {
                LOGGER.info("Регистрация зачарования Водохлёбус");
                return new VodohlebusEnchantment();
            });

    public static final RegistryObject<Enchantment> ZHIRNY_PIDOR =
            ENCHANTMENTS.register("zhirny_pidor", () -> {
                LOGGER.info("Регистрация зачарования Жирный пидор");
                return new ZhirnyPidorEnchantment();
            });

    public static final RegistryObject<Enchantment> BARYGA =
            ENCHANTMENTS.register("baryga", () -> {
                LOGGER.info("Регистрация зачарования Барыга");
                return new BarygaEnchantment();
            });

    public static final RegistryObject<Enchantment> BALL_BREAKER =
            ENCHANTMENTS.register("ball_breaker", () -> {
                LOGGER.info("Регистрация зачарования Ball Breaker");
                return new BallBreakerEnchantment();
            });

    public static final RegistryObject<Enchantment> MORICHEK =
            ENCHANTMENTS.register("morichek", () -> {
                LOGGER.info("Регистрация зачарования И теперь я моричёк...");
                return new MorichekEnchantment();
            });

    public static final RegistryObject<Enchantment> BED_WARS =
            ENCHANTMENTS.register("bed_wars", () -> {
                LOGGER.info("Регистрация зачарования БЭД Варс");
                return new BedWarsEnchantment();
            });

    public static final RegistryObject<Enchantment> YOBYR_MAMOK =
            ENCHANTMENTS.register("yobyr_mamok", () -> {
                LOGGER.info("Регистрация зачарования Ёбырь мамок");
                return new YobyrMamokEnchantment();
            });

    public static final RegistryObject<Enchantment> HOCHU_LABUBU =
            ENCHANTMENTS.register("hochu_labubu", () -> {
                LOGGER.info("Регистрация зачарования Хочу Лабубу, но, не хочу хуй сосать");
                return new HochuLabubuEnchantment();
            });

    public static final RegistryObject<Enchantment> NAHUYA =
            ENCHANTMENTS.register("nahuya", () -> {
                LOGGER.info("Регистрация зачарования Нахуя?");
                return new NahuyaEnchantment();
            });

    public static final RegistryObject<Enchantment> ZAEBIS =
            ENCHANTMENTS.register("zaebis", () -> {
                LOGGER.info("Регистрация зачарования Заебись");
                return new ZaebisEnchantment();
            });

    public static final RegistryObject<Enchantment> AIRUHAN =
        ENCHANTMENTS.register("airuhan", () -> {
            LOGGER.info("Регистрация зачарования Воздухан");
            return new AiruhanEnchantment();
        });

     public static final RegistryObject<Enchantment> SCIENCE_POWER =
            ENCHANTMENTS.register("science_power", () -> {
                LOGGER.info("Регистрация зачарования Сила науки");
                return new SciencePowerEnchantment();
            });

    public static void register(IEventBus eventBus) {
        LOGGER.info("ModEnchantments.register() вызван, регистрируем на шине");
        ENCHANTMENTS.register(eventBus);


    }
}
