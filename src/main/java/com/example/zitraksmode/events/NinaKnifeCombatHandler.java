package com.example.zitraksmode.events;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.NinaKnifeItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Backstab x2 and side strike x1.5 for Nina's knife. */
@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NinaKnifeCombatHandler {
    private NinaKnifeCombatHandler() {
    }

    @SubscribeEvent
    public static void applyDirectionalKnifeDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        LivingEntity victim = event.getEntity();

        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof NinaKnifeItem)) return;
        if (victim == attacker) return;

        Vec3 towardAttacker = attacker.position().subtract(victim.position());
        towardAttacker = new Vec3(towardAttacker.x, 0.0D, towardAttacker.z);
        if (towardAttacker.lengthSqr() < 0.001D) return;
        towardAttacker = towardAttacker.normalize();

        float yawRadians = victim.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 victimForward = new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
        double directionDot = victimForward.dot(towardAttacker);

        if (directionDot <= -0.5D) {
            event.setAmount(event.getAmount() * 2.0F);
        } else if (Math.abs(directionDot) < 0.5D) {
            event.setAmount(event.getAmount() * 1.5F);
        }
    }

    /** A knife has no own push force, including sprint knockback. */
    @SubscribeEvent
    public static void cancelKnifeKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity().getLastHurtByMob() instanceof Player attacker)) return;
        if (attacker.getMainHandItem().getItem() instanceof NinaKnifeItem) {
            event.setCanceled(true);
        }
    }
}
