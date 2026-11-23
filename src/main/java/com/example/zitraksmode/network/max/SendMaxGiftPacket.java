package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxMessengerSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SendMaxGiftPacket {

    private final UUID recipientId;
    private final String text;
    private final long replyToMessageId;
    private final List<ItemStack> stacks;

    public SendMaxGiftPacket(UUID recipientId, String text, long replyToMessageId, List<ItemStack> stacks) {
        this.recipientId = recipientId;
        this.text = text;
        this.replyToMessageId = replyToMessageId;
        this.stacks = stacks;
    }

    public static void encode(SendMaxGiftPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.recipientId);
        buf.writeUtf(packet.text == null ? "" : packet.text, 4096);
        buf.writeLong(packet.replyToMessageId);
        buf.writeInt(packet.stacks == null ? 0 : packet.stacks.size());
        if (packet.stacks != null) {
            for (ItemStack stack : packet.stacks) {
                buf.writeNbt(stack.save(new CompoundTag()));
            }
        }
    }

    public static SendMaxGiftPacket decode(FriendlyByteBuf buf) {
        UUID recipientId = buf.readUUID();
        String text = buf.readUtf(4096);
        long replyTo = buf.readLong();
        int size = buf.readInt();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.of(buf.readNbt()));
        }
        return new SendMaxGiftPacket(recipientId, text, replyTo, stacks);
    }

    public static void handle(SendMaxGiftPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }
            MaxMessengerSavedData data = MaxMessengerSavedData.get(sender.getServer());
            try {
                data.sendGiftMessage(sender, packet.recipientId, packet.stacks, packet.text, packet.replyToMessageId);
                MaxSyncHelper.syncPair(sender.getServer(), data, sender.getUUID(), packet.recipientId);
            } catch (Exception ignored) {
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
