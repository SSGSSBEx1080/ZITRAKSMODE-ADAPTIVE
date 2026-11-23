package com.example.zitraksmode.client.models;

import com.example.zitraksmode.items.DeathNoteItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class DeathNoteModel extends AnimatedGeoModel<DeathNoteItem> {
    @Override
    public ResourceLocation getModelResource(DeathNoteItem object) {
        return new ResourceLocation("zitraksmode", "geo/death_note.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(DeathNoteItem object) {
        return new ResourceLocation("zitraksmode", "textures/item/death_note.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(DeathNoteItem object) {
        return new ResourceLocation("zitraksmode", "animations/death_note.animation.json");
    }
}