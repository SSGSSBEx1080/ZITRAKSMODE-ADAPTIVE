package com.example.zitraksmode;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ZitraksMode.MODID);
    
    // Существующие звуки
    public static final RegistryObject<SoundEvent> CUSTOM_EXPLOSION = registerSoundEvent("custom_explosion");
    
    // Звуки для снайперки
    public static final RegistryObject<SoundEvent> SNIPER_SHOT = registerSoundEvent("sniper_shot");
    public static final RegistryObject<SoundEvent> SNIPER_RELOAD = registerSoundEvent("sniper_reload");
    public static final RegistryObject<SoundEvent> SNIPER_ULTIMATE_CHARGE = registerSoundEvent("sniper_ultimate_charge");
    public static final RegistryObject<SoundEvent> SNIPER_ULTIMATE_SHOT = registerSoundEvent("sniper_ultimate_shot");
    public static final RegistryObject<SoundEvent> SCOPE_IN = registerSoundEvent("scope_in");
    public static final RegistryObject<SoundEvent> SCOPE_OUT = registerSoundEvent("scope_out");
    
    // ЗВУКИ ДЛЯ АДУНА (в корневой папке sounds)
    public static final RegistryObject<SoundEvent> ADUN_AMBIENT = registerSoundEvent("adun_ambient");
    public static final RegistryObject<SoundEvent> ADUN_TRADE = registerSoundEvent("adun_trade");
    public static final RegistryObject<SoundEvent> ADUN_HURT = registerSoundEvent("adun_hurt");
    public static final RegistryObject<SoundEvent> ADUN_DEATH = registerSoundEvent("adun_death");
    
    public static final RegistryObject<SoundEvent> CASE_BATTLE = registerSoundEvent("case_battle");
    // ===== MADE IN HEAVEN =====
    public static final RegistryObject<SoundEvent> MADE_IN_HEAVEN = registerSoundEvent("made_in_heaven");

     // ════════════════════════════════════════════════════════════
    // СТАЛЬНОЙ ШАР (STEEL BALL)
    // ════════════════════════════════════════════════════════════
    public static final RegistryObject<SoundEvent> STEEL_BALL_CHARGE = registerSoundEvent("steel_ball_charge");
    public static final RegistryObject<SoundEvent> STEEL_BALL_THROW = registerSoundEvent("steel_ball_throw");
    public static final RegistryObject<SoundEvent> STEEL_BALL_SPIN = registerSoundEvent("steel_ball_spin");
    public static final RegistryObject<SoundEvent> STEEL_BALL_RETURN = registerSoundEvent("steel_ball_return");
    
    // ════════════════════════════════════════════════════════════
    // КАТАНА ЗВУКИ
    // ════════════════════════════════════════════════════════════
    
    // Обычные слеши (3 типа)
    public static final RegistryObject<SoundEvent> KATANA_SLASH_1 = 
        registerSoundEvent("katana_slash_1");
    public static final RegistryObject<SoundEvent> KATANA_SLASH_2 = 
        registerSoundEvent("katana_slash_2");
    public static final RegistryObject<SoundEvent> KATANA_SLASH_3 = 
        registerSoundEvent("katana_slash_3");
    
    // Заряженный удар
    public static final RegistryObject<SoundEvent> KATANA_CHARGE = 
        registerSoundEvent("katana_charge");
    public static final RegistryObject<SoundEvent> KATANA_CHARGED_SLASH = 
        registerSoundEvent("katana_charged_slash");
    
    // Парирование (3 типа)
    public static final RegistryObject<SoundEvent> KATANA_PARRY_1 = 
        registerSoundEvent("katana_parry_1");
    public static final RegistryObject<SoundEvent> KATANA_PARRY_2 = 
        registerSoundEvent("katana_parry_2");
    public static final RegistryObject<SoundEvent> KATANA_PARRY_3 = 
        registerSoundEvent("katana_parry_3");

    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_1 = registerSoundEvent("gazan_speech_1");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_2 = registerSoundEvent("gazan_speech_2");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_3 = registerSoundEvent("gazan_speech_3");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_4 = registerSoundEvent("gazan_speech_4");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_5 = registerSoundEvent("gazan_speech_5");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_6 = registerSoundEvent("gazan_speech_6");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_7 = registerSoundEvent("gazan_speech_7");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_8 = registerSoundEvent("gazan_speech_8");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_9 = registerSoundEvent("gazan_speech_9");
    public static final RegistryObject<SoundEvent> GAZAN_SPEECH_10 = registerSoundEvent("gazan_speech_10");
    public static final RegistryObject<SoundEvent> GAZAN_TRADE_1 = registerSoundEvent("gazan_trade_1");
    public static final RegistryObject<SoundEvent> GAZAN_TRADE_2 = registerSoundEvent("gazan_trade_2");
    public static final RegistryObject<SoundEvent> GAZAN_HURT_1 = registerSoundEvent("gazan_hurt_1");
    public static final RegistryObject<SoundEvent> GAZAN_HURT_2 = registerSoundEvent("gazan_hurt_2");
    public static final RegistryObject<SoundEvent> GAZAN_DEATH = registerSoundEvent("gazan_death");
    // Газан — открытие торговли (замена "Хммм")
    public static final RegistryObject<SoundEvent> GAZAN_TRADE_OPEN = registerSoundEvent("gazan_trade_open");
    // Газан — звук "да" при выборе товара
    public static final RegistryObject<SoundEvent> GAZAN_YES = registerSoundEvent("gazan_yes");
    // Газан — звук "нет" при отмене
    public static final RegistryObject<SoundEvent> GAZAN_NO = registerSoundEvent("gazan_no");
    
    // Ультимейт
    public static final RegistryObject<SoundEvent> KATANA_ULTIMATE_CHARGE = 
        registerSoundEvent("katana_ultimate_charge");
    public static final RegistryObject<SoundEvent> KATANA_ULTIMATE_SLASH = 
        registerSoundEvent("katana_ultimate_slash");
    public static final RegistryObject<SoundEvent> DEATH_NOTE_OST = registerSoundEvent("death_note_ost");
    // Взятие в руки
    public static final RegistryObject<SoundEvent> KATANA_EQUIP = 
        registerSoundEvent("katana_equip");

    public static final RegistryObject<SoundEvent> MAHORAGA_AMBIENT = registerSoundEvent("mahoraga_ambient");
    public static final RegistryObject<SoundEvent> MAHORAGA_HURT = registerSoundEvent("mahoraga_hurt");
    public static final RegistryObject<SoundEvent> MAHORAGA_DEATH = registerSoundEvent("mahoraga_death");
    public static final RegistryObject<SoundEvent> MAHORAGA_ATTACK = registerSoundEvent("mahoraga_attack");
    public static final RegistryObject<SoundEvent> MAHORAGA_ADAPT = registerSoundEvent("mahoraga_adapt");
    public static final RegistryObject<SoundEvent> MAHORAGA_SCREAM = registerSoundEvent("mahoraga_scream");
    public static final RegistryObject<SoundEvent> MAHORAGA_LASER = registerSoundEvent("mahoraga_laser");
    public static final RegistryObject<SoundEvent> MAHORAGA_MUSIC = registerSoundEvent("mahoraga_music");
    public static final RegistryObject<SoundEvent> MAHORAGA_JUMP = registerSoundEvent("mahoraga_jump");
    public static final RegistryObject<SoundEvent> MAHORAGA_SPEEDBLITZ = registerSoundEvent("mahoraga_speedblitz");

    // ===== АНТОН ЧИГУР =====

    // Шаги / слежка
    public static final RegistryObject<SoundEvent> CHIGUR_STEP =
            registerSoundEvent("chigur_step");

    // Дробовик
    public static final RegistryObject<SoundEvent> CHIGUR_SHOT =
            registerSoundEvent("chigur_shot");

    public static final RegistryObject<SoundEvent> CHIGUR_RELOAD =
            registerSoundEvent("chigur_reload");

    // Монетка
    public static final RegistryObject<SoundEvent> CHIGUR_COIN =
            registerSoundEvent("chigur_coin");

    // Исчезновение
    public static final RegistryObject<SoundEvent> CHIGUR_VANISH =
            registerSoundEvent("chigur_vanish");

    // Пневматическое устройство
    public static final RegistryObject<SoundEvent> CHIGUR_AIRGUN =
            registerSoundEvent("chigur_airgun");

    // Реплики Чигура
    public static final RegistryObject<SoundEvent> CHIGUR_SPEECH_1 =
            registerSoundEvent("chigur_speech_1");

    public static final RegistryObject<SoundEvent> CHIGUR_SPEECH_2 =
            registerSoundEvent("chigur_speech_2");

    public static final RegistryObject<SoundEvent> CHIGUR_SPEECH_3 =
            registerSoundEvent("chigur_speech_3");

    public static final RegistryObject<SoundEvent> CHIGUR_SPEECH_4 =
            registerSoundEvent("chigur_speech_4");

    public static final RegistryObject<SoundEvent> CHIGUR_SPEECH_5 =
            registerSoundEvent("chigur_speech_5");

    public static final RegistryObject<SoundEvent> CHIGUR_SPEECH_6 =
            registerSoundEvent("chigur_speech_6");

    public static final RegistryObject<SoundEvent> VACUUM_LOOP = registerSoundEvent("vacuum_loop");

    public static final RegistryObject<SoundEvent> INTIMATE_SOUND =
        registerSoundEvent("intimate_sound");

    public static final RegistryObject<SoundEvent> BLOWJOB_SOUND =
        registerSoundEvent("blowjob_sound");

    public static final RegistryObject<SoundEvent> SANDEVISTAN_ACTIVATE = registerSoundEvent("sandevistan_activate");
    public static final RegistryObject<SoundEvent> SANDEVISTAN_DEACTIVATE = registerSoundEvent("sandevistan_deactivate");
    public static final RegistryObject<SoundEvent> SANDEVISTAN_PSYCHOSIS = registerSoundEvent("sandevistan_psychosis");
    public static final RegistryObject<SoundEvent> SANDEVISTAN_INJECTION = registerSoundEvent("sandevistan_injection");
    
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(ZitraksMode.MODID, name)));
    }
    
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
