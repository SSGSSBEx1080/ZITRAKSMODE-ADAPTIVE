package com.example.zitraksmode.client;

import com.example.zitraksmode.items.AqualungItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class AqualungHUD implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof AqualungItem)) return;

        float percent = AqualungItem.getAirPercent(chest);
        int pct = Math.round(percent * 100);

        int barW = 50;
        int barH = 5;
        int x = 10;
        int y = height - 42; // над полоской здоровья

        int color = percent > 0.5f ? 0xFF00CCFF :
                    percent > 0.25f ? 0xFFFFCC00 :
                                      0xFFFF4444;

        // Фон
        GuiComponent.fill(poseStack, x, y, x + barW, y + barH, 0xAA000000);
        // Заполнение
        int fill = (int) (barW * percent);
        GuiComponent.fill(poseStack, x, y, x + fill, y + barH, color);
        // Проценты
        mc.font.draw(poseStack, pct + "%", x + barW + 3, y - 1, color);
    }
}