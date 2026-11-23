package com.example.zitraksmode.client.audio;

import com.example.zitraksmode.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathNoteMusicManager {
    private static final Map<UUID, SoundInstance> ACTIVE = new HashMap<>();

    public static void playDeathNoteMusic(UUID playerUUID) {
        Minecraft mc = Minecraft.getInstance();
        stopMusic(playerUUID);

        SoundInstance sound = new AbstractTickableSoundInstance(
                ModSounds.DEATH_NOTE_OST.get(),
                SoundSource.MASTER,           // <-- теперь зависит только от общей громкости
                RandomSource.create()
        ) {
            {
                this.volume = 1.0F;           // максимальная громкость
                this.pitch = 1.0F;
                this.looping = false;
                this.attenuation = Attenuation.NONE;
            }

            @Override
            public void tick() {}
        };

        mc.getSoundManager().play(sound);
        ACTIVE.put(playerUUID, sound);
    }

    public static void stopMusic(UUID playerUUID) {
        SoundInstance old = ACTIVE.remove(playerUUID);
        if (old != null) {
            Minecraft.getInstance().getSoundManager().stop(old);
        }
    }
}