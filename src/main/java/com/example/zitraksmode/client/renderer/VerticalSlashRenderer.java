package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.items.katana.VerticalSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class VerticalSlashRenderer extends EntityRenderer<VerticalSlashEntity> {

    public VerticalSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(VerticalSlashEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        Vec3 forward = entity.getDirectionForRender(partialTicks);
        if (forward.lengthSqr() < 1.0E-8) {
            return;
        }

        double progress = entity.getTravelProgressForRender(partialTicks);
        double lower = entity.getLowerExtentForRender(progress);
        double upper = entity.getUpperExtentForRender(progress);
        double halfWidth = entity.getHalfWidthForRender(progress);

        Basis basis = computeBasis(forward, entity.getStoredYawForRender());

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        // Внешняя красная дуга
        drawArcRibbon(
                consumer,
                pose,
                basis,
                lower,
                upper,
                halfWidth,
                255, 40, 40, 180,
                26,
                1.0
        );

        // Внутреннее яркое ядро
        drawArcRibbon(
                consumer,
                pose,
                basis,
                lower,
                upper,
                halfWidth * 0.35,
                255, 100, 100, 255,
                26,
                1.02
        );

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawArcRibbon(VertexConsumer vc,
                               PoseStack.Pose pose,
                               Basis basis,
                               double lower,
                               double upper,
                               double halfWidth,
                               int r, int g, int b, int a,
                               int segments,
                               double bulgeScale) {

        double totalHeight = upper - lower;
        if (totalHeight <= 0.0) return;

        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments;
            double t1 = (i + 1) / (double) segments;

            double h0 = lerp(lower, upper, t0);
            double h1 = lerp(lower, upper, t1);

            double bulge0 = Math.sin(t0 * Math.PI) * VerticalSlashEntity.ARC_BULGE * bulgeScale;
            double bulge1 = Math.sin(t1 * Math.PI) * VerticalSlashEntity.ARC_BULGE * bulgeScale;

            Vec3 p0 = basis.up.scale(h0).add(basis.forward.scale(bulge0));
            Vec3 p1 = basis.up.scale(h1).add(basis.forward.scale(bulge1));

            Vec3 p0L = p0.subtract(basis.right.scale(halfWidth));
            Vec3 p0R = p0.add(basis.right.scale(halfWidth));
            Vec3 p1L = p1.subtract(basis.right.scale(halfWidth));
            Vec3 p1R = p1.add(basis.right.scale(halfWidth));

            // передняя сторона
            quad(vc, pose, p0L, p0R, p1R, p1L, r, g, b, a);
            // задняя сторона
            quad(vc, pose, p1L, p1R, p0R, p0L, r, g, b, a);
        }
    }

    private void quad(VertexConsumer vc,
                      PoseStack.Pose pose,
                      Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                      int r, int g, int bl, int alpha) {

        vc.vertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, alpha).endVertex();
        vc.vertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, alpha).endVertex();
        vc.vertex(pose.pose(), (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, alpha).endVertex();
        vc.vertex(pose.pose(), (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, alpha).endVertex();
    }

    private Basis computeBasis(Vec3 forward, float storedYaw) {
        Vec3 f = forward.normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);

        Vec3 right;

        if (Math.abs(f.y) > 0.97) {
            float yawRad = (float) Math.toRadians(storedYaw);
            Vec3 horizontalForward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad)).normalize();
            right = horizontalForward.cross(worldUp).normalize();

            if (right.lengthSqr() < 1.0E-6) {
                right = new Vec3(1, 0, 0);
            }
        } else {
            right = f.cross(worldUp);
            if (right.lengthSqr() < 1.0E-6) {
                right = new Vec3(1, 0, 0);
            }
            right = right.normalize();
        }

        Vec3 up = right.cross(f).normalize();
        return new Basis(f, right, up);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private record Basis(Vec3 forward, Vec3 right, Vec3 up) {
    }

    @Override
    public ResourceLocation getTextureLocation(VerticalSlashEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}