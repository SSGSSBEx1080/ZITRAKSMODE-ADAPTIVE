package com.example.zitraksmode.client.armor;

import com.example.zitraksmode.items.AqualungItem;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

public class AqualungArmorRenderer extends GeoArmorRenderer<AqualungItem> {

    public AqualungArmorRenderer() {
        super(new AqualungArmorModel());
        
        // Говорим GeckoLib, как именно называются папки в твоём Blockbench
        this.headBone = "head";
        this.bodyBone = "body";
        this.rightArmBone = "right_arm";
        this.leftArmBone = "left_arm";
        this.rightLegBone = "right_leg";
        this.leftLegBone = "left_leg";
        this.rightBootBone = "right_boot";
        this.leftBootBone = "left_boot";
    }
}