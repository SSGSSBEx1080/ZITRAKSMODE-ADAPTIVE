package com.example.zitraksmode.client.katana;

import com.example.zitraksmode.items.katana.KatanaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib 3 модель для катаны
 * ПРАВИЛЬНЫЕ НАЗВАНИЯ МЕТОДОВ!
 */
public class KatanaModel extends AnimatedGeoModel<KatanaItem> {
    
    @Override
    public ResourceLocation getModelResource(KatanaItem object) {
        return new ResourceLocation("zitraksmode", "geo/katana.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(KatanaItem object) {
        return new ResourceLocation("zitraksmode", "textures/item/katana.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(KatanaItem animatable) {
        return new ResourceLocation("zitraksmode", "animations/katana.animation.json");
    }
}
