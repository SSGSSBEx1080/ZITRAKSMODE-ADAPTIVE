package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.blockentities.HentaiBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;

public class HentaiBlockRenderer implements BlockEntityRenderer<HentaiBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float LOCK_OVERLAY_OFFSET = 0.002f;

    public HentaiBlockRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(HentaiBlockEntity entity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

        Map<String, String> textureKeys = entity.getTextureKeys();
        if (textureKeys == null || textureKeys.isEmpty()) return;

        Level level = entity.getLevel();
        BlockPos pos = entity.getBlockPos();

        poseStack.pushPose();

        for (Direction dir : Direction.values()) {
            if (level != null && isNeighborSameHentaiBlock(entity, level, pos, dir)) {
                continue;
            }

            String face = dir.getSerializedName();
            String key = textureKeys.get(face);
            if (key == null || key.isEmpty()) continue;

            ResourceLocation textureLoc = getTextureLocation(key);
            if (textureLoc == null) continue;

            HentaiTextureCache.touchTexture(textureLoc);

            VertexConsumer base = buffer.getBuffer(RenderType.entitySolid(textureLoc));
            renderFace(poseStack, base, dir, combinedLight, combinedOverlay, 255, 255, 255, 255, 0.0f);

            if (entity.isFaceLocked(face)) {
                VertexConsumer overlay = buffer.getBuffer(RenderType.entityTranslucent(textureLoc));
                renderFace(poseStack, overlay, dir, combinedLight, combinedOverlay, 80, 255, 80, 150, LOCK_OVERLAY_OFFSET);
            }
        }

        poseStack.popPose();
    }

    private boolean isNeighborSameHentaiBlock(HentaiBlockEntity entity, Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        BlockState selfState = level.getBlockState(pos);
        return neighborState.getBlock() == selfState.getBlock();
    }

    private ResourceLocation getTextureLocation(String key) {
        if (key.contains(":")) {
            return new ResourceLocation(key);
        }

        String[] parts = key.split("/", 2);
        if (parts.length != 2) {
            LOGGER.error("❌ Bad key: {}", key);
            return null;
        }

        return new ResourceLocation("zitraksmode",
                "textures/block/" + parts[0] + "/" + parts[1] + ".png");
    }

    private void renderFace(PoseStack poseStack, VertexConsumer builder, Direction dir,
                            int light, int overlay, int red, int green, int blue, int alpha, float normalOffset) {

        float x1 = 0f, x2 = 1f;
        float y1 = 0f, y2 = 1f;
        float z1 = 0f, z2 = 1f;

        float u1 = 0.0f, v1 = 0.0f;
        float u2 = 1.0f, v2 = 1.0f;

        PoseStack.Pose last = poseStack.last();
        Matrix4f matrix = last.pose();
        Matrix3f normalMatrix = last.normal();

        switch (dir) {
            case DOWN -> {
                addVertex(builder, matrix, normalMatrix, x1, y1, z1, u1, v1, 0, -1, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y1, z1, u2, v1, 0, -1, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y1, z2, u2, v2, 0, -1, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y1, z2, u1, v2, 0, -1, 0, light, overlay, red, green, blue, alpha, normalOffset);
            }
            case UP -> {
                addVertex(builder, matrix, normalMatrix, x1, y2, z1, u1, v1, 0, 1, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y2, z2, u1, v2, 0, 1, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y2, z2, u2, v2, 0, 1, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y2, z1, u2, v1, 0, 1, 0, light, overlay, red, green, blue, alpha, normalOffset);
            }
            case NORTH -> {
                addVertex(builder, matrix, normalMatrix, x1, y1, z1, u2, v2, 0, 0, -1, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y2, z1, u2, v1, 0, 0, -1, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y2, z1, u1, v1, 0, 0, -1, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y1, z1, u1, v2, 0, 0, -1, light, overlay, red, green, blue, alpha, normalOffset);
            }
            case SOUTH -> {
                addVertex(builder, matrix, normalMatrix, x1, y1, z2, u1, v2, 0, 0, 1, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y1, z2, u2, v2, 0, 0, 1, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y2, z2, u2, v1, 0, 0, 1, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y2, z2, u1, v1, 0, 0, 1, light, overlay, red, green, blue, alpha, normalOffset);
            }
            case WEST -> {
                addVertex(builder, matrix, normalMatrix, x1, y1, z1, u1, v2, -1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y1, z2, u2, v2, -1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y2, z2, u2, v1, -1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x1, y2, z1, u1, v1, -1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
            }
            case EAST -> {
                addVertex(builder, matrix, normalMatrix, x2, y1, z1, u2, v2, 1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y2, z1, u2, v1, 1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y2, z2, u1, v1, 1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
                addVertex(builder, matrix, normalMatrix, x2, y1, z2, u1, v2, 1, 0, 0, light, overlay, red, green, blue, alpha, normalOffset);
            }
        }
    }

    private void addVertex(VertexConsumer builder, Matrix4f matrix, Matrix3f normalMatrix,
                           float x, float y, float z, float u, float v,
                           float nx, float ny, float nz,
                           int light, int overlay,
                           int red, int green, int blue, int alpha,
                           float normalOffset) {

        float px = x + nx * normalOffset;
        float py = y + ny * normalOffset;
        float pz = z + nz * normalOffset;

        builder.vertex(matrix, px, py, pz)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }
}