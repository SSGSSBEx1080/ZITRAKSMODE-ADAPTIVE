package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.entities.GazanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

public class GazanRenderer extends MobRenderer<GazanEntity, VillagerModel<GazanEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("zitraksmode", "textures/entity/gazan/gazan.png");

    public GazanRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        // Только один слой — скрещенные руки (как у Адуна)
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(GazanEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(GazanEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = 0.9375F;
        poseStack.scale(scale, scale, scale);
    }
}