package com.example.zitraksmode.client.katana;

import com.example.zitraksmode.items.katana.KatanaItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class KatanaRenderer extends GeoItemRenderer<KatanaItem> {
    
    public KatanaRenderer() {
        super(new KatanaModel());
    }
}