package com.example.zitraksmode.network.scooter;

import com.example.zitraksmode.entities.ScooterEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScooterActionPacket {
    public static final int HORN = 0;
    public static final int PICK_UP = 1;

    private final int entityId;
    private final int action;

    public ScooterActionPacket(int entityId, int action) {
        this.entityId = entityId;
        this.action = action;
    }

    public static void encode(ScooterActionPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeVarInt(packet.action);
    }

    public static ScooterActionPacket decode(FriendlyByteBuf buf) {
        return new ScooterActionPacket(buf.readInt(), buf.readVarInt());
    }

    public static void handle(ScooterActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender == null) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            Entity entity = sender.level.getEntity(packet.entityId);
            if (entity instanceof ScooterEntity scooter) {
                if (packet.action == HORN && scooter.getDriver() == sender) {
                    scooter.honk();
                } else if (packet.action == PICK_UP) {
                    scooter.packUpToPlayer(sender);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}