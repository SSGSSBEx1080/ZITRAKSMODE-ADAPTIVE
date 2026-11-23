package com.example.zitraksmode.client.armor;

import com.example.zitraksmode.items.AqualungItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class AqualungArmorModel extends AnimatedGeoModel<AqualungItem> {

    @Override
    public ResourceLocation getModelResource(AqualungItem object) {
        return new ResourceLocation("zitraksmode", "geo/aqualung.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AqualungItem object) {
        return new ResourceLocation("zitraksmode", "textures/armor/aqualung_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AqualungItem object) {
        return null; // Без анимаций
    }
}