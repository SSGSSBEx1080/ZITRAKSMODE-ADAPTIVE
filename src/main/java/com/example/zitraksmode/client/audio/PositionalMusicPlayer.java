package com.example.zitraksmode.client.audio;

import java.io.File;

public class PositionalMusicPlayer {

    private final DirectOpenALPlayer player = new DirectOpenALPlayer();
    private File currentFile;
    private float baseVolume = 1.0F;

    public void start(File oggFile, float volume, double x, double y, double z, long startOffsetMs) {
        stop();
        this.currentFile = oggFile;
        this.baseVolume = clamp(volume);
        player.playPositionalFrom(oggFile, this.baseVolume, x, y, z, startOffsetMs);
    }

    public void update(double x, double y, double z) {
        player.updatePosition(x, y, z);
        player.setVolume(baseVolume);
    }

    public void setVolume(float volume) {
        this.baseVolume = clamp(volume);
        player.setVolume(this.baseVolume);
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void stop() {
        player.stop();
        currentFile = null;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
