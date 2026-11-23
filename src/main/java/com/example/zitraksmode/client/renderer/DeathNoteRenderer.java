package com.example.zitraksmode.client.renderer;

import com.example.zitraksmode.client.models.DeathNoteModel;
import com.example.zitraksmode.items.DeathNoteItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class DeathNoteRenderer extends GeoItemRenderer<DeathNoteItem> {
    public DeathNoteRenderer() {
        super(new DeathNoteModel());
    }
}