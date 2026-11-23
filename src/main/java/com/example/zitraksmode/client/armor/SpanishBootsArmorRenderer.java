package com.example.zitraksmode.client.armor;

import com.example.zitraksmode.items.SpanishBootsItem;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

public class SpanishBootsArmorRenderer extends GeoArmorRenderer<SpanishBootsItem> {

    public SpanishBootsArmorRenderer() {
        super(new SpanishBootsArmorModel());

        this.headBone = null;
        this.bodyBone = null;
        this.rightArmBone = null;
        this.leftArmBone = null;

        this.rightLegBone = "right_boot";
        this.leftLegBone = "left_boot";

        this.rightBootBone = "right_boot";
        this.leftBootBone = "left_boot";
    }
}