package com.example.zitraksmode.events;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.world.MahoragaOutpostSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MahoragaOutpostHandler {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) return;
        ServerPlayer player = (ServerPlayer) event.player;
        if (!(player.level instanceof ServerLevel serverLevel)) return;

        MahoragaOutpostSpawner.OutpostSavedData data = serverLevel.getDataStorage()
            .computeIfAbsent(MahoragaOutpostSpawner.OutpostSavedData::new, MahoragaOutpostSpawner.OutpostSavedData::new, "mahoraga_outpost_status");

        if (data.isPlaced()) {
            double distSq = player.distanceToSqr(data.getOutpostPos().getX() + 0.5, data.getOutpostPos().getY() + 0.5, data.getOutpostPos().getZ() + 0.5);
            if (distSq < 36.0) { // 6 блоков радиус
                ZitraksMode.FIND_MAHORAGA_OUTPOST.trigger(player);
            }
        }
    }
}