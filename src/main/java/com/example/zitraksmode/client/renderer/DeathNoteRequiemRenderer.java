package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.client.models.DeathNoteRequiemModel;
import com.example.zitraksmode.items.DeathNoteRequiemItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class DeathNoteRequiemRenderer extends GeoItemRenderer<DeathNoteRequiemItem> {

    public DeathNoteRequiemRenderer() {
        super(new DeathNoteRequiemModel());
    }
}