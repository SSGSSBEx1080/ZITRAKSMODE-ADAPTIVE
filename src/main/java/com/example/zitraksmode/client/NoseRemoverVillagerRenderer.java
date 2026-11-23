package com.example.zitraksmode.client;

import com.example.zitraksmode.villager.ModVillagers; // ← поправь импорт под свой пакет, где лежит ModVillagers
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Кастомный рендерер жителя, который скрывает нос-куб у определённых профессий.
 * Сейчас отключает нос только у "demolitionist".
 *
 * Регистрация через EntityRenderersEvent.RegisterRenderers (см. ниже).
 */
@OnlyIn(Dist.CLIENT)
public class NoseRemoverVillagerRenderer extends VillagerRenderer {

    public NoseRemoverVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(Villager villager, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        VillagerProfession profession = villager.getVillagerData().getProfession();
        boolean isDemolitionist = profession == ModVillagers.DEMOLITIONIST.get();

        ModelPart nose = null;
        boolean wasVisible = true;

        if (isDemolitionist) {
            // Достаём ModelPart носа из модели жителя
            try {
                ModelPart head = this.getModel().getHead();
                if (head.hasChild("nose")) {
                    nose = head.getChild("nose");
                    wasVisible = nose.visible;
                    nose.visible = false;
                }
            } catch (Exception ignored) {
                // На всякий случай — если структура модели нестандартная, просто рендерим как есть
            }
        }

        super.render(villager, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // Возвращаем видимость носа обратно (важно — модель шарится между всеми жителями!)
        if (nose != null) {
            nose.visible = wasVisible;
        }
    }
}
