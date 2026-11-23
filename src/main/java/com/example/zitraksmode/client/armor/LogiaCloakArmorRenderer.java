package com.example.zitraksmode.client.armor;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LogiaCloakItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

/** GeckoLib renderer. Every cloak has its own geo/texture file, even though
 * the included starter meshes share one silhouette. */
public final class LogiaCloakArmorRenderer extends GeoArmorRenderer<LogiaCloakItem> {
    public LogiaCloakArmorRenderer() {
        super(new AnimatedGeoModel<>() {
            @Override
            public ResourceLocation getModelResource(LogiaCloakItem item) {
                return new ResourceLocation(ZitraksMode.MODID,
                        "geo/cloak_" + item.getCloakType().id + ".geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(LogiaCloakItem item) {
                return new ResourceLocation(ZitraksMode.MODID,
                        "textures/item/cloak_" + item.getCloakType().id + ".png");
            }

            @Override
            public ResourceLocation getAnimationResource(LogiaCloakItem item) {
                return new ResourceLocation(ZitraksMode.MODID, "animations/cloak_idle.animation.json");
            }
        });
        // GeckoLib 3 expects this exact Blockbench bone to follow the player's chest.
        this.bodyBone = "armorBody";
    }
}
