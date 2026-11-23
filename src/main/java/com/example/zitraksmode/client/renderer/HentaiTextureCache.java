package com.example.zitraksmode.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU-кэш для текстур хентай-блока.
 * Держит в VRAM максимум MAX_CACHED_TEXTURES картинок.
 * Когда лимит превышен — выгружает самую давно использованную.
 *
 * Это спасает VRAM при большом количестве картинок (130+×3=390).
 * Без кэша: 390 × 4 МБ = ~1.5 ГБ. С кэшем: ~максимум 240 МБ.
 */
@OnlyIn(Dist.CLIENT)
public class HentaiTextureCache {

    /** Максимум одновременно загруженных текстур в VRAM. */
    private static final int MAX_CACHED_TEXTURES = 60000;

    /** LRU-мапа: последняя использованная в конце, самая старая в начале. */
    private static final LinkedHashMap<ResourceLocation, Boolean> CACHE =
            new LinkedHashMap<>(MAX_CACHED_TEXTURES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ResourceLocation, Boolean> eldest) {
                    if (size() > MAX_CACHED_TEXTURES) {
                        unloadTexture(eldest.getKey());
                        return true;
                    }
                    return false;
                }
            };

    private HentaiTextureCache() {}

    /**
     * Отмечает текстуру как "недавно использованную" и убеждается что она загружена.
     * Вызывать ПЕРЕД рендером грани.
     */
    public static void touchTexture(ResourceLocation loc) {
        if (loc == null) return;

        synchronized (CACHE) {
            if (!CACHE.containsKey(loc)) {
                // Не в кэше — регистрируем в TextureManager (это загрузит с диска)
                TextureManager tm = Minecraft.getInstance().getTextureManager();
                tm.register(loc, new SimpleTexture(loc));
            }
            // put обновляет LRU-позицию → эта текстура теперь "самая свежая"
            CACHE.put(loc, Boolean.TRUE);
        }
    }

    private static void unloadTexture(ResourceLocation loc) {
        // Выгружаем на главном потоке (OpenGL требует)
        RenderSystem.recordRenderCall(() -> {
            try {
                Minecraft.getInstance().getTextureManager().release(loc);
            } catch (Exception ignored) {
            }
        });
    }

    public static int getCachedCount() {
        synchronized (CACHE) {
            return CACHE.size();
        }
    }
}