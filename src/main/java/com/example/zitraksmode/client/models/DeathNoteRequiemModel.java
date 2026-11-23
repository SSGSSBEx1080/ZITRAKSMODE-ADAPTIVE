package com.example.zitraksmode.client.models;

import com.example.zitraksmode.items.DeathNoteRequiemItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class DeathNoteRequiemModel extends AnimatedGeoModel<DeathNoteRequiemItem> {
    @Override
    public ResourceLocation getModelResource(DeathNoteRequiemItem object) {
        return new ResourceLocation("zitraksmode", "geo/death_note_requiem.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DeathNoteRequiemItem object) {
        return new ResourceLocation("zitraksmode", "textures/item/death_note_requiem.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DeathNoteRequiemItem object) {
        return new ResourceLocation("zitraksmode", "animations/death_note_requiem.animation.json");
    }
}