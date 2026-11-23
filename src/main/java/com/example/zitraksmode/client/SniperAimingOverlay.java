package com.example.zitraksmode.client;

import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.items.SniperItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SniperAimingOverlay implements IGuiOverlay {

    public static final SniperAimingOverlay INSTANCE = new SniperAimingOverlay();

    private static final ResourceLocation SCOPE_TEXTURE =
            new ResourceLocation("zitraksmode", "textures/gui/sniper_scope.png");

    private static final ResourceLocation SCOPE_OVERLAY_ID =
            new ResourceLocation("zitraksmode", "sniper_scope");

    private static final int SCOPE_FOV = 30;

    private boolean isScoping = false;
    private int originalFOV = 70;

    private SniperAimingOverlay() {
    }

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            if (isScoping) {
                stopScoping(minecraft);
            }
            return;
        }

        boolean shouldScope = isHoldingSniperAndAiming(player);

        if (shouldScope && !isScoping) {
            startScoping(minecraft);
        } else if (!shouldScope && isScoping) {
            stopScoping(minecraft);
        }

        if (isScoping) {
            renderSniperScope(poseStack, screenWidth, screenHeight);
        }
    }

    @SubscribeEvent
    public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!isScoping) {
            return;
        }

        if (event.getOverlay() != null && SCOPE_OVERLAY_ID.equals(event.getOverlay().id())) {
            return;
        }

        event.setCanceled(true);
    }

    private void startScoping(Minecraft minecraft) {
        isScoping = true;
        originalFOV = minecraft.options.fov().get();
        minecraft.options.fov().set(SCOPE_FOV);
        playScopeSound(true);
    }

    private void stopScoping(Minecraft minecraft) {
        isScoping = false;
        minecraft.options.fov().set(originalFOV);
        playScopeSound(false);
    }

    private boolean isHoldingSniperAndAiming(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof SniperItem;
    }

    private void renderSniperScope(PoseStack poseStack, int screenWidth, int screenHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SCOPE_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();

        GuiComponent.blit(
                poseStack,
                0,
                0,
                0,
                0,
                screenWidth,
                screenHeight,
                screenWidth,
                screenHeight
        );

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void playScopeSound(boolean scopeIn) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            if (scopeIn) {
                minecraft.player.playSound(ModSounds.SCOPE_IN.get(), 0.5F, 1.0F);
            } else {
                minecraft.player.playSound(ModSounds.SCOPE_OUT.get(), 0.5F, 1.0F);
            }
        }
    }

    public boolean isScoping() {
        return isScoping;
    }
}