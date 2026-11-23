package com.example.zitraksmode.network.scooter;

import com.example.zitraksmode.entities.ScooterEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScooterInputPacket {

    private final int entityId;
    private final boolean forward;
    private final boolean back;
    private final boolean left;
    private final boolean right;

    public ScooterInputPacket(int entityId, boolean forward, boolean back, boolean left, boolean right) {
        this.entityId = entityId;
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
    }

    public static void encode(ScooterInputPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeBoolean(packet.forward);
        buf.writeBoolean(packet.back);
        buf.writeBoolean(packet.left);
        buf.writeBoolean(packet.right);
    }

    public static ScooterInputPacket decode(FriendlyByteBuf buf) {
        return new ScooterInputPacket(buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ScooterInputPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender == null) {
            ctx.setPacketHandled(true);
            return;
        }

        Entity entity = sender.level.getEntity(packet.entityId);
        if (entity instanceof ScooterEntity scooter && scooter.getDriver() == sender) {
            scooter.setInputState(packet.forward, packet.back, packet.left, packet.right);
        }

        ctx.setPacketHandled(true);
    }
}
