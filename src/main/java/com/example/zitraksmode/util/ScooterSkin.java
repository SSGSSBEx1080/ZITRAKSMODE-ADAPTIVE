package com.example.zitraksmode.util;

import net.minecraft.resources.ResourceLocation;

public enum ScooterSkin {
    DEFAULT(0, "Стандарт", new ResourceLocation("minecraft", "textures/block/iron_block.png")),
    YANDEX(1, "Яндекс Go", new ResourceLocation("minecraft", "textures/block/yellow_concrete.png")),
    YURENT(2, "Юрент", new ResourceLocation("minecraft", "textures/block/lime_concrete.png"));

    private final int id;
    private final String displayName;
    private final ResourceLocation texture;

    ScooterSkin(int id, String displayName, ResourceLocation texture) {
        this.id = id;
        this.displayName = displayName;
        this.texture = texture;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public static ScooterSkin byId(int id) {
        for (ScooterSkin skin : values()) {
            if (skin.id == id) {
                return skin;
            }
        }
        return DEFAULT;
    }

    public static ScooterSkin random(java.util.Random random) {
        ScooterSkin[] values = values();
        return values[random.nextInt(values.length)];
    }
}
