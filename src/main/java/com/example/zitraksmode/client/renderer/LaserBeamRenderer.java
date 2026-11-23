package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.entities.LaserBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class LaserBeamRenderer extends EntityRenderer<LaserBeamEntity> {

    private static final ResourceLocation DUMMY_TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public LaserBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull LaserBeamEntity entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(LaserBeamEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        Vec3 start = entity.getStartPos();
        Vec3 end = entity.getEndPos();
        Vec3 dir = end.subtract(start);
        double length = dir.length();

        if (length < 0.01D) {
            return;
        }

        Vec3 localStart = start.subtract(entity.position());

        float yaw = (float) (Mth.atan2(dir.x, dir.z) * (180F / Math.PI));
        float pitch = (float) (-Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180F / Math.PI));

        poseStack.pushPose();
        poseStack.translate(localStart.x, localStart.y, localStart.z);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(pitch));

        renderBeamCross(poseStack, bufferSource, (float) length, 0.62F, 255, 255, 255, 75);
        renderBeamCross(poseStack, bufferSource, (float) length, 0.34F, 255, 255, 255, 220);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderBeamCross(PoseStack poseStack, MultiBufferSource bufferSource, float length, float radius,
                                 int red, int green, int blue, int alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        float half = radius * 0.5F;

        renderQuad(consumer, pose, normal, -half, -radius, 0.0F, half, radius, length, red, green, blue, alpha);
        renderQuad(consumer, pose, normal, -radius, -half, 0.0F, radius, half, length, red, green, blue, alpha);
        renderQuad(consumer, pose, normal, -half, -radius, 0.0F, half, radius, length, red, green, blue, alpha);
        renderQuadRotated45(consumer, pose, normal, radius, length, red, green, blue, alpha);
    }

    private void renderQuadRotated45(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                     float radius, float length, int red, int green, int blue, int alpha) {
        float diag = radius * 0.70710677F;
        renderQuad(consumer, pose, normal, -diag, -diag, 0.0F, diag, diag, length, red, green, blue, alpha);
        renderQuad(consumer, pose, normal, -diag, diag, 0.0F, diag, -diag, length, red, green, blue, alpha);
    }

    private void renderQuad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            int red, int green, int blue, int alpha) {
        vertex(consumer, pose, normal, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, pose, normal, x1, y1, z2, red, green, blue, alpha);
        vertex(consumer, pose, normal, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, pose, normal, x2, y2, z1, red, green, blue, alpha);
    }

    private void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                        float x, float y, float z,
                        int red, int green, int blue, int alpha) {
        consumer.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LaserBeamEntity entity) {
        return DUMMY_TEXTURE;
    }
}
