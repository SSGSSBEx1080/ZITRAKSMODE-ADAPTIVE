package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.client.models.AntonChigurModel;
import com.example.zitraksmode.entities.AntonChigurEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public final class AntonChigurRenderer extends GeoEntityRenderer<AntonChigurEntity> {
    public AntonChigurRenderer(EntityRendererProvider.Context context) {
        super(context, new AntonChigurModel());
        shadowRadius = 0.45F;
    }
}
