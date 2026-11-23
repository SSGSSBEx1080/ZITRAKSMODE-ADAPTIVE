package com.example.zitraksmode.client;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.events.RabbitRecoilHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only full-body 180° flip while rabbit B is lying on its back. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class RabbitRecoilClientRenderer {
    private RabbitRecoilClientRenderer() {
    }

    @SubscribeEvent
    public static void beforeRabbitRender(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Rabbit rabbit)) return;
        if (!RabbitRecoilHandler.isUpsideDown(rabbit)) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        double halfHeight = rabbit.getBbHeight() * 0.5D;
        poseStack.translate(0.0D, halfHeight, 0.0D);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, -halfHeight, 0.0D);
    }

    @SubscribeEvent
    public static void afterRabbitRender(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Rabbit rabbit)) return;
        if (RabbitRecoilHandler.isUpsideDown(rabbit)) {
            event.getPoseStack().popPose();
        }
    }
}
