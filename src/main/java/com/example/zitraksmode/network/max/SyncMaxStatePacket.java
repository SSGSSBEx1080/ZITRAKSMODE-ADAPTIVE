package com.example.zitraksmode.network.max;

import com.example.zitraksmode.max.MaxChatMessage;
import com.example.zitraksmode.max.MaxClientState;
import com.example.zitraksmode.max.MaxFileAttachment;
import com.example.zitraksmode.max.MaxGiftOffer;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import com.example.zitraksmode.max.MaxUserProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncMaxStatePacket {

    private final UUID selfId;
    private final List<MaxUserProfile> profiles;
    private final List<MaxChatMessage> messages;
    private final List<MaxFileAttachment> attachments;
    private final List<MaxGiftOffer> gifts;

    public SyncMaxStatePacket(UUID selfId, List<MaxUserProfile> profiles, List<MaxChatMessage> messages, List<MaxFileAttachment> attachments, List<MaxGiftOffer> gifts) {
        this.selfId = selfId;
        this.profiles = profiles;
        this.messages = messages;
        this.attachments = attachments;
        this.gifts = gifts;
    }

    public static SyncMaxStatePacket fromServerState(ServerPlayer player, MaxMessengerSavedData data) {
        if (player.getServer() != null) {
            data.ensureOnlineProfiles(player.getServer());
        }
        return new SyncMaxStatePacket(
                player.getUUID(),
                data.getProfiles(),
                data.getMessagesForUser(player.getUUID()),
                data.getAttachmentMetadataForUser(player.getUUID()),
                data.getGiftsForUser(player.getUUID())
        );
    }

    public static void encode(SyncMaxStatePacket packet, FriendlyByteBuf buf) {
        CompoundTag root = new CompoundTag();
        root.putUUID("SelfId", packet.selfId);

        ListTag profilesTag = new ListTag();
        for (MaxUserProfile profile : packet.profiles) {
            profilesTag.add(profile.save());
        }
        root.put("Profiles", profilesTag);

        ListTag messagesTag = new ListTag();
        for (MaxChatMessage message : packet.messages) {
            messagesTag.add(message.save());
        }
        root.put("Messages", messagesTag);

        ListTag attachmentsTag = new ListTag();
        for (MaxFileAttachment attachment : packet.attachments) {
            attachmentsTag.add(attachment.saveMetadata());
        }
        root.put("Attachments", attachmentsTag);

        ListTag giftsTag = new ListTag();
        for (MaxGiftOffer gift : packet.gifts) {
            giftsTag.add(gift.save());
        }
        root.put("Gifts", giftsTag);

        buf.writeNbt(root);
    }

    public static SyncMaxStatePacket decode(FriendlyByteBuf buf) {
        CompoundTag root = buf.readNbt();
        if (root == null) {
            return new SyncMaxStatePacket(new UUID(0L, 0L), List.of(), List.of(), List.of(), List.of());
        }

        List<MaxUserProfile> profiles = new ArrayList<>();
        ListTag profilesTag = root.getList("Profiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < profilesTag.size(); i++) {
            profiles.add(MaxUserProfile.load(profilesTag.getCompound(i)));
        }

        List<MaxChatMessage> messages = new ArrayList<>();
        ListTag messagesTag = root.getList("Messages", Tag.TAG_COMPOUND);
        for (int i = 0; i < messagesTag.size(); i++) {
            messages.add(MaxChatMessage.load(messagesTag.getCompound(i)));
        }

        List<MaxFileAttachment> attachments = new ArrayList<>();
        ListTag attachmentsTag = root.getList("Attachments", Tag.TAG_COMPOUND);
        for (int i = 0; i < attachmentsTag.size(); i++) {
            attachments.add(MaxFileAttachment.loadMetadata(attachmentsTag.getCompound(i)));
        }

        List<MaxGiftOffer> gifts = new ArrayList<>();
        ListTag giftsTag = root.getList("Gifts", Tag.TAG_COMPOUND);
        for (int i = 0; i < giftsTag.size(); i++) {
            gifts.add(MaxGiftOffer.load(giftsTag.getCompound(i)));
        }

        return new SyncMaxStatePacket(root.getUUID("SelfId"), profiles, messages, attachments, gifts);
    }

    public static void handle(SyncMaxStatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncMaxStatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        MaxClientState.replace(packet.selfId, packet.profiles, packet.messages, packet.attachments, packet.gifts);
    }
}
