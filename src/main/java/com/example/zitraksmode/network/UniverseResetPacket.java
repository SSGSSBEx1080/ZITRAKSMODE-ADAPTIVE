package com.example.zitraksmode.network;

import com.example.zitraksmode.client.UniverseResetOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UniverseResetPacket {
    
    public UniverseResetPacket() {
    }

    public UniverseResetPacket(FriendlyByteBuf buf) {
        // Пакет пустой, просто сигнал
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Ничего не отправляем
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Обрабатываем на клиенте
            handleClientSide();
        });
        context.setPacketHandled(true);
        return true;
    }

    private void handleClientSide() {
        // Эта часть выполнится на клиенте
        // Проверяем что мы на клиенте через DistExecutor
        net.minecraftforge.api.distmarker.Dist dist = net.minecraftforge.fml.loading.FMLEnvironment.dist;
        if (dist.isClient()) {
            // Запускаем белую вспышку
            UniverseResetOverlay.triggerFlash();
        }
    }
}
