package com.example.zitraksmode.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;
import com.example.zitraksmode.entities.AdunVillager;

public class AdunVillagerRenderer extends MobRenderer<AdunVillager, VillagerModel<AdunVillager>> {
    // ПРЯМОЙ ПУТЬ К ТЕКСТУРЕ - проверь что MODID правильный
    private static final ResourceLocation ADUN_TEXTURE = new ResourceLocation("zitraksmode", "textures/entity/adun_villager.png");

    public AdunVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

  private static boolean textureLogged = false;

@Override
public ResourceLocation getTextureLocation(AdunVillager entity) {
    if (!textureLogged) {
        System.out.println("=== LOADING ADUN TEXTURE ONCE: " + ADUN_TEXTURE + " ===");
        textureLogged = true;
    }
    return ADUN_TEXTURE;
}

    @Override
    protected void scale(AdunVillager villager, PoseStack poseStack, float partialTickTime) {
        float scale = 0.9375F;
        poseStack.scale(scale, scale, scale);
    }
}