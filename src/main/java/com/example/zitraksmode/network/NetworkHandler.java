package com.example.zitraksmode.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.example.zitraksmode.ZitraksMode;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    // Инициализируем INSTANCE как null. 
    // Создание SimpleChannel должно происходить в FMLCommonSetupEvent.
    public static SimpleChannel INSTANCE; 
    
    private static int ID = 0;
    
    // Вспомогательный метод для инкремента ID
    private static int nextID() {
        return ID++;
    }

    // Главный метод регистрации. Вызываем его в FMLCommonSetupEvent.
    public static void register() {
        // Защита от повторной инициализации
        if (INSTANCE != null) return; 

        // Инициализация канала сети - теперь происходит во время setup
        INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ZitraksMode.MODID, "universe_reset"),   // было "main"
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
        
        // Регистрируем существующий пакет
        INSTANCE.registerMessage(nextID(), 
            TriggerExplosionPacket.class,
            TriggerExplosionPacket::toBytes, 
            TriggerExplosionPacket::new,
            TriggerExplosionPacket::handle);
        
        // ===== MADE IN HEAVEN =====
        // Регистрируем пакет Universe Reset
        INSTANCE.registerMessage(nextID(),
            UniverseResetPacket.class,
            UniverseResetPacket::toBytes,
            UniverseResetPacket::new,
            UniverseResetPacket::handle);
    }
}
