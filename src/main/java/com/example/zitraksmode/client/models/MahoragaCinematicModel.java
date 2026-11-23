package com.example.zitraksmode.client.models;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.MahoragaCinematicEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MahoragaCinematicModel extends AnimatedGeoModel<MahoragaCinematicEntity> {

    @Override
    public ResourceLocation getModelResource(MahoragaCinematicEntity object) {
        return new ResourceLocation(ZitraksMode.MODID, "geo/cinematic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MahoragaCinematicEntity object) {
        return new ResourceLocation(ZitraksMode.MODID, "textures/entity/cinematic.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MahoragaCinematicEntity object) {
        return new ResourceLocation(ZitraksMode.MODID, "animations/cinematic.animation.json");
    }
}