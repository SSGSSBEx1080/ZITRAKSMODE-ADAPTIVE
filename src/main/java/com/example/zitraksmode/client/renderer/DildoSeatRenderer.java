package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.entities.DildoSeatEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Пустой рендерер для DildoSeatEntity
 * Сам entity невидимый - игрок просто сидит на нём
 */
public class DildoSeatRenderer extends EntityRenderer<DildoSeatEntity> {
    
    public DildoSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public ResourceLocation getTextureLocation(DildoSeatEntity entity) {
        // Возвращаем любую текстуру (entity всё равно невидимый)
        return new ResourceLocation("minecraft", "textures/block/stone.png");
    }
}
