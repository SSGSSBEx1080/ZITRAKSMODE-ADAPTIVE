package com.example.zitraksmode.network;

import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.util.LogiaGaugeCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Separate channel so it cannot collide with the mod's existing packet systems. */
public final class LogiaNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZitraksMode.MODID, "logia"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;
    private static boolean registered;

    private LogiaNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(SyncGaugePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncGaugePacket::encode)
                .decoder(SyncGaugePacket::decode)
                .consumerNetworkThread(SyncGaugePacket::handle)
                .add();
    }

    public static void sync(ServerPlayer player, LogiaGaugeCapability.LogiaGauge data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncGaugePacket(data.getGauge(), data.getKizaruCooldown(),
                        data.getWhitebeardLockTicks(), data.getWhitebeardCooldownTicks()));
    }

    public static final class SyncGaugePacket {
        private final int gauge;
        private final int kizaruCooldown;
        private final int whitebeardLockTicks;
        private final int whitebeardCooldownTicks;

        private SyncGaugePacket(int gauge, int kizaruCooldown, int whitebeardLockTicks,
                                int whitebeardCooldownTicks) {
            this.gauge = gauge;
            this.kizaruCooldown = kizaruCooldown;
            this.whitebeardLockTicks = whitebeardLockTicks;
            this.whitebeardCooldownTicks = whitebeardCooldownTicks;
        }

        private static void encode(SyncGaugePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.gauge);
            buffer.writeVarInt(packet.kizaruCooldown);
            buffer.writeVarInt(packet.whitebeardLockTicks);
            buffer.writeVarInt(packet.whitebeardCooldownTicks);
        }

        private static SyncGaugePacket decode(FriendlyByteBuf buffer) {
            return new SyncGaugePacket(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        public int gauge() {
            return gauge;
        }

        public int kizaruCooldown() {
            return kizaruCooldown;
        }

        public int whitebeardLockTicks() {
            return whitebeardLockTicks;
        }

        public int whitebeardCooldownTicks() {
            return whitebeardCooldownTicks;
        }

        private static void handle(SyncGaugePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.example.zitraksmode.client.LogiaClientPacketHandler.apply(packet)));
            context.setPacketHandled(true);
        }
    }
}
