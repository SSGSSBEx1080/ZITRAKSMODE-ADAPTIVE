package com.example.zitraksmode.client.katana;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.katana.KatanaItem;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Скрывает ванильные руки игрока в 3-м лице,
 * если он держит катану.
 *
 * Работает и для себя в F5, и для других игроков.
 *
 * На first-person это НЕ влияет.
 */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, value = Dist.CLIENT)
public class KatanaThirdPersonArmHider {

    private static final Map<UUID, ArmState> SAVED_STATES = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();

        if (!isHoldingKatana(player)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        // Сохраняем текущее состояние, чтобы потом вернуть
        SAVED_STATES.put(player.getUUID(), new ArmState(
                model.leftArm.visible,
                model.rightArm.visible,
                model.leftSleeve.visible,
                model.rightSleeve.visible
        ));

        // Скрываем руки и рукава
        model.leftArm.visible = false;
        model.rightArm.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        ArmState saved = SAVED_STATES.remove(player.getUUID());

        if (saved == null) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        // Возвращаем как было
        model.leftArm.visible = saved.leftArmVisible;
        model.rightArm.visible = saved.rightArmVisible;
        model.leftSleeve.visible = saved.leftSleeveVisible;
        model.rightSleeve.visible = saved.rightSleeveVisible;
    }

    private static boolean isHoldingKatana(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.getItem() instanceof KatanaItem
                || offHand.getItem() instanceof KatanaItem;
    }

    private static class ArmState {
        private final boolean leftArmVisible;
        private final boolean rightArmVisible;
        private final boolean leftSleeveVisible;
        private final boolean rightSleeveVisible;

        private ArmState(boolean leftArmVisible, boolean rightArmVisible,
                         boolean leftSleeveVisible, boolean rightSleeveVisible) {
            this.leftArmVisible = leftArmVisible;
            this.rightArmVisible = rightArmVisible;
            this.leftSleeveVisible = leftSleeveVisible;
            this.rightSleeveVisible = rightSleeveVisible;
        }
    }
}