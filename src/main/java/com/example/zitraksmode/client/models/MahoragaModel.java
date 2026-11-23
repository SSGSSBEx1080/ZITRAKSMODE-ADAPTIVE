package com.example.zitraksmode.client.models;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.MahoragaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import javax.annotation.Nullable;

public class MahoragaModel extends AnimatedGeoModel<MahoragaEntity> {

    @Override
    public ResourceLocation getModelResource(MahoragaEntity object) {
        return new ResourceLocation(ZitraksMode.MODID, "geo/mahoraga.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MahoragaEntity object) {
        return new ResourceLocation(ZitraksMode.MODID, "textures/entity/mahoraga.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MahoragaEntity object) {
        return new ResourceLocation(ZitraksMode.MODID, "animations/mahoraga.animation.json");
    }

    @Override
    public void setLivingAnimations(MahoragaEntity entity, Integer uniqueID,
                                    @Nullable AnimationEvent customPredicate) {
        super.setLivingAnimations(entity, uniqueID, customPredicate);

        IBone wheel = this.getAnimationProcessor().getBone("wheel");
        if (wheel != null) {
            wheel.setRotationY((float) Math.toRadians(entity.getWheelRotation()));
        }
    }
}