package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.client.models.MahoragaCinematicModel;
import com.example.zitraksmode.entities.MahoragaCinematicEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class MahoragaCinematicRenderer extends GeoEntityRenderer<MahoragaCinematicEntity> {
    public MahoragaCinematicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MahoragaCinematicModel());
        this.shadowRadius = 0.0F;
    }
}