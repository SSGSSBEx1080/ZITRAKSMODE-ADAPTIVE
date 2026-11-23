package com.example.zitraksmode.client.armor;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.SpanishBootsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SpanishBootsArmorModel extends AnimatedGeoModel<SpanishBootsItem> {

    @Override
    public ResourceLocation getModelResource(SpanishBootsItem object) {
        return new ResourceLocation(ZitraksMode.MODID, "geo/spanish_boots.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SpanishBootsItem object) {
        return new ResourceLocation(ZitraksMode.MODID, "textures/armor/spanish_boots_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SpanishBootsItem animatable) {
        return new ResourceLocation(ZitraksMode.MODID, "animations/empty.animation.json");
    }
}