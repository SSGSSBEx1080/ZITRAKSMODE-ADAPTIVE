package com.example.zitraksmode.network;

import com.example.zitraksmode.caps.SandevistanCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.function.Supplier;

public class SandevistanSyncPacket {
    private final int entityId;
    private final CompoundTag nbt;

    public SandevistanSyncPacket(int entityId, CompoundTag nbt) {
        this.entityId = entityId;
        this.nbt = nbt;
    }

    public SandevistanSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeNbt(nbt);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Ищем игрока по ID и применяем к нему эффекты (работает для всех на сервере)
                Entity e = mc.level.getEntity(entityId);
                if (e instanceof Player) {
                    Player targetPlayer = (Player) e;
                    SandevistanCapability.get(targetPlayer).ifPresent(cap -> cap.loadNBTData(nbt));
                }
            }
        });
        context.setPacketHandled(true);
    }
}