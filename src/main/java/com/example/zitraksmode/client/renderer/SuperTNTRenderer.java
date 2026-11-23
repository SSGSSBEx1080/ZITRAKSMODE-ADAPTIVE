package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.entities.SuperTNTPrimedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import com.example.zitraksmode.ModBlocks;

public class SuperTNTRenderer extends EntityRenderer<SuperTNTPrimedEntity> {
    
    private final BlockRenderDispatcher blockRenderer;
    
    public SuperTNTRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }
    
    @Override
    public void render(SuperTNTPrimedEntity entity, float entityYaw, float partialTicks, 
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.0D);
        
        int fuse = entity.getFuse();
        if (fuse - partialTicks + 1.0F < 10.0F) {
            // Мигание перед взрывом
            float f = 1.0F - (fuse - partialTicks + 1.0F) / 10.0F;
            f = Mth.clamp(f, 0.0F, 1.0F);
            f = f * f;
            f = f * f;
            float scale = 1.0F + f * 0.3F;
            poseStack.scale(scale, scale, scale);
        }
        
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5D, -0.5D, 0.5D);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));
        
        // Рендерим блок Супер TNT
        TntMinecartRenderer.renderWhiteSolidBlock(
            this.blockRenderer,
            ModBlocks.SUPER_TNT.get().defaultBlockState(),
            poseStack,
            buffer,
            packedLight,
            fuse / 5 % 2 == 0
        );
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    @Override
    public ResourceLocation getTextureLocation(SuperTNTPrimedEntity entity) {
        return new ResourceLocation("minecraft", "textures/block/tnt_side.png");
    }
}
