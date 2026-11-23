package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.entities.BulletEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class BulletRenderer extends EntityRenderer<BulletEntity> {

    private final ItemStack bulletStack;

    // Геометрический центр модели по X из твоего json
    private static final double MODEL_X_OFFSET = -0.0955090625D;
    private static final float MODEL_SCALE = 2.0F;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.bulletStack = new ItemStack(ModItems.BULLET.get());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        Vec3 pos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        Vec3 prev = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 segment = pos.subtract(prev);
        Vec3 motion = entity.getDeltaMovement();

        Vec3 dir = segment.lengthSqr() > 1.0E-7 ? segment : motion;

        if (dir.lengthSqr() > 1.0E-7) {
            float renderYaw = yawFrom(dir);
            float renderPitch = pitchFrom(dir);

            poseStack.mulPose(Vector3f.YP.rotationDegrees(-renderYaw));
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(renderPitch));
        }

        poseStack.translate(MODEL_X_OFFSET, 0.0D, 0.0D);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                this.bulletStack,
                ItemTransforms.TransformType.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return new ResourceLocation(ZitraksMode.MODID, "textures/item/bullet.png");
    }

    private static float yawFrom(Vec3 v) {
        if (v.lengthSqr() < 1.0E-12) return 0.0F;
        return (float) Math.toDegrees(Math.atan2(v.z, v.x));
    }

    private static float pitchFrom(Vec3 v) {
        if (v.lengthSqr() < 1.0E-12) return 0.0F;
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        return (float) Math.toDegrees(Math.atan2(v.y, horizontal));
    }
}