package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.client.models.MahoragaModel;
import com.example.zitraksmode.entities.MahoragaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class MahoragaRenderer extends GeoEntityRenderer<MahoragaEntity> {
    public MahoragaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MahoragaModel());
        this.shadowRadius = 0.7F;
    }

    @Override
    protected void applyRotations(MahoragaEntity entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTicks) {
        // Разворачиваем модель на 180°, чтобы ходил лицом вперёд
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
    }
}