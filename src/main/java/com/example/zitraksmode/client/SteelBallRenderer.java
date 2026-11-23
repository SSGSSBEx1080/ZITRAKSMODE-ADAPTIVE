package com.example.zitraksmode.client;

import com.example.zitraksmode.entities.SteelBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SteelBallRenderer extends EntityRenderer<SteelBallEntity> {

    private final ItemRenderer itemRenderer;

    public SteelBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SteelBallEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();

        pose.translate(0.0D, 0.08D, 0.0D);

        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-5D) {
            float renderYaw = (float) (Mth.atan2(motion.x, motion.z) * 57.2957763671875D);
            float renderPitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * 57.2957763671875D);

            pose.mulPose(Vector3f.YP.rotationDegrees(180.0F - renderYaw));
            pose.mulPose(Vector3f.XP.rotationDegrees(renderPitch));
        }

        float angle = entity.spinAngle + partialTicks * 8.0F;
        pose.mulPose(Vector3f.ZP.rotationDegrees(angle));

        pose.scale(0.75F, 0.75F, 0.75F);

        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemTransforms.TransformType.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                pose,
                buffer,
                entity.getId()
        );

        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SteelBallEntity entity) {
        return new ResourceLocation("zitraksmode", "textures/item/steel_ball.png");
    }
}