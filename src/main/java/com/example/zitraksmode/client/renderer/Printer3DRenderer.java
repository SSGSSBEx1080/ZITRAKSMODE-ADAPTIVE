package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.blockentities.Printer3DBlockEntity;
import com.example.zitraksmode.blocks.Printer3DBlock;
import com.example.zitraksmode.printer.BlockPlacement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Full, cached hologram renderer.
 *
 * The hologram never skips every second block. Instead it renders every visible
 * outer block of a recipe. Blocks fully hidden inside a solid build are omitted
 * because the player could not see them anyway. This keeps large solid prints
 * visually complete while dramatically reducing useless draw calls.
 */
public final class Printer3DRenderer implements BlockEntityRenderer<Printer3DBlockEntity> {
    private static final float GHOST_ALPHA_MULTIPLIER = 0.42F;

    private static final float AREA_RED = 0.00F;
    private static final float AREA_GREEN = 1.00F;
    private static final float AREA_BLUE = 0.00F;
    private static final float AREA_ALPHA = 0.45F;

    /** Rebuild expensive preview geometry once per second, not every frame. */
    private static final int CACHE_REFRESH_TICKS = 20;

    private static final Map<Printer3DBlockEntity, CachedPreview> PREVIEW_CACHE =
            new WeakHashMap<>();

    @Override
    public void render(
            Printer3DBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        if (blockEntity.isPrinting()) {
            PREVIEW_CACHE.remove(blockEntity);
            return;
        }

        CachedPreview preview = getCachedPreview(blockEntity);
        BlockPos printerPos = blockEntity.getBlockPos();

        poseStack.pushPose();
        poseStack.translate(
                -printerPos.getX(),
                -printerPos.getY(),
                -printerPos.getZ()
        );

        if (!preview.visibleBlocks().isEmpty()) {
            renderGhostBlocks(preview.visibleBlocks(), poseStack, buffer);

            if (preview.bounds() != null) {
                VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());

                LevelRenderer.renderLineBox(
                        poseStack,
                        lineConsumer,
                        preview.bounds(),
                        AREA_RED,
                        AREA_GREEN,
                        AREA_BLUE,
                        AREA_ALPHA
                );
            }
        } else {
            VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());

            LevelRenderer.renderLineBox(
                    poseStack,
                    lineConsumer,
                    getDefaultArea(blockEntity),
                    AREA_RED,
                    AREA_GREEN,
                    AREA_BLUE,
                    0.35F
            );
        }

        poseStack.popPose();
    }

    private CachedPreview getCachedPreview(Printer3DBlockEntity blockEntity) {
        long gameTime = blockEntity.getLevel() == null
                ? 0L
                : blockEntity.getLevel().getGameTime();

        CachedPreview cached = PREVIEW_CACHE.get(blockEntity);
        if (cached != null && gameTime - cached.updatedAt() < CACHE_REFRESH_TICKS) {
            return cached;
        }

        List<BlockPlacement> allBlocks = blockEntity.getPreviewPlacements();
        CachedPreview refreshed = buildSurfacePreview(allBlocks, gameTime);
        PREVIEW_CACHE.put(blockEntity, refreshed);
        return refreshed;
    }

    private CachedPreview buildSurfacePreview(
            List<BlockPlacement> allBlocks,
            long gameTime
    ) {
        Map<BlockPos, BlockPlacement> occupied = new HashMap<>();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPlacement placement : allBlocks) {
            if (placement.state().isAir()) {
                continue;
            }

            BlockPos pos = placement.pos();
            occupied.put(pos, placement);

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        if (occupied.isEmpty()) {
            return new CachedPreview(List.of(), null, gameTime);
        }

        List<BlockPlacement> visibleBlocks = new ArrayList<>();

        for (BlockPlacement placement : occupied.values()) {
            BlockPos pos = placement.pos();

            if (isSurfaceBlock(pos, occupied)) {
                visibleBlocks.add(placement);
            }
        }

        AABB bounds = new AABB(
                minX,
                minY,
                minZ,
                maxX + 1,
                maxY + 1,
                maxZ + 1
        );

        return new CachedPreview(visibleBlocks, bounds, gameTime);
    }

    private boolean isSurfaceBlock(
            BlockPos pos,
            Map<BlockPos, BlockPlacement> occupied
    ) {
        for (Direction direction : Direction.values()) {
            if (!occupied.containsKey(pos.relative(direction))) {
                return true;
            }
        }

        return false;
    }

    private void renderGhostBlocks(
            List<BlockPlacement> visibleBlocks,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        MultiBufferSource ghostBuffer = new GhostBufferSource(buffer, GHOST_ALPHA_MULTIPLIER);

        // No sampling: every visible surface block is rendered.
        for (BlockPlacement placement : visibleBlocks) {
            BlockPos blockPos = placement.pos();
            BlockState state = placement.state();

            poseStack.pushPose();
            poseStack.translate(
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
            );

            try {
                blockRenderer.renderSingleBlock(
                        state,
                        poseStack,
                        ghostBuffer,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        RenderType.translucent()
                );
            } catch (Exception ignored) {
            }

            poseStack.popPose();
        }
    }

    private AABB getDefaultArea(Printer3DBlockEntity blockEntity) {
        BlockPos printer = blockEntity.getBlockPos();
        Rotation rotation = getRotation(blockEntity);

        int[][] corners = new int[][]{
                rotatePoint(1, 1, rotation),
                rotatePoint(5, 1, rotation),
                rotatePoint(1, 5, rotation),
                rotatePoint(5, 5, rotation)
        };

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int[] corner : corners) {
            minX = Math.min(minX, corner[0]);
            minZ = Math.min(minZ, corner[1]);
            maxX = Math.max(maxX, corner[0]);
            maxZ = Math.max(maxZ, corner[1]);
        }

        return new AABB(
                printer.getX() + minX,
                printer.getY(),
                printer.getZ() + minZ,
                printer.getX() + maxX + 1,
                printer.getY() + 6,
                printer.getZ() + maxZ + 1
        );
    }

    private Rotation getRotation(Printer3DBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(Printer3DBlock.FACING)
                ? state.getValue(Printer3DBlock.FACING)
                : Direction.NORTH;

        return switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private int[] rotatePoint(int x, int z, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new int[]{x, z};
            case CLOCKWISE_90 -> new int[]{-z, x};
            case CLOCKWISE_180 -> new int[]{-x, -z};
            case COUNTERCLOCKWISE_90 -> new int[]{z, -x};
        };
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private record CachedPreview(
            List<BlockPlacement> visibleBlocks,
            AABB bounds,
            long updatedAt
    ) {
    }

    private static final class GhostBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final float alphaMultiplier;

        private GhostBufferSource(
                MultiBufferSource delegate,
                float alphaMultiplier
        ) {
            this.delegate = delegate;
            this.alphaMultiplier = alphaMultiplier;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new GhostVertexConsumer(
                    delegate.getBuffer(renderType),
                    alphaMultiplier
            );
        }
    }

    private static final class GhostVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alphaMultiplier;

        private GhostVertexConsumer(
                VertexConsumer delegate,
                float alphaMultiplier
        ) {
            this.delegate = delegate;
            this.alphaMultiplier = alphaMultiplier;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, scaleAlpha(alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, scaleAlpha(alpha));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }

        private int scaleAlpha(int alpha) {
            int scaled = (int) (alpha * alphaMultiplier);
            return Math.max(0, Math.min(scaled, 255));
        }
    }
}
