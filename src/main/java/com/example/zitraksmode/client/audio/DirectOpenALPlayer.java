package com.example.zitraksmode.client.audio;

import com.mojang.logging.LogUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;

/**
 * Прямой OpenAL плеер для OGG.
 * Attenuation по дистанции считается ВРУЧНУЮ в SpeakerAudioManager.tick().
 * OpenAL просто играет с текущим volume, без своей формулы затухания.
 */
public class DirectOpenALPlayer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private int bufferId = -1;
    private int sourceId = -1;
    private boolean isPlaying = false;

    public void play(File oggFile, float volume) {
        playInternal(oggFile, volume, 0.0D, 0.0D, 0.0D, true, 0L);
    }

    public void playPositional(File oggFile, float volume, double x, double y, double z) {
        playInternal(oggFile, volume, x, y, z, false, 0L);
    }

    public void playPositionalFrom(File oggFile, float volume, double x, double y, double z, long startOffsetMs) {
        playInternal(oggFile, volume, x, y, z, false, Math.max(0L, startOffsetMs));
    }

    private void playInternal(File oggFile,
                              float volume,
                              double x,
                              double y,
                              double z,
                              boolean relative,
                              long startOffsetMs) {
        stop();

        try {
            byte[] fileData = Files.readAllBytes(oggFile.toPath());
            ByteBuffer fileBuffer = BufferUtils.createByteBuffer(fileData.length);
            fileBuffer.put(fileData);
            fileBuffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                long decoder = STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null);
                if (decoder == MemoryUtil.NULL) {
                    LOGGER.error("JBL: stb_vorbis_open_memory failed for {}. Error code={}", oggFile.getAbsolutePath(), error.get(0));
                    return;
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);
                int channels = info.channels();
                int sampleRate = info.sample_rate();
                int lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
                if (lengthSamples <= 0) {
                    STBVorbis.stb_vorbis_close(decoder);
                    LOGGER.error("JBL: invalid/empty ogg stream {}", oggFile.getAbsolutePath());
                    return;
                }

                ShortBuffer decodedPcm = BufferUtils.createShortBuffer(lengthSamples * channels);
                int samplesDecoded = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, decodedPcm);
                STBVorbis.stb_vorbis_close(decoder);

                if (samplesDecoded <= 0) {
                    LOGGER.error("JBL: failed to decode PCM for {}", oggFile.getAbsolutePath());
                    return;
                }

                decodedPcm.limit(samplesDecoded * channels);

                int samplesToSkipPerChannel = (int) ((startOffsetMs * sampleRate) / 1000L);
                int totalSamplesToSkip = Math.max(0, samplesToSkipPerChannel * channels);
                int remainingSamples = Math.max(0, decodedPcm.limit() - totalSamplesToSkip);

                if (remainingSamples <= 0) {
                    LOGGER.warn("JBL: start offset is beyond track duration for {}", oggFile.getAbsolutePath());
                    return;
                }

                ShortBuffer pcm = BufferUtils.createShortBuffer(remainingSamples);
                for (int i = totalSamplesToSkip; i < decodedPcm.limit(); i++) {
                    pcm.put(decodedPcm.get(i));
                }
                pcm.flip();

                bufferId = AL10.alGenBuffers();
                int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                AL10.alBufferData(bufferId, format, pcm, sampleRate);

                sourceId = AL10.alGenSources();
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
                AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0F);
                AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
                AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
                AL10.alSource3f(sourceId, AL10.AL_VELOCITY, 0.0F, 0.0F, 0.0F);
                setVolume(volume);

                if (relative) {
                    AL10.alSource3f(sourceId, AL10.AL_POSITION, 0.0F, 0.0F, 0.0F);
                } else {
                    // OpenAL attenuation ОТКЛЮЧЁН — громкость считаем вручную в SpeakerAudioManager
                    AL10.alDistanceModel(AL10.AL_NONE);
                    updatePosition(x, y, z);
                }

                AL10.alSourcePlay(sourceId);
                isPlaying = true;

                LOGGER.info("JBL: started {} (channels={}, rate={}, relative={}, offsetMs={})",
                        oggFile.getName(), channels, sampleRate, relative, startOffsetMs);
            }

        } catch (IOException e) {
            LOGGER.error("JBL: failed to read ogg {}", oggFile.getAbsolutePath(), e);
            stop();
        } catch (Exception e) {
            LOGGER.error("JBL: OpenAL playback error for {}", oggFile.getAbsolutePath(), e);
            stop();
        }
    }

    public void updatePosition(double x, double y, double z) {
        if (sourceId != -1) {
            AL10.alSource3f(sourceId, AL10.AL_POSITION, (float) x, (float) y, (float) z);
        }
    }

    public void setVolume(float volume) {
        if (sourceId != -1) {
            AL10.alSourcef(sourceId, AL10.AL_GAIN, clamp(volume, 0.0F, 1.0F));
        }
    }

    public void setPitch(float pitch) {
        if (sourceId != -1) {
            AL10.alSourcef(sourceId, AL10.AL_PITCH, clamp(pitch, 0.5F, 2.0F));
        }
    }

    public void pause() {
        if (sourceId != -1) {
            AL10.alSourcePause(sourceId);
            isPlaying = false;
        }
    }

    public void resume() {
        if (sourceId != -1) {
            AL10.alSourcePlay(sourceId);
            isPlaying = true;
        }
    }

    public void stop() {
        if (sourceId != -1) {
            AL10.alSourceStop(sourceId);
            AL10.alDeleteSources(sourceId);
            sourceId = -1;
        }
        if (bufferId != -1) {
            AL10.alDeleteBuffers(bufferId);
            bufferId = -1;
        }
        isPlaying = false;
    }

    public boolean isPlaying() {
        if (!isPlaying || sourceId == -1) {
            return false;
        }
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    public boolean isPaused() {
        if (sourceId == -1) {
            return false;
        }
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}