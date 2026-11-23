package com.example.zitraksmode.network;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class SandevistanNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ZitraksMode.MODID, "sandevistan_network"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        INSTANCE.messageBuilder(SandevistanTogglePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SandevistanTogglePacket::new)
            .encoder(SandevistanTogglePacket::toBytes)
            .consumerMainThread(SandevistanTogglePacket::handle)
            .add();

        INSTANCE.messageBuilder(SandevistanSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SandevistanSyncPacket::new)
            .encoder(SandevistanSyncPacket::toBytes)
            .consumerMainThread(SandevistanSyncPacket::handle)
            .add();

        INSTANCE.messageBuilder(SandevistanFreezePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SandevistanFreezePacket::new)
            .encoder(SandevistanFreezePacket::toBytes)
            .consumerMainThread(SandevistanFreezePacket::handle)
            .add();
    }
    
    public static void sendToClient(Object packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    
    // ОТПРАВЛЯЕМ ПАКЕТ ИГРОКУ И ВСЕМ, КТО ЕГО ВИДИТ
    public static void sendToTrackingAndSelf(Object packet, Entity entity) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
    
    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}