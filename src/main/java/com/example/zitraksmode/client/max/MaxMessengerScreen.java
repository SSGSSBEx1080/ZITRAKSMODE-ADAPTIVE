package com.example.zitraksmode.client.max;

import com.example.zitraksmode.max.MaxChatMessage;
import com.example.zitraksmode.max.MaxClientState;
import com.example.zitraksmode.max.MaxClientTransferState;
import com.example.zitraksmode.max.MaxFileAttachment;
import com.example.zitraksmode.max.MaxGiftOffer;
import com.example.zitraksmode.max.MaxUserProfile;
import com.example.zitraksmode.network.max.DeleteMaxMessagePacket;
import com.example.zitraksmode.network.max.EditMaxMessagePacket;
import com.example.zitraksmode.network.max.FinishMaxAttachmentUploadPacket;
import com.example.zitraksmode.network.max.ForwardMaxMessagePacket;
import com.example.zitraksmode.network.max.MarkMaxConversationReadPacket;
import com.example.zitraksmode.network.max.MaxAttachmentUploadChunkPacket;
import com.example.zitraksmode.network.max.MaxPackets;
import com.example.zitraksmode.network.max.RegisterMaxProfilePacket;
import com.example.zitraksmode.network.max.RequestMaxAttachmentDownloadPacket;
import com.example.zitraksmode.network.max.RequestMaxSyncPacket;
import com.example.zitraksmode.network.max.RespondMaxGiftPacket;
import com.example.zitraksmode.network.max.SendMaxGiftPacket;
import com.example.zitraksmode.network.max.SendMaxMessagePacket;
import com.example.zitraksmode.network.max.StartMaxAttachmentUploadPacket;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.registries.ForgeRegistries;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MaxMessengerScreen extends Screen {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("d MMMM", new Locale("ru", "RU"));
    private static final List<String> STOCK_AVATAR_IDS = List.of(
            "stock_steve",
            "stock_alex",
            "stock_smile",
            "stock_cat",
            "stock_skull"
    );
    private static final String UPLOAD_AVATAR_ID = "__upload__";

    private final List<ContactRow> contactRows = new ArrayList<>();
    private final List<AvatarClickBox> avatarBoxes = new ArrayList<>();
    private final List<ShowcaseClickBox> showcaseClickBoxes = new ArrayList<>();
    private final List<MessageHitBox> messageHitBoxes = new ArrayList<>();
    private final List<MessageActionBox> messageActionBoxes = new ArrayList<>();
    private final List<FileButtonBox> fileButtonBoxes = new ArrayList<>();
    private final List<GiftButtonBox> giftButtonBoxes = new ArrayList<>();
    private final List<GiftDraftSlotBox> giftDraftSlotBoxes = new ArrayList<>();
    private final List<GiftInventoryBox> giftInventoryBoxes = new ArrayList<>();
    private final List<AttachActionBox> attachActionBoxes = new ArrayList<>();
    private final Map<String, ResourceLocation> avatarTextureCache = new HashMap<>();
    private final Map<String, ResourceLocation> attachmentPreviewTextureCache = new HashMap<>();
    private final Map<String, int[]> attachmentPreviewSizes = new HashMap<>();

    private EditBox displayNameBox;
    private EditBox descriptionBox;
    private EditBox searchBox;
    private String currentSearchQuery = "";
    private List<Long> searchResults = new ArrayList<>();
    private int currentSearchResultIndex = -1;
    private long jumpToMessageId = 0L;

    private Button saveProfileButton;
    private Button cancelProfileButton;
    private Button sendButton;
    private Button profileBackButton;
    private Button profileEditButton;

    private ViewMode viewMode = ViewMode.CHATS;
    private UUID selectedContactId;
    private UUID viewedProfileId;

    private int contactScroll = 0;
    private int messageScroll = 0;
    private float smoothMessageScroll = -1;
    private long smoothScrollStartTime = 0;
    private int smoothScrollStartPos = 0;
    private Rect scrollToBottomBox = null;

    private String selectedAvatarId = "stock_steve";
    private String selectedShowcaseItemId = "";
    private String chatDraft = "";
    private MaxMessageInputBox messageInput;
    private String statusLine = "";

    private boolean chatInputFocused = false;
    private boolean showcasePickerOpen = false;
    private boolean awaitingAvatarDrop = false;
    private boolean awaitingChatFileDrop = false;
    private boolean avatarCropOpen = false;
    private boolean avatarCropDragging = false;
    private boolean profileDraftLoaded = false;
    private boolean forceScrollToBottom = true;
    private boolean attachMenuOpen = false;
    private boolean giftComposerOpen = false;
    private long replyToMessageId = 0L;
    private long editingMessageId = 0L;
    private long forwardingMessageId = 0L;
    private long contextMenuMessageId = 0L;
    private String openedImageAttachmentId = null;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private int lastKnownConversationMaxScroll = 0;
    private long lastKnownConversationTailMessageId = 0L;
    private UUID lastKnownConversationId = null;
    private PendingAttachment pendingAttachment = null;
    private final List<ItemStack> giftDraftStacks = new ArrayList<>();
    private final List<Integer> giftDraftSourceSlots = new ArrayList<>();
    private BufferedImage avatarCropSourceImage = null;
    private ResourceLocation avatarCropTexture = null;
    private int avatarCropBoxX = 0;
    private int avatarCropBoxY = 0;
    private int avatarCropBoxSize = 64;
    private int avatarCropDragOffsetX = 0;
    private int avatarCropDragOffsetY = 0;
    private UUID lastMarkedReadContactId;
    private int lastMarkedReadUnreadCount = -1;

    private boolean draggingMessageScrollbar = false;
    private int scrollbarGrabOffset = 0;

    public MaxMessengerScreen() {
        super(Component.literal("MAX"));
    }

    @Override
    protected void init() {
        super.init();
        
        if (messageInput == null) {
            messageInput = new MaxMessageInputBox(this.font, 0, 0, 10, 10);
        }

        Rect editor = getEditorRect();
        Rect input = getChatInputRect();
        Rect profile = getProfileViewRect();
        int fieldX = editor.x1 + 126;
        int fieldWidth = editor.width() - 140;

        this.displayNameBox = new EditBox(this.font, fieldX, editor.y1 + 82, fieldWidth, 16, Component.literal("Никнейм"));
        this.displayNameBox.setMaxLength(24);
        this.addRenderableWidget(this.displayNameBox);

        this.descriptionBox = new EditBox(this.font, fieldX, editor.y1 + 116, fieldWidth, 16, Component.literal("Описание"));
        this.descriptionBox.setMaxLength(120);
        this.addRenderableWidget(this.descriptionBox);

        this.searchBox = new EditBox(this.font, 0, 0, 100, 14, Component.literal("Поиск..."));
        this.searchBox.setMaxLength(100);
        this.addRenderableWidget(this.searchBox);

        this.saveProfileButton = this.addRenderableWidget(new ExtendedButton(
                editor.x2 - 72,
                editor.y2 - 22,
                64,
                16,
                Component.literal("Сохранить"),
                button -> saveProfile()
        ));

        this.cancelProfileButton = this.addRenderableWidget(new ExtendedButton(
                editor.x2 - 140,
                editor.y2 - 22,
                60,
                16,
                Component.literal("Назад"),
                button -> closeProfileOverlay()
        ));

        this.sendButton = this.addRenderableWidget(new ExtendedButton(
                input.x2 + 4,
                input.y1 + 4,
                18,
                18,
                Component.literal("➤"),
                button -> sendMessage()
        ));

        this.profileBackButton = this.addRenderableWidget(new ExtendedButton(
                profile.x1 + 8,
                profile.y2 - 22,
                58,
                16,
                Component.literal("Назад"),
                button -> closeProfileOverlay()
        ));

        this.profileEditButton = this.addRenderableWidget(new ExtendedButton(
                profile.x2 - 72,
                profile.y2 - 22,
                64,
                16,
                Component.literal("Изменить"),
                button -> openProfileEditor()
        ));

        MaxPackets.sendToServer(new RequestMaxSyncPacket());
        refreshModeFromState();
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation location : avatarTextureCache.values()) {
            mc.getTextureManager().release(location);
        }
        for (ResourceLocation location : attachmentPreviewTextureCache.values()) {
            mc.getTextureManager().release(location);
        }
        if (avatarCropTexture != null) {
            mc.getTextureManager().release(avatarCropTexture);
            avatarCropTexture = null;
        }
        avatarTextureCache.clear();
        attachmentPreviewTextureCache.clear();
        attachmentPreviewSizes.clear();
    }

    @Override
    public void tick() {
        super.tick();
        refreshModeFromState();

        if (displayNameBox != null) {
            displayNameBox.tick();
        }
        if (descriptionBox != null) {
            descriptionBox.tick();
        }
        if (searchBox != null) {
            searchBox.tick();
        }

        syncReadStateIfNeeded();
    }

    private void refreshModeFromState() {
        MaxUserProfile self = MaxClientState.getSelfProfile();
        boolean unregistered = self == null || !self.isRegistered();

        if (unregistered) {
            viewMode = ViewMode.REGISTER;
            if (!profileDraftLoaded && self != null) {
                displayNameBox.setValue(self.getDisplayName());
                descriptionBox.setValue(self.getDescription());
                selectedShowcaseItemId = self.getShowcaseItemId();
                loadAvatarSelection(self.getAvatarId());
                profileDraftLoaded = true;
            }
        } else {
            if (viewMode == ViewMode.REGISTER) {
                viewMode = ViewMode.CHATS;
            }
            profileDraftLoaded = false;
            if (selectedContactId == null) {
                selectedContactId = MaxClientState.getSelfId();
            }
        }

        if (selectedContactId != null && MaxClientState.getSelfId() != null) {
            boolean exists = selectedContactId.equals(MaxClientState.getSelfId()) || MaxClientState.getProfile(selectedContactId) != null;
            if (!exists) {
                selectedContactId = MaxClientState.getSelfId();
            }
        }

        if (viewedProfileId != null && MaxClientState.getProfile(viewedProfileId) == null && !viewedProfileId.equals(MaxClientState.getSelfId())) {
            viewedProfileId = null;
            if (viewMode == ViewMode.PROFILE_VIEW) {
                viewMode = ViewMode.CHATS;
            }
        }

        boolean editing = viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT;
        boolean viewingProfile = viewMode == ViewMode.PROFILE_VIEW;

        displayNameBox.setVisible(editing);
        descriptionBox.setVisible(editing);
        saveProfileButton.visible = editing;
        cancelProfileButton.visible = viewMode == ViewMode.PROFILE_EDIT;

        sendButton.visible = viewMode == ViewMode.CHATS && selectedContactId != null;
        sendButton.active = viewMode == ViewMode.CHATS && selectedContactId != null && (editingMessageId != 0L || pendingAttachment != null || !messageInput.isBlank());

        searchBox.setVisible(viewMode == ViewMode.CHATS && selectedContactId != null);
        if (searchBox.isVisible()) {
            Rect content = getContentRect();
            searchBox.setWidth(120);
            searchBox.x = content.x2 - 130;
            searchBox.y = content.y1 + 8;
            
            String q = searchBox.getValue().toLowerCase(Locale.ROOT);
            if (!q.equals(currentSearchQuery)) {
                currentSearchQuery = q;
                performSearch(q);
            }
        }

        profileBackButton.visible = viewingProfile;
        profileEditButton.visible = viewingProfile && viewedProfileId != null && viewedProfileId.equals(MaxClientState.getSelfId());
    }

    private void syncReadStateIfNeeded() {
        if (viewMode != ViewMode.CHATS || selectedContactId == null || selectedContactId.equals(MaxClientState.getSelfId())) {
            return;
        }

        int unread = MaxClientState.getUnreadCount(selectedContactId);
        if (unread <= 0) {
            lastMarkedReadContactId = selectedContactId;
            lastMarkedReadUnreadCount = 0;
            return;
        }

        boolean changedContact = !selectedContactId.equals(lastMarkedReadContactId);
        boolean changedUnread = unread != lastMarkedReadUnreadCount;
        if (changedContact || changedUnread) {
            MaxPackets.sendToServer(new MarkMaxConversationReadPacket(selectedContactId));
            lastMarkedReadContactId = selectedContactId;
            lastMarkedReadUnreadCount = unread;
        }
    }

    private void saveProfile() {
        String name = displayNameBox.getValue().trim();
        if (name.isBlank()) {
            statusLine = "Впиши ник";
            return;
        }

        String showcaseItemId = selectedShowcaseItemId == null ? "" : selectedShowcaseItemId;
        MaxPackets.sendToServer(new RegisterMaxProfilePacket(name, descriptionBox.getValue(), selectedAvatarId, showcaseItemId));
        MaxPackets.sendToServer(new RequestMaxSyncPacket());
        showcasePickerOpen = false;
        statusLine = "Профиль сохранён";

        if (viewMode == ViewMode.PROFILE_EDIT) {
            viewMode = ViewMode.PROFILE_VIEW;
            viewedProfileId = MaxClientState.getSelfId();
        }
    }

    private void sendMessage() {
        if (selectedContactId == null) {
            return;
        }

        String draft = messageInput.getValue();
        String normalized = draft.replace("\r", "").trim();

        if (editingMessageId != 0L) {
            if (normalized.isBlank()) {
                return;
            }
            MaxPackets.sendToServer(new EditMaxMessagePacket(editingMessageId, draft));
            messageInput.clear();
            editingMessageId = 0L;
            forceScrollToBottom = true;
            statusLine = "Сообщение изменено";
            MaxPackets.sendToServer(new RequestMaxSyncPacket());
            return;
        }

        if (pendingAttachment != null) {
            startChunkedAttachmentUpload(selectedContactId, normalized, replyToMessageId, pendingAttachment);
            pendingAttachment = null;
            messageInput.clear();
            replyToMessageId = 0L;
            forceScrollToBottom = true;
            statusLine = "Файл отправляется...";
            return;
        }

        if (normalized.isBlank()) {
            return;
        }

        MaxPackets.sendToServer(new SendMaxMessagePacket(selectedContactId, draft, replyToMessageId));
        messageInput.clear();
        replyToMessageId = 0L;
        forceScrollToBottom = true;
        statusLine = "";
        MaxPackets.sendToServer(new RequestMaxSyncPacket());
    }

    private void openProfileView(UUID userId) {
        if (userId == null) {
            return;
        }
        viewedProfileId = userId;
        showcasePickerOpen = false;
        awaitingAvatarDrop = false;
        profileDraftLoaded = false;
        viewMode = ViewMode.PROFILE_VIEW;
        chatInputFocused = false;
        refreshModeFromState();
    }

    private void openProfileEditor() {
        MaxUserProfile self = MaxClientState.getSelfProfile();
        if (self == null) {
            return;
        }

        displayNameBox.setValue(self.getDisplayName());
        descriptionBox.setValue(self.getDescription());
        selectedShowcaseItemId = self.getShowcaseItemId();
        showcasePickerOpen = false;
        profileDraftLoaded = true;
        loadAvatarSelection(self.getAvatarId());
        viewedProfileId = self.getUserId();
        viewMode = ViewMode.PROFILE_EDIT;
        statusLine = "";

        this.setFocused(displayNameBox);
        displayNameBox.setFocus(true);
        descriptionBox.setFocus(false);

        refreshModeFromState();
    }

    private void closeProfileOverlay() {
        MaxUserProfile self = MaxClientState.getSelfProfile();
        if (self == null || !self.isRegistered()) {
            return;
        }
        viewedProfileId = null;
        showcasePickerOpen = false;
        awaitingAvatarDrop = false;
        profileDraftLoaded = false;
        viewMode = ViewMode.CHATS;
        refreshModeFromState();
    }

    private void loadAvatarSelection(String avatarId) {
        if (avatarId == null || avatarId.isBlank()) {
            selectedAvatarId = "stock_steve";
            return;
        }
        if (avatarId.startsWith("custom;")) {
            selectedAvatarId = "stock_steve";
            return;
        }
        selectedAvatarId = avatarId;
    }

    private ItemStack getSelectedShowcaseStack() {
        return getShowcaseStack(selectedShowcaseItemId);
    }

    private ItemStack getShowcaseStack(String showcaseItemId) {
        if (showcaseItemId == null || showcaseItemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation id = ResourceLocation.tryParse(showcaseItemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    private String getShowcaseName(String showcaseItemId) {
        ItemStack stack = getShowcaseStack(showcaseItemId);
        return stack.isEmpty() ? "Пусто" : stack.getHoverName().getString();
    }

    private void pickCustomAvatarFromFile() {
        showcasePickerOpen = false;
        awaitingAvatarDrop = true;
        statusLine = "Перетащи PNG/JPG в окно игры";
    }

    private String buildImageAvatarId(File file) {
        try {
            BufferedImage source = ImageIO.read(file);
            if (source == null) {
                return null;
            }

            int[] sizes = new int[] {192, 160, 128, 96, 80, 64};
            for (int targetSize : sizes) {
                BufferedImage cropped = cropCenterSquare(source);
                BufferedImage scaled = scaleImage(cropped, targetSize, targetSize);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(scaled, "png", output);
                String encoded = Base64.getEncoder().encodeToString(output.toByteArray());
                if (encoded.length() < 30000) {
                    return "image:" + encoded;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private BufferedImage cropCenterSquare(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        return source.getSubimage(x, y, side, side);
    }

    private BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private void beginAvatarCrop(BufferedImage source) {
        avatarCropSourceImage = source;
        avatarCropOpen = true;
        profileDraftLoaded = true;
        if (avatarCropTexture != null) {
            Minecraft.getInstance().getTextureManager().release(avatarCropTexture);
            avatarCropTexture = null;
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(source, "png", output);
            NativeImage image = NativeImage.read(new ByteArrayInputStream(output.toByteArray()));
            avatarCropTexture = Minecraft.getInstance().getTextureManager().register("max_avatar_crop_" + System.nanoTime(), new DynamicTexture(image));
        } catch (Exception ignored) {
            avatarCropTexture = null;
        }
        Rect imageRect = getAvatarCropImageRect();
        avatarCropBoxSize = Math.min(imageRect.width(), imageRect.height());
        avatarCropBoxX = imageRect.x1 + (imageRect.width() - avatarCropBoxSize) / 2;
        avatarCropBoxY = imageRect.y1 + (imageRect.height() - avatarCropBoxSize) / 2;
        statusLine = "Подвинь квадрат и нажми Обрезать";
    }

    private void cancelAvatarCrop() {
        avatarCropOpen = false;
        avatarCropDragging = false;
        avatarCropSourceImage = null;
        if (avatarCropTexture != null) {
            Minecraft.getInstance().getTextureManager().release(avatarCropTexture);
            avatarCropTexture = null;
        }
        statusLine = "Обрезка отменена";
    }

    private void applyAvatarCrop() {
        if (avatarCropSourceImage == null) {
            cancelAvatarCrop();
            return;
        }
        Rect imageRect = getAvatarCropImageRect();
        double scale = avatarCropSourceImage.getWidth() / (double) imageRect.width();
        int cropX = (int) Math.round((avatarCropBoxX - imageRect.x1) * scale);
        int cropY = (int) Math.round((avatarCropBoxY - imageRect.y1) * scale);
        int cropSize = (int) Math.round(avatarCropBoxSize * scale);
        cropX = clamp(cropX, 0, Math.max(0, avatarCropSourceImage.getWidth() - 1));
        cropY = clamp(cropY, 0, Math.max(0, avatarCropSourceImage.getHeight() - 1));
        cropSize = clamp(cropSize, 8, Math.min(avatarCropSourceImage.getWidth() - cropX, avatarCropSourceImage.getHeight() - cropY));

        BufferedImage cropped = avatarCropSourceImage.getSubimage(cropX, cropY, cropSize, cropSize);
        try {
            String avatarData = buildImageAvatarId(writeTempAvatar(cropped));
            if (avatarData != null && !avatarData.isBlank()) {
                selectedAvatarId = avatarData;
                statusLine = "Аватарка обрезана, теперь сохрани профиль";
            }
        } catch (Exception exception) {
            statusLine = "Ошибка обрезки: " + exception.getClass().getSimpleName();
        }
        avatarCropOpen = false;
        avatarCropDragging = false;
        avatarCropSourceImage = null;
        if (avatarCropTexture != null) {
            Minecraft.getInstance().getTextureManager().release(avatarCropTexture);
            avatarCropTexture = null;
        }
    }

    private File writeTempAvatar(BufferedImage image) throws Exception {
        File temp = File.createTempFile("max_avatar_crop", ".png");
        temp.deleteOnExit();
        ImageIO.write(image, "png", temp);
        return temp;
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }

        Path path = paths.get(0);

        if (awaitingAvatarDrop && (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT)) {
            awaitingAvatarDrop = false;
            statusLine = "Обрабатываю аватарку...";

            new Thread(() -> {
                try {
                    BufferedImage source = ImageIO.read(path.toFile());
                    if (source == null) {
                        Minecraft.getInstance().execute(() -> statusLine = "Не удалось прочитать картинку");
                        return;
                    }
                    Minecraft.getInstance().execute(() -> beginAvatarCrop(source));
                } catch (Exception exception) {
                    Minecraft.getInstance().execute(() -> statusLine = "Ошибка аватарки: " + exception.getClass().getSimpleName());
                }
            }, "MAX-Avatar-Drop").start();
            return;
        }

        if (awaitingChatFileDrop && viewMode == ViewMode.CHATS && selectedContactId != null) {
            awaitingChatFileDrop = false;
            statusLine = "Обрабатываю файл...";
            new Thread(() -> {
                try {
                    long size = Files.size(path);
                    if (size <= 0L) {
                        Minecraft.getInstance().execute(() -> statusLine = "Пустой файл");
                        return;
                    }
                    String mime = Files.probeContentType(path);
                    if (mime == null || mime.isBlank()) {
                        mime = "application/octet-stream";
                    }
                    byte[] previewBytes = buildAttachmentPreviewBytes(path, mime);
                    PendingAttachment attachment = new PendingAttachment(path.getFileName().toString(), mime, path, size, previewBytes);
                    Minecraft.getInstance().execute(() -> {
                        pendingAttachment = attachment;
                        statusLine = "Файл прикреплён";
                    });
                } catch (Exception exception) {
                    Minecraft.getInstance().execute(() -> statusLine = "Ошибка файла: " + exception.getClass().getSimpleName());
                }
            }, "MAX-File-Drop").start();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openedImageAttachmentId != null) {
            openedImageAttachmentId = null;
            return true;
        }

        if (avatarCropOpen) {
            if (getAvatarCropApplyRect().contains(mouseX, mouseY)) {
                applyAvatarCrop();
                return true;
            }
            if (getAvatarCropCancelRect().contains(mouseX, mouseY)) {
                cancelAvatarCrop();
                return true;
            }
            if (getAvatarCropSelectionRect().contains(mouseX, mouseY)) {
                avatarCropDragging = true;
                avatarCropDragOffsetX = (int) mouseX - avatarCropBoxX;
                avatarCropDragOffsetY = (int) mouseY - avatarCropBoxY;
                return true;
            }
            return true;
        }

        if (giftComposerOpen && viewMode == ViewMode.CHATS) {
            for (GiftDraftSlotBox box : giftDraftSlotBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    if (box.slotIndex() >= 0 && box.slotIndex() < giftDraftStacks.size()) {
                        giftDraftStacks.set(box.slotIndex(), ItemStack.EMPTY);
                        giftDraftSourceSlots.set(box.slotIndex(), -1);
                    }
                    return true;
                }
            }
            for (GiftInventoryBox box : giftInventoryBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    addGiftDraftStack(box.stack(), box.inventoryIndex());
                    return true;
                }
            }
            if (getGiftSendButtonRect().contains(mouseX, mouseY)) {
                sendGiftDraft();
                return true;
            }
            if (getGiftCancelButtonRect().contains(mouseX, mouseY)) {
                giftComposerOpen = false;
                return true;
            }
            return true;
        }

        if (showcasePickerOpen && (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT)) {
            Rect editor = getEditorRect();
            for (ShowcaseClickBox box : showcaseClickBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    selectedShowcaseItemId = box.itemId();
                    showcasePickerOpen = false;
                    statusLine = selectedShowcaseItemId.isBlank() ? "Трофей очищен" : "Трофей выбран";
                    return true;
                }
            }
            if (!getShowcasePickerRect(editor).contains(mouseX, mouseY)) {
                showcasePickerOpen = false;
                return true;
            }
            return true;
        }

        if (button == 0 && contextMenuMessageId != 0L) {
            for (MessageActionBox box : messageActionBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    handleMessageAction(box.action(), box.messageId());
                    return true;
                }
            }
            contextMenuMessageId = 0L;
            messageActionBoxes.clear();
        }

        if (awaitingAvatarDrop && (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT)) {
            awaitingAvatarDrop = false;
            statusLine = "Загрузка аватарки отменена";
            return true;
        }

        if (awaitingChatFileDrop && viewMode == ViewMode.CHATS) {
            awaitingChatFileDrop = false;
            statusLine = "Отправка файла отменена";
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT) {
            Rect editor = getEditorRect();

            if (getEditorShowcaseSlotRect(editor).contains(mouseX, mouseY)) {
                awaitingAvatarDrop = false;
                showcasePickerOpen = true;
                return true;
            }

            for (AvatarClickBox box : avatarBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    if (UPLOAD_AVATAR_ID.equals(box.avatarId)) {
                        pickCustomAvatarFromFile();
                    } else {
                        awaitingAvatarDrop = false;
                        selectedAvatarId = box.avatarId;
                    }
                    return true;
                }
            }
            return false;
        }

        if (viewMode == ViewMode.PROFILE_VIEW) {
            return true;
        }

        Rect messagesRect = getMessagesRect();
        Rect thumb = getMessageScrollbarThumbRect(messagesRect);
        if (thumb != null && thumb.contains(mouseX, mouseY)) {
            draggingMessageScrollbar = true;
            scrollbarGrabOffset = (int) mouseY - thumb.y1;
            return true;
        }

        for (GiftButtonBox box : giftButtonBoxes) {
            if (box.contains(mouseX, mouseY)) {
                MaxPackets.sendToServer(new RespondMaxGiftPacket(box.giftId(), box.accept()));
                return true;
            }
        }

        for (FileButtonBox box : fileButtonBoxes) {
            if (box.contains(mouseX, mouseY)) {
                try {
                    MaxPackets.sendToServer(new RequestMaxAttachmentDownloadPacket(UUID.fromString(box.attachmentId())));
                    statusLine = "Скачиваю файл...";
                } catch (Exception ignored) {
                }
                return true;
            }
        }

        Rect clipRect = getAttachButtonRect();
        if (clipRect.contains(mouseX, mouseY)) {
            attachMenuOpen = !attachMenuOpen;
            return true;
        }

        if (attachMenuOpen) {
            for (AttachActionBox box : attachActionBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    if (box.action() == AttachAction.FILE) {
                        attachMenuOpen = false;
                        awaitingChatFileDrop = true;
                        statusLine = "Перетащи файл в окно игры";
                    } else if (box.action() == AttachAction.GIFT) {
                        attachMenuOpen = false;
                        giftComposerOpen = true;
                        ensureGiftDraftSize();
                    }
                    return true;
                }
            }
            attachMenuOpen = false;
        }

        if (button == 1) {
            for (MessageHitBox box : messageHitBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    openMessageContextMenu(box.messageId(), (int) mouseX, (int) mouseY);
                    return true;
                }
            }
        }

        if (button == 0) {
            for (MessageHitBox box : messageHitBoxes) {
                if (box.contains(mouseX, mouseY)) {
                    MaxChatMessage hit = MaxClientState.getMessage(box.messageId());
                    if (hit != null && hit.getType() == MaxChatMessage.MessageType.FILE) {
                        MaxFileAttachment attachment = MaxClientState.getAttachment(hit.getAttachmentId());
                        if (attachment != null && attachment.isImage() && attachment.getPreviewImageBytes().length > 0) {
                            openedImageAttachmentId = hit.getAttachmentId();
                            return true;
                        }
                    }
                    if (box.messageId() == replyToMessageId) {
                        return true;
                    }
                }
            }
            if (getComposerClearRect().contains(mouseX, mouseY)) {
                clearComposerModes();
                return true;
            }
            if (pendingAttachment != null && getPendingAttachmentRemoveRect().contains(mouseX, mouseY)) {
                pendingAttachment = null;
                statusLine = "Файл убран";
                return true;
            }
        }

        rebuildContactRows();
        for (ContactRow row : contactRows) {
            if (row.contains(mouseX, mouseY)) {
                if (forwardingMessageId != 0L) {
                    MaxPackets.sendToServer(new ForwardMaxMessagePacket(forwardingMessageId, row.userId));
                    statusLine = "Сообщение переслано";
                    forwardingMessageId = 0L;
                }
                selectedContactId = row.userId;
                forceScrollToBottom = true;
                chatInputFocused = false;
                lastMarkedReadContactId = null;
                lastMarkedReadUnreadCount = -1;
                return true;
            }
        }

        if (getSelfProfileRect().contains(mouseX, mouseY)) {
            openProfileView(MaxClientState.getSelfId());
            return true;
        }

        if (getChatHeaderRect().contains(mouseX, mouseY)) {
            UUID profileId = selectedContactId != null ? selectedContactId : MaxClientState.getSelfId();
            openProfileView(profileId);
            return true;
        }

        if (viewMode == ViewMode.CHATS && selectedContactId != null && scrollToBottomBox != null && scrollToBottomBox.contains(mouseX, mouseY)) {
            forceScrollToBottom = true;
            return true;
        }

        if (viewMode == ViewMode.CHATS && selectedContactId != null && searchBox != null) {
            if (searchBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (viewMode == ViewMode.CHATS && selectedContactId != null && messageInput != null) {
            if (messageInput.mouseClicked(mouseX, mouseY, button)) {
                chatInputFocused = messageInput.isFocused();
                return true;
            } else {
                chatInputFocused = false;
                messageInput.setFocused(false);
            }
        }

        chatInputFocused = selectedContactId != null && getChatInputRect().contains(mouseX, mouseY);
        if (chatInputFocused && messageInput != null) {
            messageInput.setFocused(true);
        }
        return chatInputFocused;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (messageInput != null) {
            messageInput.mouseReleased();
        }
        draggingMessageScrollbar = false;
        avatarCropDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (messageInput != null && messageInput.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }

        if (avatarCropOpen && avatarCropDragging) {
            Rect imageRect = getAvatarCropImageRect();
            avatarCropBoxX = clamp((int) mouseX - avatarCropDragOffsetX, imageRect.x1, imageRect.x2 - avatarCropBoxSize);
            avatarCropBoxY = clamp((int) mouseY - avatarCropDragOffsetY, imageRect.y1, imageRect.y2 - avatarCropBoxSize);
            return true;
        }

        if (draggingMessageScrollbar && viewMode == ViewMode.CHATS && selectedContactId != null) {
            Rect messagesRect = getMessagesRect();
            ChatMetrics metrics = buildChatMetrics(messagesRect);
            forceScrollToBottom = false;
            Rect track = getMessageScrollbarTrackRect(messagesRect);
            int thumbHeight = getThumbHeight(metrics, track.height());
            int movable = Math.max(1, track.height() - thumbHeight);
            int thumbTop = (int) mouseY - scrollbarGrabOffset;
            thumbTop = clamp(thumbTop, track.y1, track.y2 - thumbHeight);
            int maxScroll = Math.max(1, metrics.maxScroll());
            messageScroll = (thumbTop - track.y1) * maxScroll / movable;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (messageInput != null && getChatInputRect().contains(mouseX, mouseY)) {
            if (messageInput.mouseScrolled(delta)) {
                return true;
            }
        }

        if (avatarCropOpen) {
            Rect imageRect = getAvatarCropImageRect();
            int oldSize = avatarCropBoxSize;
            avatarCropBoxSize = clamp(avatarCropBoxSize + (delta > 0 ? -8 : 8), 24, Math.min(imageRect.width(), imageRect.height()));
            int diff = avatarCropBoxSize - oldSize;
            avatarCropBoxX = clamp(avatarCropBoxX - diff / 2, imageRect.x1, imageRect.x2 - avatarCropBoxSize);
            avatarCropBoxY = clamp(avatarCropBoxY - diff / 2, imageRect.y1, imageRect.y2 - avatarCropBoxSize);
            return true;
        }

        if (viewMode != ViewMode.CHATS) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        Rect contactsRect = getContactsRect();
        if (contactsRect.contains(mouseX, mouseY)) {
            int totalRows = getSidebarContacts().size();
            int visibleRows = Math.max(1, contactsRect.height() / 36);
            int maxScroll = Math.max(0, totalRows - visibleRows);
            if (delta < 0 && contactScroll < maxScroll) {
                contactScroll++;
            } else if (delta > 0 && contactScroll > 0) {
                contactScroll--;
            }
            return true;
        }

        if (getMessagesRect().contains(mouseX, mouseY) && selectedContactId != null) {
            ChatMetrics metrics = buildChatMetrics(getMessagesRect());
            forceScrollToBottom = false;
            if (delta < 0) {
                messageScroll = Math.min(metrics.maxScroll(), messageScroll + 20);
            } else if (delta > 0) {
                messageScroll = Math.max(0, messageScroll - 20);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT) {
            if (displayNameBox.isFocused() && displayNameBox.charTyped(codePoint, modifiers)) {
                return true;
            }
            if (descriptionBox.isFocused() && descriptionBox.charTyped(codePoint, modifiers)) {
                return true;
            }
            return super.charTyped(codePoint, modifiers);
        }

        if (viewMode == ViewMode.CHATS && selectedContactId != null) {
            if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
                return true;
            }
            if (messageInput.isFocused() && messageInput.charTyped(codePoint, modifiers)) {
                return true;
            }
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (viewMode == ViewMode.PROFILE_VIEW || viewMode == ViewMode.PROFILE_EDIT) {
                closeProfileOverlay();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT) {
            if (displayNameBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (descriptionBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == 258) {
                if (displayNameBox.isFocused()) {
                    displayNameBox.setFocus(false);
                    descriptionBox.setFocus(true);
                    this.setFocused(descriptionBox);
                } else {
                    descriptionBox.setFocus(false);
                    displayNameBox.setFocus(true);
                    this.setFocused(displayNameBox);
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                saveProfile();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (viewMode == ViewMode.CHATS && selectedContactId != null) {
            if (searchBox != null && searchBox.isFocused()) {
                if (keyCode == 265) { // UP
                    cycleSearch(-1);
                    return true;
                } else if (keyCode == 264) { // DOWN
                    cycleSearch(1);
                    return true;
                } else if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
            if (messageInput.isFocused()) {
                if (keyCode == 257 || keyCode == 335) {
                    if (hasShiftDown()) {
                        messageInput.insertText("\n");
                    } else {
                        sendMessage();
                    }
                    return true;
                }
                if (messageInput.keyPressed(keyCode, modifiers)) {
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        fill(poseStack, 0, 0, this.width, this.height, 0xFF0C1015);

        Rect editorRect = null;
        if (viewMode == ViewMode.CHATS) {
            renderShell(poseStack);
            renderContacts(poseStack, mouseX, mouseY);
            renderChatArea(poseStack, mouseX, mouseY);
            renderSelfProfileCard(poseStack, mouseX, mouseY);
        } else if (viewMode == ViewMode.REGISTER || viewMode == ViewMode.PROFILE_EDIT) {
            editorRect = getEditorRect();
            renderProfileEditor(poseStack, mouseX, mouseY);
        } else if (viewMode == ViewMode.PROFILE_VIEW) {
            renderProfileView(poseStack, mouseX, mouseY);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);

        if (showcasePickerOpen && editorRect != null) {
            fill(poseStack, 0, 0, this.width, this.height, 0xA0000000);
            renderShowcasePicker(poseStack, mouseX, mouseY, editorRect);
        }

        if (awaitingAvatarDrop && editorRect != null) {
            renderAvatarDropOverlay(poseStack, editorRect);
        }

        if (avatarCropOpen) {
            renderAvatarCropOverlay(poseStack);
        }

        if (awaitingChatFileDrop && viewMode == ViewMode.CHATS) {
            renderChatFileDropOverlay(poseStack);
        }

        if (giftComposerOpen && viewMode == ViewMode.CHATS) {
            renderGiftComposerOverlay(poseStack, mouseX, mouseY);
        }

        if (contextMenuMessageId != 0L && viewMode == ViewMode.CHATS && !giftComposerOpen) {
            renderMessageContextMenu(poseStack, mouseX, mouseY);
        }

        if (openedImageAttachmentId != null) {
            renderImagePreviewOverlay(poseStack);
        }

        renderTransfersOverlay(poseStack);

        if (!statusLine.isEmpty()) {
            drawCenteredString(poseStack, this.font, Component.literal(statusLine), this.width / 2, this.height - 8, 0xFF98A8BA);
        }
    }

    private void renderShell(PoseStack poseStack) {
        Rect sidebar = getSidebarRect();
        Rect content = getContentRect();

        fill(poseStack, sidebar.x1, sidebar.y1, sidebar.x2, sidebar.y2, 0xFF151C24);
        fill(poseStack, content.x1, content.y1, content.x2, content.y2, 0xFF111821);
        fill(poseStack, sidebar.x2, sidebar.y1, sidebar.x2 + 1, sidebar.y2, 0xFF273140);

        drawString(poseStack, this.font, "MAX", sidebar.x1 + 7, sidebar.y1 + 6, 0xFFFFFFFF);
        drawString(poseStack, this.font, "Чаты", sidebar.x1 + 7, sidebar.y1 + 16, 0xFF8394A7);
    }

    private void renderContacts(PoseStack poseStack, int mouseX, int mouseY) {
        rebuildContactRows();
        for (ContactRow row : contactRows) {
            int bg = row.userId.equals(selectedContactId) ? 0xFF223349 : (row.contains(mouseX, mouseY) ? 0xFF1A2736 : 0x00000000);
            fill(poseStack, row.x1, row.y1, row.x2, row.y2, bg);

            drawAvatarSquare(poseStack, row.avatarId, row.x1 + 6, row.y1 + 6, 20, row.online, true);
            drawString(poseStack, this.font, trim(row.displayName, 12), row.x1 + 30, row.y1 + 6, 0xFFFFFFFF);
            drawString(poseStack, this.font, trim(row.preview, 14), row.x1 + 30, row.y1 + 16, row.favorite ? 0xFFD7E8FF : 0xFF8FA0B2);

            if (row.unreadCount > 0) {
                String badge = row.unreadCount > 9 ? "9+" : String.valueOf(row.unreadCount);
                fill(poseStack, row.x2 - 18, row.y1 + 10, row.x2 - 6, row.y1 + 22, 0xFFD63B3B);
                drawCenteredString(poseStack, this.font, Component.literal(badge), row.x2 - 12, row.y1 + 13, 0xFFFFFFFF);
            }
        }
    }

    private void renderChatArea(PoseStack poseStack, int mouseX, int mouseY) {
        Rect header = getChatHeaderRect();
        Rect messages = getMessagesRect();
        Rect input = getChatInputRect();

        messageHitBoxes.clear();
        fileButtonBoxes.clear();
        giftButtonBoxes.clear();
        attachActionBoxes.clear();
        messageActionBoxes.clear();

        fill(poseStack, header.x1, header.y1, header.x2, header.y2, 0xFF151D27);
        fill(poseStack, messages.x1, messages.y1, messages.x2, messages.y2, 0x331B2430);
        fill(poseStack, input.x1, input.y1, input.x2, input.y2, chatInputFocused ? 0xFF1A2E43 : 0xFF172330);
        fill(poseStack, input.x1, input.y1, input.x2, input.y1 + 1, 0xFF2E557D);

        if (selectedContactId == null) {
            drawCenteredString(poseStack, this.font, Component.literal("Выбери чат"), messages.centerX(), messages.centerY(), 0xFF90A0B2);
            return;
        }

        boolean favorites = selectedContactId.equals(MaxClientState.getSelfId());
        MaxUserProfile selectedProfile = favorites ? MaxClientState.getSelfProfile() : MaxClientState.getProfile(selectedContactId);
        String title = favorites ? "Избранное" : (selectedProfile == null ? "Чат" : selectedProfile.getDisplayName());
        String subtitle;
        String avatarId;
        boolean online;

        if (favorites) {
            subtitle = "самому себе";
            avatarId = "favorite_flag";
            online = true;
        } else if (selectedProfile != null) {
            subtitle = selectedProfile.getDescription().isBlank() ? (isOnline(selectedProfile.getUserId()) ? "в сети" : "не в сети") : trim(selectedProfile.getDescription(), 28);
            avatarId = selectedProfile.getAvatarId();
            online = isOnline(selectedProfile.getUserId());
        } else {
            subtitle = "профиль недоступен";
            avatarId = "stock_steve";
            online = false;
        }

        drawAvatarSquare(poseStack, avatarId, header.x1 + 6, header.y1 + 6, 18, online, true);
        drawString(poseStack, this.font, title, header.x1 + 30, header.y1 + 6, 0xFFFFFFFF);
        drawString(poseStack, this.font, subtitle, header.x1 + 30, header.y1 + 16, favorites ? 0xFFD3E4FF : 0xFF8394A7);

        List<MaxChatMessage> conversation = MaxClientState.getConversation(selectedContactId);
        ChatMetrics metrics = buildChatMetrics(messages);
        long tailMessageId = conversation.isEmpty() ? 0L : conversation.get(conversation.size() - 1).getMessageId();
        boolean sameConversation = selectedContactId.equals(lastKnownConversationId);
        boolean wasAtBottom = messageScroll >= Math.max(0, lastKnownConversationMaxScroll - 4);

        if (!sameConversation) {
            forceScrollToBottom = true;
        }

        if (smoothMessageScroll >= 0 && metrics != null) {
            long now = System.currentTimeMillis();
            long elapsed = now - smoothScrollStartTime;
            if (elapsed >= 1000L) {
                messageScroll = (int) smoothMessageScroll;
                smoothMessageScroll = -1;
            } else {
                float progress = elapsed / 1000.0f;
                // ease out cubic
                progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
                messageScroll = (int) (smoothScrollStartPos + (smoothMessageScroll - smoothScrollStartPos) * progress);
            }
        }

        if (jumpToMessageId != 0L && metrics != null) {
            int entryY = 0;
            for (ChatEntry entry : metrics.entries()) {
                if (entry.message() != null && entry.message().getMessageId() == jumpToMessageId) {
                    messageScroll = clamp(entryY - metrics.viewportHeight() / 2, 0, metrics.maxScroll());
                    smoothMessageScroll = -1;
                    break;
                }
                entryY += entry.height();
            }
            jumpToMessageId = 0L;
        }

        if (forceScrollToBottom && metrics != null) {
            messageScroll = metrics.maxScroll();
            forceScrollToBottom = false;
            smoothMessageScroll = -1;
        } else if (tailMessageId != lastKnownConversationTailMessageId && sameConversation && wasAtBottom && metrics != null) {
            smoothMessageScroll = metrics.maxScroll();
            smoothScrollStartPos = messageScroll;
            smoothScrollStartTime = System.currentTimeMillis();
        } else if (metrics != null && messageScroll > metrics.maxScroll() && smoothMessageScroll < 0) {
            messageScroll = metrics.maxScroll();
        }

        GuiComponent.enableScissor(messages.x1, messages.y1, messages.x2, messages.y2);
        int currentY = messages.y1 + 4 - messageScroll;
        for (ChatEntry entry : metrics.entries()) {
            if (entry.dayLabel() != null) {
                if (currentY + entry.height() >= messages.y1 && currentY <= messages.y2) {
                    drawCenteredString(poseStack, this.font, Component.literal(entry.dayLabel()), messages.centerX() - 2, currentY + 2, 0xFF7E8792);
                }
            } else if (entry.message() != null) {
                renderChatEntry(poseStack, mouseX, mouseY, messages, entry, currentY);
            }
            currentY += entry.height();
        }
        GuiComponent.disableScissor();

        lastKnownConversationId = selectedContactId;
        lastKnownConversationTailMessageId = tailMessageId;
        lastKnownConversationMaxScroll = metrics.maxScroll();

        renderMessageScrollbar(poseStack, messages, metrics);

        if (metrics.maxScroll() > 0 && messageScroll < metrics.maxScroll() - 100) {
            int arrowSize = 32;
            int arrowX = messages.x2 - 16 - arrowSize;
            int arrowY = messages.y2 - 16 - arrowSize;
            scrollToBottomBox = new Rect(arrowX, arrowY, arrowX + arrowSize, arrowY + arrowSize);
            fill(poseStack, arrowX, arrowY, arrowX + arrowSize, arrowY + arrowSize, 0xAA2B6EE6);
            drawCenteredString(poseStack, this.font, Component.literal("↓"), arrowX + arrowSize / 2, arrowY + arrowSize / 2 - 4, 0xFFFFFFFF);
            
            int unread = MaxClientState.getUnreadCount(selectedContactId);
            if (unread > 0) {
                fill(poseStack, arrowX + arrowSize - 12, arrowY - 4, arrowX + arrowSize + 4, arrowY + 12, 0xFFE53935);
                drawCenteredString(poseStack, this.font, Component.literal(String.valueOf(unread)), arrowX + arrowSize - 4, arrowY, 0xFFFFFFFF);
            }
        } else {
            scrollToBottomBox = null;
        }

        renderComposer(poseStack, mouseX, mouseY, input);
    }

    private void renderChatEntry(PoseStack poseStack, int mouseX, int mouseY, Rect messages, ChatEntry entry, int blockTop) {
        MaxChatMessage message = entry.message();
        if (message == null) {
            return;
        }

        boolean own = entry.own();
        int bubbleX = own ? messages.x2 - entry.bubbleWidth() - 14 : messages.x1 + 6;
        int bubbleY = blockTop;
        int bubbleColor = own ? 0xFF2B6EE6 : 0xFF283342;
        int metaY = bubbleY + entry.bubbleHeight() + 1;
        String metaText = entry.metaText();

        int actionHeight = entry.bubbleHeight() + 10;
        messageHitBoxes.add(new MessageHitBox(message.getMessageId(), bubbleX, bubbleY, bubbleX + entry.bubbleWidth(), bubbleY + actionHeight));

        if (message.isDeleted()) {
            fill(poseStack, bubbleX, bubbleY, bubbleX + entry.bubbleWidth(), bubbleY + entry.bubbleHeight(), bubbleColor);
            drawString(poseStack, this.font, "Сообщение удалено", bubbleX + 6, bubbleY + 4, 0xFF8394A7);
            drawString(poseStack, this.font, metaText, own ? bubbleX + entry.bubbleWidth() - this.font.width(metaText) : bubbleX, metaY, 0xFF94A3B5);
            return;
        }

        if (message.getType() == MaxChatMessage.MessageType.SYSTEM) {
            drawCenteredString(poseStack, this.font, Component.literal(message.getText()), messages.centerX(), bubbleY + 2, 0xFF8795A6);
            return;
        }

        fill(poseStack, bubbleX, bubbleY, bubbleX + entry.bubbleWidth(), bubbleY + entry.bubbleHeight(), bubbleColor);
        if (currentSearchResultIndex >= 0 && currentSearchResultIndex < searchResults.size()) {
            if (searchResults.get(currentSearchResultIndex) == message.getMessageId()) {
                fill(poseStack, bubbleX, bubbleY, bubbleX + entry.bubbleWidth(), bubbleY + entry.bubbleHeight(), 0x44FFFF00);
            }
        }
        int lineY = bubbleY + 4;

        if (message.isForwarded()) {
            String forward = "Переслано" + (message.getForwardSourceName().isBlank() ? "" : " от " + trim(message.getForwardSourceName(), 14));
            drawString(poseStack, this.font, forward, bubbleX + 6, lineY, 0xFFD7E8FF);
            lineY += this.font.lineHeight;
        }

        if (message.getReplyToMessageId() > 0L) {
            String reply = buildReplyPreview(message.getReplyToMessageId());
            fill(poseStack, bubbleX + 4, lineY, bubbleX + entry.bubbleWidth() - 4, lineY + this.font.lineHeight + 2, own ? 0x55213570 : 0x55333C48);
            drawString(poseStack, this.font, trim(reply, 24), bubbleX + 6, lineY + 1, 0xFFE3EDFF);
            lineY += this.font.lineHeight + 4;
        }

        if (message.getType() == MaxChatMessage.MessageType.FILE) {
            MaxFileAttachment attachment = MaxClientState.getAttachment(message.getAttachmentId());
            String name = attachment == null ? "file.bin" : trim(attachment.getFileName(), 20);
            String sizeText = attachment == null ? "файл" : formatBytes(attachment.getSizeBytes());
            String typeText = attachment == null ? "Файл" : attachment.isImage() ? "Фото" : attachment.isVideo() ? "Видео" : attachment.isAudio() ? "Аудио" : "Файл";
            drawString(poseStack, this.font, typeText, bubbleX + 6, lineY, 0xFFFFFFFF);
            if (attachment != null && attachment.isImage() && attachment.getPreviewImageBytes().length > 0) {
                ResourceLocation previewTexture = getOrCreateAttachmentPreviewTexture(message.getAttachmentId(), attachment.getPreviewImageBytes());
                int previewX = bubbleX + 6;
                int previewY = lineY + this.font.lineHeight + 2;
                int previewW = Math.min(entry.bubbleWidth() - 12, 160);
                int previewH = Math.max(48, previewW * 192 / 256);
                if (previewTexture != null) {
                    int[] size = attachmentPreviewSizes.get(message.getAttachmentId());
                    if (size != null && size[0] > 0) {
                        previewH = previewW * size[1] / size[0];
                    }
                }
                fill(poseStack, previewX, previewY, previewX + previewW, previewY + previewH, 0x22111111);
                if (previewTexture != null) {
                    RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, previewTexture);
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                    GuiComponent.blit(poseStack, previewX, previewY, 0, 0, previewW, previewH, previewW, previewH);
                }
                drawString(poseStack, this.font, name, previewX, previewY + previewH + 2, 0xFFFFFFFF);
                drawString(poseStack, this.font, sizeText, previewX, previewY + previewH + 12, 0xFFD7E8FF);
                
                int textY = previewY + previewH + 24;
                for (String line : entry.lines()) {
                    drawString(poseStack, this.font, line, bubbleX + 6, textY, 0xFFFFFFFF);
                    textY += this.font.lineHeight;
                }
            } else {
                drawString(poseStack, this.font, name, bubbleX + 6, lineY + this.font.lineHeight, 0xFFFFFFFF);
                drawString(poseStack, this.font, sizeText, bubbleX + 6, lineY + this.font.lineHeight * 2, 0xFFD7E8FF);
                int textY = lineY + this.font.lineHeight * 3 + 2;
                for (String line : entry.lines()) {
                    drawString(poseStack, this.font, line, bubbleX + 6, textY, 0xFFFFFFFF);
                    textY += this.font.lineHeight;
                }
            }
            Rect downloadRect = new Rect(bubbleX + entry.bubbleWidth() - 30, bubbleY + 6, bubbleX + entry.bubbleWidth() - 6, bubbleY + 20);
            fill(poseStack, downloadRect.x1, downloadRect.y1, downloadRect.x2, downloadRect.y2, 0x553A4A5B);
            drawCenteredString(poseStack, this.font, Component.literal("↓"), downloadRect.centerX(), downloadRect.y1 + 3, 0xFFFFFFFF);
            fileButtonBoxes.add(new FileButtonBox(message.getAttachmentId(), downloadRect.x1, downloadRect.y1, downloadRect.x2, downloadRect.y2));
        } else if (message.getType() == MaxChatMessage.MessageType.GIFT) {
            MaxGiftOffer gift = MaxClientState.getGift(message.getGiftId());
            drawString(poseStack, this.font, "Подарок", bubbleX + 6, lineY, 0xFFFFFFFF);
            if (gift != null) {
                int slotX = bubbleX + 6;
                int slotY = lineY + this.font.lineHeight + 2;
                int shown = Math.min(4, gift.getOfferedStacks().size());
                for (int i = 0; i < shown; i++) {
                    int col = i % 2;
                    int row = i / 2;
                    drawItemSlot(poseStack, slotX + col * 20, slotY + row * 20, gift.getOfferedStacks().get(i));
                }
                String stateText = switch (gift.getState()) {
                    case ACCEPTED -> "Принято";
                    case REJECTED -> "Отклонено";
                    case FAILED -> gift.getLastError().isBlank() ? "Ошибка" : trim(gift.getLastError(), 18);
                    default -> "Ожидает";
                };
                drawString(poseStack, this.font, stateText, bubbleX + 6, slotY + 44, 0xFFD7E8FF);
                if (gift.getRecipientId().equals(MaxClientState.getSelfId()) && (gift.getState() == MaxGiftOffer.GiftState.PENDING || gift.getState() == MaxGiftOffer.GiftState.FAILED)) {
                    Rect acceptRect = new Rect(bubbleX + 6, slotY + 58, bubbleX + 22, slotY + 72);
                    Rect rejectRect = new Rect(bubbleX + 26, slotY + 58, bubbleX + 42, slotY + 72);
                    fill(poseStack, acceptRect.x1, acceptRect.y1, acceptRect.x2, acceptRect.y2, 0x55307A48);
                    fill(poseStack, rejectRect.x1, rejectRect.y1, rejectRect.x2, rejectRect.y2, 0x557A3030);
                    drawCenteredString(poseStack, this.font, Component.literal("✓"), acceptRect.centerX(), acceptRect.y1 + 3, 0xFFFFFFFF);
                    drawCenteredString(poseStack, this.font, Component.literal("✕"), rejectRect.centerX(), rejectRect.y1 + 3, 0xFFFFFFFF);
                    giftButtonBoxes.add(new GiftButtonBox(gift.getGiftId(), true, acceptRect.x1, acceptRect.y1, acceptRect.x2, acceptRect.y2));
                    giftButtonBoxes.add(new GiftButtonBox(gift.getGiftId(), false, rejectRect.x1, rejectRect.y1, rejectRect.x2, rejectRect.y2));
                }
            }
        } else {
            for (String line : entry.lines()) {
                drawString(poseStack, this.font, line, bubbleX + 6, lineY, 0xFFFFFFFF);
                lineY += this.font.lineHeight;
            }
        }

        drawString(poseStack, this.font, metaText, own ? bubbleX + entry.bubbleWidth() - this.font.width(metaText) : bubbleX, metaY, 0xFF94A3B5);
    }

    private void renderComposer(PoseStack poseStack, int mouseX, int mouseY, Rect input) {
        int attachInset = 20;
        Rect attachRect = getAttachButtonRect();
        fill(poseStack, attachRect.x1, attachRect.y1, attachRect.x2, attachRect.y2, 0x55354758);
        drawCenteredString(poseStack, this.font, Component.literal("+") , attachRect.centerX(), attachRect.y1 + 3, 0xFFFFFFFF);

        int textX = input.x1 + attachInset + 3;
        int barY = input.y1 + 3;
        if (editingMessageId != 0L || replyToMessageId != 0L || pendingAttachment != null || forwardingMessageId != 0L) {
            fill(poseStack, input.x1 + 2, barY, input.x2 - 2, barY + 10, 0x332E557D);
            String barText = buildComposerStatusText();
            drawString(poseStack, this.font, trim(barText, 52), textX, barY + 1, 0xFFD7E8FF);
            Rect clearRect = getComposerClearRect();
            drawCenteredString(poseStack, this.font, Component.literal("✕"), clearRect.centerX(), clearRect.y1 + 1, 0xFFFFFFFF);
            if (pendingAttachment != null) {
                Rect removeRect = getPendingAttachmentRemoveRect();
                drawCenteredString(poseStack, this.font, Component.literal("-"), removeRect.centerX(), removeRect.y1 + 1, 0xFFFFFFFF);
            }
            barY += 11;
        }

        if (messageInput != null) {
            messageInput.setBounds(textX - 4, barY, input.x2 - textX, input.y2 - barY - 2);
            messageInput.render(poseStack, mouseX, mouseY);
            if (messageInput.isBlank() && !messageInput.isFocused()) {
                drawString(poseStack, this.font, "Сообщение...", textX, barY + 2, 0xFF708398);
            }
        }

        if (attachMenuOpen) {
            renderAttachMenu(poseStack, attachRect);
        }
    }

    private void renderAttachMenu(PoseStack poseStack, Rect attachRect) {
        Rect menu = getAttachMenuRect(attachRect);
        fill(poseStack, menu.x1, menu.y1, menu.x2, menu.y2, 0xEE111821);
        Rect fileRect = new Rect(menu.x1 + 4, menu.y1 + 4, menu.x2 - 4, menu.y1 + 18);
        Rect giftRect = new Rect(menu.x1 + 4, menu.y1 + 20, menu.x2 - 4, menu.y1 + 34);
        fill(poseStack, fileRect.x1, fileRect.y1, fileRect.x2, fileRect.y2, 0x33354758);
        fill(poseStack, giftRect.x1, giftRect.y1, giftRect.x2, giftRect.y2, 0x33354758);
        drawString(poseStack, this.font, "Файл", fileRect.x1 + 4, fileRect.y1 + 3, 0xFFFFFFFF);
        drawString(poseStack, this.font, "Подарок", giftRect.x1 + 4, giftRect.y1 + 3, 0xFFFFFFFF);
        attachActionBoxes.add(new AttachActionBox(AttachAction.FILE, fileRect.x1, fileRect.y1, fileRect.x2, fileRect.y2));
        attachActionBoxes.add(new AttachActionBox(AttachAction.GIFT, giftRect.x1, giftRect.y1, giftRect.x2, giftRect.y2));
    }

    private void renderMessageScrollbar(PoseStack poseStack, Rect messagesRect, ChatMetrics metrics) {
        if (metrics.contentHeight() <= metrics.viewportHeight()) {
            return;
        }

        Rect track = getMessageScrollbarTrackRect(messagesRect);
        Rect thumb = getMessageScrollbarThumbRect(messagesRect);
        fill(poseStack, track.x1, track.y1, track.x2, track.y2, 0x55232C38);
        if (thumb != null) {
            fill(poseStack, thumb.x1, thumb.y1, thumb.x2, thumb.y2, draggingMessageScrollbar ? 0xFFA8B7C7 : 0xFF6D7D8F);
        }
    }

    private String buildComposerStatusText() {
        if (editingMessageId != 0L) {
            return "Редактирование сообщения";
        }
        if (pendingAttachment != null) {
            return "Файл: " + pendingAttachment.fileName();
        }
        if (replyToMessageId != 0L) {
            return "Ответ: " + buildReplyPreview(replyToMessageId);
        }
        if (forwardingMessageId != 0L) {
            return "Пересылка: выбери чат слева";
        }
        return "";
    }

    private String buildReplyPreview(long messageId) {
        MaxChatMessage source = MaxClientState.getMessage(messageId);
        if (source == null) {
            return "сообщение";
        }
        if (source.getType() == MaxChatMessage.MessageType.FILE) {
            return "Файл: " + MaxClientState.getAttachmentName(source.getAttachmentId());
        }
        if (source.getType() == MaxChatMessage.MessageType.GIFT) {
            return "Подарок";
        }
        return trim(source.getText().replace('\n', ' '), 28);
    }

    private void clearComposerModes() {
        replyToMessageId = 0L;
        editingMessageId = 0L;
        forwardingMessageId = 0L;
        pendingAttachment = null;
        awaitingChatFileDrop = false;
        attachMenuOpen = false;
    }

    private void openMessageContextMenu(long messageId, int x, int y) {
        contextMenuMessageId = messageId;
        contextMenuX = x;
        contextMenuY = y;
    }

    private void handleMessageAction(MessageAction action, long messageId) {
        MaxChatMessage message = MaxClientState.getMessage(messageId);
        contextMenuMessageId = 0L;
        if (message == null) {
            return;
        }

        switch (action) {
            case REPLY -> {
                editingMessageId = 0L;
                replyToMessageId = messageId;
                chatInputFocused = true;
                statusLine = "Ответ на сообщение";
            }
            case EDIT -> {
                if (message.getSenderId().equals(MaxClientState.getSelfId()) && message.getType() == MaxChatMessage.MessageType.TEXT && !message.isDeleted()) {
                    editingMessageId = messageId;
                    replyToMessageId = 0L;
                    chatDraft = message.getRawText();
                    chatInputFocused = true;
                    statusLine = "Режим редактирования";
                }
            }
            case DELETE -> {
                if (message.getSenderId().equals(MaxClientState.getSelfId())) {
                    MaxPackets.sendToServer(new DeleteMaxMessagePacket(messageId));
                }
            }
            case FORWARD -> {
                forwardingMessageId = messageId;
                statusLine = "Выбери чат слева для пересылки";
            }
        }
    }

    private void renderMessageContextMenu(PoseStack poseStack, int mouseX, int mouseY) {
        if (contextMenuMessageId == 0L) {
            return;
        }
        MaxChatMessage message = MaxClientState.getMessage(contextMenuMessageId);
        if (message == null) {
            contextMenuMessageId = 0L;
            return;
        }

        List<MessageAction> actions = new ArrayList<>();
        actions.add(MessageAction.REPLY);
        actions.add(MessageAction.FORWARD);
        if (message.getSenderId().equals(MaxClientState.getSelfId()) && message.getType() == MaxChatMessage.MessageType.TEXT && !message.isDeleted()) {
            actions.add(0, MessageAction.EDIT);
            actions.add(1, MessageAction.DELETE);
        } else if (message.getSenderId().equals(MaxClientState.getSelfId())) {
            actions.add(0, MessageAction.DELETE);
        }

        int width = 82;
        int height = 4 + actions.size() * 14;
        int x = clamp(contextMenuX, 6, this.width - width - 6);
        int y = clamp(contextMenuY, 6, this.height - height - 6);
        fill(poseStack, x, y, x + width, y + height, 0xEE111821);
        messageActionBoxes.clear();
        for (int i = 0; i < actions.size(); i++) {
            int y1 = y + 2 + i * 14;
            int y2 = y1 + 12;
            fill(poseStack, x + 2, y1, x + width - 2, y2, 0x33354758);
            drawString(poseStack, this.font, actions.get(i).label, x + 6, y1 + 2, 0xFFFFFFFF);
            messageActionBoxes.add(new MessageActionBox(actions.get(i), contextMenuMessageId, x + 2, y1, x + width - 2, y2));
        }
    }

    private void ensureGiftDraftSize() {
        while (giftDraftStacks.size() < 4) {
            giftDraftStacks.add(ItemStack.EMPTY);
            giftDraftSourceSlots.add(-1);
        }
    }

    private void addGiftDraftStack(ItemStack stack, int sourceSlot) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ensureGiftDraftSize();
        if (giftDraftSourceSlots.contains(sourceSlot)) {
            return; // Prevent duplicate selection of same stack
        }
        for (int i = 0; i < giftDraftStacks.size(); i++) {
            if (giftDraftStacks.get(i).isEmpty()) {
                giftDraftStacks.set(i, stack.copy());
                giftDraftSourceSlots.set(i, sourceSlot);
                return;
            }
        }
        statusLine = "Все 4 слота заняты";
    }

    private void sendGiftDraft() {
        List<ItemStack> payload = new ArrayList<>();
        for (ItemStack stack : giftDraftStacks) {
            if (stack != null && !stack.isEmpty()) {
                payload.add(stack.copy());
            }
        }
        if (payload.isEmpty() || selectedContactId == null) {
            return;
        }
        String text = chatDraft.isBlank() ? "Подарок" : chatDraft;
        MaxPackets.sendToServer(new SendMaxGiftPacket(selectedContactId, text, replyToMessageId, payload));
        giftComposerOpen = false;
        giftDraftStacks.clear();
        giftDraftSourceSlots.clear();
        replyToMessageId = 0L;
        chatDraft = "";
        forceScrollToBottom = true;
        statusLine = "Подарок отправлен";
        MaxPackets.sendToServer(new RequestMaxSyncPacket());
    }

    private void renderGiftComposerOverlay(PoseStack poseStack, int mouseX, int mouseY) {
        fill(poseStack, 0, 0, this.width, this.height, 0xA0000000);
        Rect box = getGiftComposerRect();
        fill(poseStack, box.x1, box.y1, box.x2, box.y2, 0xEE111821);
        fill(poseStack, box.x1, box.y1, box.x2, box.y1 + 18, 0xFF1B2734);
        drawString(poseStack, this.font, "Отправить подарок", box.x1 + 6, box.y1 + 6, 0xFFFFFFFF);
        ensureGiftDraftSize();
        giftDraftSlotBoxes.clear();
        giftInventoryBoxes.clear();

        int slotStartX = box.x1 + 12;
        int slotStartY = box.y1 + 26;
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = slotStartX + col * 22;
            int y = slotStartY + row * 22;
            drawItemSlot(poseStack, x, y, giftDraftStacks.get(i));
            giftDraftSlotBoxes.add(new GiftDraftSlotBox(i, x, y, x + 20, y + 20));
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int startX = box.x1 + 62;
            int startY = box.y1 + 26;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = 9 + row * 9 + col;
                    ItemStack stack = mc.player.getInventory().getItem(slotIndex);
                    int x = startX + col * 18;
                    int y = startY + row * 18;
                    fill(poseStack, x, y, x + 18, y + 18, 0xFF2A3440);
                    fill(poseStack, x + 1, y + 1, x + 17, y + 17, 0xFF0F161F);
                    if (!stack.isEmpty()) {
                        if (!giftDraftSourceSlots.contains(slotIndex)) {
                            mc.getItemRenderer().renderAndDecorateItem(stack, x + 1, y + 1);
                            mc.getItemRenderer().renderGuiItemDecorations(this.font, stack, x + 1, y + 1);
                            giftInventoryBoxes.add(new GiftInventoryBox(slotIndex, stack.copy(), x, y, x + 18, y + 18));
                        } else {
                            fill(poseStack, x + 1, y + 1, x + 17, y + 17, 0x88000000);
                        }
                    }
                }
            }
            int hotbarY = startY + 3 * 18 + 4;
            for (int col = 0; col < 9; col++) {
                int slotIndex = col;
                ItemStack stack = mc.player.getInventory().getItem(slotIndex);
                int x = startX + col * 18;
                int y = hotbarY;
                fill(poseStack, x, y, x + 18, y + 18, 0xFF2A3440);
                fill(poseStack, x + 1, y + 1, x + 17, y + 17, 0xFF0F161F);
                if (!stack.isEmpty()) {
                    if (!giftDraftSourceSlots.contains(slotIndex)) {
                        mc.getItemRenderer().renderAndDecorateItem(stack, x + 1, y + 1);
                        mc.getItemRenderer().renderGuiItemDecorations(this.font, stack, x + 1, y + 1);
                        giftInventoryBoxes.add(new GiftInventoryBox(slotIndex, stack.copy(), x, y, x + 18, y + 18));
                    } else {
                        fill(poseStack, x + 1, y + 1, x + 17, y + 17, 0x88000000);
                    }
                }
            }
        }

        Rect sendRect = getGiftSendButtonRect();
        Rect cancelRect = getGiftCancelButtonRect();
        fill(poseStack, sendRect.x1, sendRect.y1, sendRect.x2, sendRect.y2, 0x55307A48);
        fill(poseStack, cancelRect.x1, cancelRect.y1, cancelRect.x2, cancelRect.y2, 0x553A2020);
        drawCenteredString(poseStack, this.font, Component.literal("Отправить"), sendRect.centerX(), sendRect.y1 + 4, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("Отмена"), cancelRect.centerX(), cancelRect.y1 + 4, 0xFFFFFFFF);
    }

    private Rect getGiftComposerRect() {
        int w = 240;
        int h = 150;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        return new Rect(x, y, x + w, y + h);
    }

    private Rect getGiftSendButtonRect() {
        Rect box = getGiftComposerRect();
        return new Rect(box.x2 - 80, box.y2 - 18, box.x2 - 8, box.y2 - 4);
    }

    private Rect getGiftCancelButtonRect() {
        Rect box = getGiftComposerRect();
        return new Rect(box.x2 - 156, box.y2 - 18, box.x2 - 84, box.y2 - 4);
    }

    private Rect getAttachButtonRect() {
        Rect input = getChatInputRect();
        return new Rect(input.x1 + 2, input.y1 + 2, input.x1 + 18, input.y1 + 18);
    }

    private Rect getAttachMenuRect(Rect attachRect) {
        return new Rect(attachRect.x1, attachRect.y1 - 36, attachRect.x1 + 72, attachRect.y1 - 2);
    }

    private Rect getComposerClearRect() {
        Rect input = getChatInputRect();
        return new Rect(input.x2 - 18, input.y1 + 3, input.x2 - 4, input.y1 + 13);
    }

    private Rect getPendingAttachmentRemoveRect() {
        Rect input = getChatInputRect();
        return new Rect(input.x2 - 32, input.y1 + 3, input.x2 - 20, input.y1 + 13);
    }

    private String formatBytes(int bytes) {
        if (bytes >= 1024 * 1024) {
            return (bytes / (1024 * 1024)) + " МБ";
        }
        if (bytes >= 1024) {
            return (bytes / 1024) + " КБ";
        }
        return bytes + " Б";
    }

    private byte[] buildAttachmentPreviewBytes(Path path, String mimeType) {
        try {
            String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
            String lowerName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            boolean image = mime.startsWith("image/") || lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".bmp") || lowerName.endsWith(".gif") || lowerName.endsWith(".webp");
            if (!image) {
                return new byte[0];
            }
            BufferedImage source = ImageIO.read(path.toFile());
            if (source == null) {
                return new byte[0];
            }
            int targetW = 160;
            int targetH = 120;
            double scale = Math.min(targetW / (double) source.getWidth(), targetH / (double) source.getHeight());
            int drawW = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int drawH = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int drawX = (targetW - drawW) / 2;
            int drawY = (targetH - drawH) / 2;
            BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, drawX, drawY, drawW, drawH, null);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", output);
            return output.toByteArray();
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    private void startChunkedAttachmentUpload(UUID recipientId, String caption, long replyToMessageId, PendingAttachment attachment) {
        if (recipientId == null || attachment == null || attachment.path() == null) {
            return;
        }
        UUID attachmentId = UUID.randomUUID();
        MaxClientTransferState.startUpload(attachmentId, attachment.fileName(), attachment.sizeBytes());
        new Thread(() -> {
            try {
                MaxPackets.sendToServer(new StartMaxAttachmentUploadPacket(
                        attachmentId,
                        recipientId,
                        caption,
                        replyToMessageId,
                        attachment.fileName(),
                        attachment.mimeType(),
                        attachment.sizeBytes(),
                        attachment.previewBytes()
                ));

                byte[] buffer = new byte[30 * 1024];
                try (java.io.InputStream input = Files.newInputStream(attachment.path())) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        byte[] chunk = new byte[read];
                        System.arraycopy(buffer, 0, chunk, 0, read);
                        MaxPackets.sendToServer(new MaxAttachmentUploadChunkPacket(attachmentId, chunk));
                        MaxClientTransferState.advanceUpload(attachmentId, read);
                    }
                }

                MaxPackets.sendToServer(new FinishMaxAttachmentUploadPacket(attachmentId));
                MaxClientTransferState.finishUpload(attachmentId, true, "Отправлено");
                Minecraft.getInstance().execute(() -> {
                    statusLine = "Файл отправлен";
                    MaxPackets.sendToServer(new RequestMaxSyncPacket());
                });
            } catch (Exception exception) {
                MaxClientTransferState.finishUpload(attachmentId, false, "Ошибка");
                Minecraft.getInstance().execute(() -> statusLine = "Ошибка отправки файла: " + exception.getClass().getSimpleName());
            }
        }, "MAX-Chunk-Upload").start();
    }

    private void renderTransfersOverlay(PoseStack poseStack) {
        MaxClientTransferState.cleanupFinished();
        List<MaxClientTransferState.TransferProgressSnapshot> transfers = MaxClientTransferState.getSnapshots();
        if (transfers.isEmpty()) {
            return;
        }
        int shown = Math.min(2, transfers.size());
        int boxW = 156;
        int boxH = shown * 22 + 6;
        int x = this.width - boxW - 8;
        int y = 8;
        fill(poseStack, x, y, x + boxW, y + boxH, 0xD0111821);
        for (int i = 0; i < shown; i++) {
            MaxClientTransferState.TransferProgressSnapshot transfer = transfers.get(i);
            int rowY = y + 4 + i * 22;
            String title = (transfer.direction() == MaxClientTransferState.Direction.UPLOAD ? "↑ " : "↓ ") + trim(transfer.fileName(), 18);
            drawString(poseStack, this.font, title, x + 6, rowY, 0xFFFFFFFF);
            drawString(poseStack, this.font, transfer.percent() + "%", x + boxW - 24, rowY, 0xFFD7E8FF);
            fill(poseStack, x + 6, rowY + 10, x + boxW - 6, rowY + 14, 0x55354758);
            int progressW = (boxW - 12) * transfer.percent() / 100;
            fill(poseStack, x + 6, rowY + 10, x + 6 + progressW, rowY + 14, transfer.finished() ? (transfer.success() ? 0xFF3AA35C : 0xFFB04040) : 0xFF3C89FF);
            drawString(poseStack, this.font, trim(transfer.status(), 22), x + 6, rowY + 15, 0xFF9CB0C3);
        }
    }

    private void renderSelfProfileCard(PoseStack poseStack, int mouseX, int mouseY) {
        MaxUserProfile self = MaxClientState.getSelfProfile();
        if (self == null) {
            return;
        }

        Rect rect = getSelfProfileRect();
        int bg = rect.contains(mouseX, mouseY) ? 0xFF1C2937 : 0xFF161E28;
        fill(poseStack, rect.x1, rect.y1, rect.x2, rect.y2, bg);
        drawAvatarSquare(poseStack, self.getAvatarId(), rect.x1 + 6, rect.y1 + 6, 20, true, true);
        drawString(poseStack, this.font, trim(self.getDisplayName(), 12), rect.x1 + 30, rect.y1 + 6, 0xFFFFFFFF);
        drawString(poseStack, this.font, trim(self.getDescription().isBlank() ? "профиль" : self.getDescription(), 14), rect.x1 + 30, rect.y1 + 16, 0xFF8FA0B2);
    }

    private void renderProfileEditor(PoseStack poseStack, int mouseX, int mouseY) {
        Rect modal = getEditorRect();
        boolean registration = viewMode == ViewMode.REGISTER;

        fill(poseStack, modal.x1, modal.y1, modal.x2, modal.y2, 0xFF151D27);
        fill(poseStack, modal.x1, modal.y1, modal.x2, modal.y1 + 22, 0xFF1C2632);

        drawString(poseStack, this.font, registration ? "Регистрация MAX" : "Профиль MAX", modal.x1 + 8, modal.y1 + 7, 0xFFFFFFFF);
        drawString(poseStack, this.font, registration ? "выбери аву и подпиши поля" : "измени профиль", modal.x1 + 8, modal.y1 + 17, 0xFF8798AA);

        int leftX = modal.x1 + 8;
        int topY = modal.y1 + 32;
        drawString(poseStack, this.font, "Аватарка", leftX, topY, 0xFFFFFFFF);

        avatarBoxes.clear();
        int tile = 28;
        int gap = 6;
        int gridY = topY + 10;

        for (int i = 0; i < STOCK_AVATAR_IDS.size(); i++) {
            String avatarId = STOCK_AVATAR_IDS.get(i);
            int col = i % 3;
            int row = i / 3;
            int x = leftX + col * (tile + gap);
            int y = gridY + row * (tile + gap);
            boolean selected = selectedAvatarId.equals(avatarId);
            fill(poseStack, x - 1, y - 1, x + tile + 1, y + tile + 1, selected ? 0xFF4B92FF : 0xFF2A3544);
            drawAvatarSquare(poseStack, avatarId, x, y, tile, false, false);
            avatarBoxes.add(new AvatarClickBox(avatarId, x - 1, y - 1, x + tile + 1, y + tile + 1));
        }

        int uploadCol = 2;
        int uploadRow = 1;
        int uploadX = leftX + uploadCol * (tile + gap);
        int uploadY = gridY + uploadRow * (tile + gap);
        boolean uploadSelected = selectedAvatarId.startsWith("image:");
        fill(poseStack, uploadX - 1, uploadY - 1, uploadX + tile + 1, uploadY + tile + 1, uploadSelected ? 0xFF4B92FF : 0xFF2A3544);
        fill(poseStack, uploadX, uploadY, uploadX + tile, uploadY + tile, 0xFF16202B);
        drawCenteredString(poseStack, this.font, Component.literal("+"), uploadX + tile / 2, uploadY + 8, 0xFFFFFFFF);
        avatarBoxes.add(new AvatarClickBox(UPLOAD_AVATAR_ID, uploadX - 1, uploadY - 1, uploadX + tile + 1, uploadY + tile + 1));
        drawString(poseStack, this.font, "с компа", uploadX - 4, uploadY + tile + 3, 0xFF8A9AAD);

        int previewX = leftX;
        int previewY = modal.y1 + 116;
        drawString(poseStack, this.font, "Превью", previewX, previewY, 0xFFFFFFFF);
        drawAvatarSquare(poseStack, selectedAvatarId, previewX, previewY + 10, 58, true, false);

        int textX = modal.x1 + 126;
        drawString(poseStack, this.font, "Ник", textX, modal.y1 + 72, 0xFFFFFFFF);
        drawString(poseStack, this.font, "Описание", textX, modal.y1 + 106, 0xFFFFFFFF);

        drawString(poseStack, this.font, "Витрина", textX, modal.y1 + 144, 0xFFFFFFFF);
        ItemStack showcase = getSelectedShowcaseStack();
        drawItemSlot(poseStack, textX, modal.y1 + 154, showcase);
        drawString(poseStack, this.font, trim(showcase.isEmpty() ? "нажми и выбери" : showcase.getHoverName().getString(), 18), textX + 24, modal.y1 + 159, 0xFFB0C0D1);
        drawString(poseStack, this.font, "кликни по слоту и выбери из инвентаря", textX, modal.y1 + 177, 0xFF7F90A2);

        if (getEditorShowcaseSlotRect(modal).contains(mouseX, mouseY) && !showcase.isEmpty() && !showcasePickerOpen) {
            renderTooltip(poseStack, showcase, mouseX, mouseY);
        }
    }

    private void renderAvatarDropOverlay(PoseStack poseStack, Rect editor) {
        fill(poseStack, 0, 0, this.width, this.height, 0xB0000000);
        Rect box = new Rect(editor.x1 + 24, editor.y1 + 34, editor.x2 - 24, editor.y2 - 34);
        fill(poseStack, box.x1, box.y1, box.x2, box.y2, 0xEE111821);
        fill(poseStack, box.x1, box.y1, box.x2, box.y1 + 18, 0xFF1B2734);
        drawCenteredString(poseStack, this.font, Component.literal("Загрузка аватарки"), box.centerX(), box.y1 + 6, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("Перетащи PNG/JPG/BMP прямо в окно игры"), box.centerX(), box.centerY() - 10, 0xFFD7E8FF);
        drawCenteredString(poseStack, this.font, Component.literal("Клик мышкой — отмена"), box.centerX(), box.centerY() + 4, 0xFF8FA0B2);
    }

    private void renderAvatarCropOverlay(PoseStack poseStack) {
        fill(poseStack, 0, 0, this.width, this.height, 0xC0000000);
        Rect box = getAvatarCropBoxRect();
        Rect imageRect = getAvatarCropImageRect();
        fill(poseStack, box.x1, box.y1, box.x2, box.y2, 0xEE111821);
        fill(poseStack, box.x1, box.y1, box.x2, box.y1 + 18, 0xFF1B2734);
        drawString(poseStack, this.font, "Обрезка аватарки", box.x1 + 8, box.y1 + 6, 0xFFFFFFFF);

        if (avatarCropTexture != null) {
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, avatarCropTexture);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            GuiComponent.blit(poseStack, imageRect.x1, imageRect.y1, 0, 0, imageRect.width(), imageRect.height(), imageRect.width(), imageRect.height());
        } else {
            fill(poseStack, imageRect.x1, imageRect.y1, imageRect.x2, imageRect.y2, 0xFF202A35);
        }

        fill(poseStack, imageRect.x1, imageRect.y1, imageRect.x2, avatarCropBoxY, 0x88000000);
        fill(poseStack, imageRect.x1, avatarCropBoxY + avatarCropBoxSize, imageRect.x2, imageRect.y2, 0x88000000);
        fill(poseStack, imageRect.x1, avatarCropBoxY, avatarCropBoxX, avatarCropBoxY + avatarCropBoxSize, 0x88000000);
        fill(poseStack, avatarCropBoxX + avatarCropBoxSize, avatarCropBoxY, imageRect.x2, avatarCropBoxY + avatarCropBoxSize, 0x88000000);
        fill(poseStack, avatarCropBoxX, avatarCropBoxY, avatarCropBoxX + avatarCropBoxSize, avatarCropBoxY + 1, 0xFFFFFFFF);
        fill(poseStack, avatarCropBoxX, avatarCropBoxY + avatarCropBoxSize - 1, avatarCropBoxX + avatarCropBoxSize, avatarCropBoxY + avatarCropBoxSize, 0xFFFFFFFF);
        fill(poseStack, avatarCropBoxX, avatarCropBoxY, avatarCropBoxX + 1, avatarCropBoxY + avatarCropBoxSize, 0xFFFFFFFF);
        fill(poseStack, avatarCropBoxX + avatarCropBoxSize - 1, avatarCropBoxY, avatarCropBoxX + avatarCropBoxSize, avatarCropBoxY + avatarCropBoxSize, 0xFFFFFFFF);

        Rect cancel = getAvatarCropCancelRect();
        Rect apply = getAvatarCropApplyRect();
        fill(poseStack, cancel.x1, cancel.y1, cancel.x2, cancel.y2, 0x553A2020);
        fill(poseStack, apply.x1, apply.y1, apply.x2, apply.y2, 0x55307A48);
        drawCenteredString(poseStack, this.font, Component.literal("Отмена"), cancel.centerX(), cancel.y1 + 4, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("Обрезать"), apply.centerX(), apply.y1 + 4, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("Колёсико — менять размер, мышью — двигать"), box.centerX(), box.y2 - 30, 0xFFD7E8FF);
    }

    private void renderChatFileDropOverlay(PoseStack poseStack) {
        fill(poseStack, 0, 0, this.width, this.height, 0x88000000);
        Rect box = new Rect(this.width / 2 - 110, this.height / 2 - 26, this.width / 2 + 110, this.height / 2 + 26);
        fill(poseStack, box.x1, box.y1, box.x2, box.y2, 0xEE111821);
        fill(poseStack, box.x1, box.y1, box.x2, box.y1 + 18, 0xFF1B2734);
        drawCenteredString(poseStack, this.font, Component.literal("Отправка файла"), box.centerX(), box.y1 + 6, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, Component.literal("Перетащи файл в окно игры"), box.centerX(), box.centerY() - 6, 0xFFD7E8FF);
        drawCenteredString(poseStack, this.font, Component.literal("Клик мышкой — отмена"), box.centerX(), box.centerY() + 6, 0xFF8FA0B2);
    }

    private void renderImagePreviewOverlay(PoseStack poseStack) {
        MaxFileAttachment attachment = MaxClientState.getAttachment(openedImageAttachmentId);
        if (attachment == null || attachment.getPreviewImageBytes().length == 0) {
            return;
        }
        fill(poseStack, 0, 0, this.width, this.height, 0xD0000000);
        Rect box = new Rect(24, 24, this.width - 24, this.height - 24);
        fill(poseStack, box.x1, box.y1, box.x2, box.y2, 0xEE111821);
        ResourceLocation texture = getOrCreateAttachmentPreviewTexture(openedImageAttachmentId, attachment.getPreviewImageBytes());
        int[] size = attachmentPreviewSizes.get(openedImageAttachmentId);
        int imageW = (size != null && size[0] > 0) ? size[0] : 256;
        int imageH = (size != null && size[1] > 0) ? size[1] : 192;
        int drawW = box.width() - 16;
        int drawH = drawW * imageH / imageW;
        if (drawH > box.height() - 32) {
            drawH = box.height() - 32;
            drawW = drawH * imageW / imageH;
        }
        int drawX = box.x1 + (box.width() - drawW) / 2;
        int drawY = box.y1 + (box.height() - 16 - drawH) / 2;
        if (texture != null) {
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            GuiComponent.blit(poseStack, drawX, drawY, 0, 0, drawW, drawH, drawW, drawH);
        }
        drawCenteredString(poseStack, this.font, Component.literal(trim(attachment.getFileName(), 40)), box.centerX(), box.y2 - 14, 0xFFFFFFFF);
    }

    private void renderShowcasePicker(PoseStack poseStack, int mouseX, int mouseY, Rect editor) {
        Rect picker = getShowcasePickerRect(editor);
        fill(poseStack, picker.x1, picker.y1, picker.x2, picker.y2, 0xEE111821);
        fill(poseStack, picker.x1, picker.y1, picker.x2, picker.y1 + 18, 0xFF1B2734);
        drawString(poseStack, this.font, "Выбери трофей", picker.x1 + 6, picker.y1 + 6, 0xFFFFFFFF);

        showcaseClickBoxes.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int slotSize = 18;
        int startX = picker.x1 + 8;
        int startY = picker.y1 + 24;

        ShowcaseClickBox hovered = null;
        ShowcaseClickBox clearBox = new ShowcaseClickBox("", ItemStack.EMPTY, picker.x2 - 26, picker.y1 + 2, picker.x2 - 8, picker.y1 + 20);
        showcaseClickBoxes.add(clearBox);
        fill(poseStack, clearBox.x1(), clearBox.y1(), clearBox.x2(), clearBox.y2(), 0xFF3A2020);
        drawCenteredString(poseStack, this.font, Component.literal("X"), (clearBox.x1() + clearBox.x2()) / 2, clearBox.y1() + 5, 0xFFFFFFFF);
        if (clearBox.contains(mouseX, mouseY)) {
            hovered = clearBox;
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getItem(slotIndex);
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;
                hovered = renderShowcaseInventorySlot(poseStack, mouseX, mouseY, hovered, stack, x, y);
            }
        }

        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            ItemStack stack = mc.player.getInventory().getItem(slotIndex);
            int x = startX + col * slotSize;
            int y = startY + 3 * slotSize + 4;
            hovered = renderShowcaseInventorySlot(poseStack, mouseX, mouseY, hovered, stack, x, y);
        }

        if (hovered != null) {
            if (hovered.stack().isEmpty()) {
                renderTooltip(poseStack, Component.literal("Очистить трофей"), mouseX, mouseY);
            } else {
                renderTooltip(poseStack, hovered.stack(), mouseX, mouseY);
            }
        }
    }

    private ShowcaseClickBox renderShowcaseInventorySlot(PoseStack poseStack, int mouseX, int mouseY, ShowcaseClickBox hovered, ItemStack stack, int x, int y) {
        fill(poseStack, x, y, x + 18, y + 18, 0xFF2A3440);
        fill(poseStack, x + 1, y + 1, x + 17, y + 17, 0xFF0F161F);
        if (!stack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            mc.getItemRenderer().renderAndDecorateItem(stack, x + 1, y + 1);
            mc.getItemRenderer().renderGuiItemDecorations(this.font, stack, x + 1, y + 1);
        }

        if (stack.isEmpty()) {
            return hovered;
        }

        String itemId = "";
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null) {
            itemId = key.toString();
        }

        ShowcaseClickBox box = new ShowcaseClickBox(itemId, stack.copy(), x, y, x + 18, y + 18);
        showcaseClickBoxes.add(box);
        if (box.contains(mouseX, mouseY)) {
            return box;
        }
        return hovered;
    }

    private void renderProfileView(PoseStack poseStack, int mouseX, int mouseY) {
        Rect modal = getProfileViewRect();
        fill(poseStack, modal.x1, modal.y1, modal.x2, modal.y2, 0xFF151D27);
        fill(poseStack, modal.x1, modal.y1, modal.x2, modal.y1 + 22, 0xFF1C2632);
        drawString(poseStack, this.font, "Профиль MAX", modal.x1 + 8, modal.y1 + 7, 0xFFFFFFFF);

        MaxUserProfile profile = viewedProfileId == null ? null : MaxClientState.getProfile(viewedProfileId);
        if (viewedProfileId != null && viewedProfileId.equals(MaxClientState.getSelfId())) {
            profile = MaxClientState.getSelfProfile();
        }

        if (profile == null) {
            drawCenteredString(poseStack, this.font, Component.literal("Профиль не найден"), modal.centerX(), modal.centerY(), 0xFFFFFFFF);
            return;
        }

        drawAvatarSquare(poseStack, profile.getAvatarId(), modal.x1 + 10, modal.y1 + 34, 54, viewedProfileId == null || viewedProfileId.equals(MaxClientState.getSelfId()) || isOnline(profile.getUserId()), false);

        int textX = modal.x1 + 74;
        drawString(poseStack, this.font, "Ник", textX, modal.y1 + 34, 0xFF8A9CAE);
        drawString(poseStack, this.font, profile.getDisplayName(), textX, modal.y1 + 44, 0xFFFFFFFF);

        drawString(poseStack, this.font, "Логин", textX, modal.y1 + 62, 0xFF8A9CAE);
        drawString(poseStack, this.font, trim(profile.getBaseName(), 24), textX, modal.y1 + 72, 0xFFFFFFFF);

        drawString(poseStack, this.font, "Описание", textX, modal.y1 + 90, 0xFF8A9CAE);
        List<String> aboutLines = wrapText(profile.getDescription().isBlank() ? "Описание пока пустое" : profile.getDescription(), modal.width() - 86);
        int lineY = modal.y1 + 100;
        for (int i = 0; i < Math.min(3, aboutLines.size()); i++) {
            drawString(poseStack, this.font, aboutLines.get(i), textX, lineY, 0xFFFFFFFF);
            lineY += this.font.lineHeight;
        }

        drawString(poseStack, this.font, "Витрина", modal.x1 + 10, modal.y1 + 102, 0xFF8A9CAE);
        ItemStack showcaseStack = getShowcaseStack(profile.getShowcaseItemId());
        Rect showcaseRect = new Rect(modal.x1 + 10, modal.y1 + 112, modal.x1 + 30, modal.y1 + 132);
        drawItemSlot(poseStack, showcaseRect.x1, showcaseRect.y1, showcaseStack);
        drawString(poseStack, this.font, trim(getShowcaseName(profile.getShowcaseItemId()), 22), modal.x1 + 34, modal.y1 + 117, 0xFFFFFFFF);
        if (showcaseRect.contains(mouseX, mouseY) && !showcaseStack.isEmpty()) {
            renderTooltip(poseStack, showcaseStack, mouseX, mouseY);
        }

        drawString(poseStack, this.font, "UUID", modal.x1 + 10, modal.y1 + 136, 0xFF8A9CAE);
        drawString(poseStack, this.font, shortenUuid(profile.getUserId()), modal.x1 + 38, modal.y1 + 136, 0xFFFFFFFF);
    }

    private void drawItemSlot(PoseStack poseStack, int x, int y, ItemStack stack) {
        fill(poseStack, x, y, x + 20, y + 20, 0xFF2C3640);
        fill(poseStack, x + 1, y + 1, x + 19, y + 19, 0xFF0F161F);
        if (!stack.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            mc.getItemRenderer().renderAndDecorateItem(stack, x + 2, y + 2);
            mc.getItemRenderer().renderGuiItemDecorations(this.font, stack, x + 2, y + 2);
        }
    }

    private ChatMetrics buildChatMetrics(Rect messagesRect) {
        List<ChatEntry> entries = new ArrayList<>();
        List<MaxChatMessage> conversation = selectedContactId == null ? List.of() : MaxClientState.getConversation(selectedContactId);
        UUID selfId = MaxClientState.getSelfId();
        int contentHeight = 0;
        LocalDate lastDay = null;
        int availableWidth = Math.max(60, messagesRect.width() - 42);
        int maxBubbleWidth = Math.max(48, availableWidth - 46);

        for (MaxChatMessage message : conversation) {
            LocalDate currentDay = Instant.ofEpochMilli(message.getSentAtMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!currentDay.equals(lastDay)) {
                String dayText = formatDay(currentDay);
                entries.add(ChatEntry.day(dayText, 14));
                contentHeight += 14;
                lastDay = currentDay;
            }

            boolean own = selfId != null && message.getSenderId().equals(selfId);
            List<String> lines = new ArrayList<>();
            int bubbleWidth;
            int bubbleHeight;

            if (message.isDeleted()) {
                lines.add("Сообщение удалено");
                bubbleWidth = Math.min(maxBubbleWidth, this.font.width("Сообщение удалено") + 12);
                bubbleHeight = 6 + this.font.lineHeight;
                String metaText = formatTime(message.getSentAtMillis());
                int totalHeight = bubbleHeight + 10;
                entries.add(ChatEntry.message(message, own, lines, bubbleWidth, bubbleHeight, totalHeight, metaText));
                contentHeight += totalHeight;
                continue;
            }

            if (message.getType() == MaxChatMessage.MessageType.FILE) {
                MaxFileAttachment attachment = MaxClientState.getAttachment(message.getAttachmentId());
                bubbleWidth = Math.min(maxBubbleWidth, attachment != null && attachment.isImage() ? 188 : 170);
                
                if (!message.getText().isBlank()) {
                    lines.addAll(wrapText(message.getText(), bubbleWidth - 12));
                }

                if (attachment != null && attachment.isImage()) {
                    int previewW = Math.min(bubbleWidth - 12, 160);
                    int previewH = Math.max(48, previewW * 192 / 256);
                    int[] size = attachmentPreviewSizes.get(message.getAttachmentId());
                    if (size != null && size[0] > 0) {
                        previewH = previewW * size[1] / size[0];
                    }
                    bubbleHeight = this.font.lineHeight + previewH + 34 + lines.size() * this.font.lineHeight;
                } else {
                    bubbleHeight = this.font.lineHeight * 3 + 10 + lines.size() * this.font.lineHeight;
                }
            } else if (message.getType() == MaxChatMessage.MessageType.GIFT) {
                bubbleWidth = Math.min(maxBubbleWidth, 120);
                bubbleHeight = 92;
            } else if (message.getType() == MaxChatMessage.MessageType.SYSTEM) {
                lines.add(message.getText());
                bubbleWidth = maxBubbleWidth;
                bubbleHeight = 12;
            } else {
                lines = wrapText(message.getText(), maxBubbleWidth - 12);
                int widest = 12;
                for (String line : lines) {
                    widest = Math.max(widest, this.font.width(line) + 12);
                }
                bubbleWidth = clamp(widest, 22, maxBubbleWidth);
                bubbleHeight = 6 + lines.size() * this.font.lineHeight;
            }

            if (message.getType() != MaxChatMessage.MessageType.SYSTEM) {
                if (message.isForwarded()) {
                    bubbleHeight += this.font.lineHeight;
                }
                if (message.getReplyToMessageId() > 0L) {
                    bubbleHeight += this.font.lineHeight + 4;
                }
            }

            String metaText = formatTime(message.getSentAtMillis()) + (message.isEdited() ? " изм." : "") + (own ? (message.isRead() ? " ✓✓" : " ✓") : "");
            int totalHeight = bubbleHeight + 10;
            entries.add(ChatEntry.message(message, own, lines, bubbleWidth, bubbleHeight, totalHeight, metaText));
            contentHeight += totalHeight;
        }

        int viewportHeight = messagesRect.height() - 8;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        return new ChatMetrics(entries, contentHeight, viewportHeight, maxScroll);
    }

    private void rebuildContactRows() {
        contactRows.clear();
        Rect contactsRect = getContactsRect();
        List<SidebarContact> contacts = getSidebarContacts();
        int rowHeight = 36;
        int visibleRows = Math.max(1, contactsRect.height() / rowHeight);
        int maxScroll = Math.max(0, contacts.size() - visibleRows);
        if (contactScroll > maxScroll) {
            contactScroll = maxScroll;
        }

        int y = contactsRect.y1;
        for (int i = 0; i < visibleRows && contactScroll + i < contacts.size(); i++) {
            SidebarContact contact = contacts.get(contactScroll + i);
            contactRows.add(new ContactRow(
                    contact.userId,
                    contact.displayName,
                    contact.avatarId,
                    contact.preview,
                    contact.unreadCount,
                    contact.online,
                    contact.favorite,
                    contactsRect.x1,
                    y,
                    contactsRect.x2,
                    y + rowHeight - 2
            ));
            y += rowHeight;
        }
    }

    private List<SidebarContact> getSidebarContacts() {
        List<SidebarContact> result = new ArrayList<>();

        UUID selfId = MaxClientState.getSelfId();
        MaxUserProfile self = MaxClientState.getSelfProfile();
        if (selfId != null && self != null) {
            String preview = MaxClientState.getLastMessagePreview(selfId);
            if (preview.isBlank()) {
                preview = "заметки";
            }
            result.add(new SidebarContact(selfId, "Избранное", "favorite_flag", preview, 0, true, true));
        }

        for (MaxUserProfile profile : MaxClientState.getContacts()) {
            String preview = MaxClientState.getLastMessagePreview(profile.getUserId());
            if (preview.isBlank()) {
                preview = profile.getDescription().isBlank() ? "пусто" : profile.getDescription();
            }
            result.add(new SidebarContact(profile.getUserId(), profile.getDisplayName(), profile.getAvatarId(), preview, MaxClientState.getUnreadCount(profile.getUserId()), isOnline(profile.getUserId()), false));
        }

        return result;
    }

    private boolean isOnline(UUID userId) {
        if (Minecraft.getInstance().getConnection() == null || userId == null) {
            return false;
        }
        return Minecraft.getInstance().getConnection().getOnlinePlayers().stream().anyMatch(info -> info.getProfile().getId().equals(userId));
    }

    private Rect getSidebarRect() {
        return new Rect(6, 6, 148, this.height - 6);
    }

    private Rect getContentRect() {
        return new Rect(152, 6, this.width - 6, this.height - 6);
    }

    private Rect getContactsRect() {
        Rect sidebar = getSidebarRect();
        return new Rect(sidebar.x1 + 4, sidebar.y1 + 24, sidebar.x2 - 4, sidebar.y2 - 46);
    }

    private Rect getSelfProfileRect() {
        Rect sidebar = getSidebarRect();
        return new Rect(sidebar.x1 + 4, sidebar.y2 - 40, sidebar.x2 - 4, sidebar.y2 - 4);
    }

    private Rect getChatHeaderRect() {
        Rect content = getContentRect();
        return new Rect(content.x1 + 4, content.y1 + 4, content.x2 - 4, content.y1 + 28);
    }

    private Rect getMessagesRect() {
        Rect content = getContentRect();
        int inputHeight = getChatInputHeight();
        return new Rect(content.x1 + 4, content.y1 + 30, content.x2 - 4, content.y2 - inputHeight - 6);
    }

    private Rect getChatInputRect() {
        Rect content = getContentRect();
        int inputHeight = getChatInputHeight();
        return new Rect(content.x1 + 4, content.y2 - inputHeight - 4, content.x2 - 26, content.y2 - 4);
    }

    private int getChatInputHeight() {
        int baseHeight = 24;
        if (messageInput != null) {
            int maxLines = Math.max(2, this.height / 3 / Math.max(1, this.font.lineHeight));
            baseHeight = messageInput.getPreferredHeight(2, maxLines);
        }
        int extra = (editingMessageId != 0L || replyToMessageId != 0L || pendingAttachment != null || forwardingMessageId != 0L) ? 16 : 0;
        return baseHeight + extra;
    }

    private Rect getEditorRect() {
        int w = Math.min(330, this.width - 24);
        int h = Math.min(220, this.height - 20);
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        return new Rect(x, y, x + w, y + h);
    }

    private Rect getAvatarCropBoxRect() {
        int w = Math.min(292, this.width - 24);
        int h = Math.min(320, this.height - 20);
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        return new Rect(x, y, x + w, y + h);
    }

    private Rect getAvatarCropImageRect() {
        Rect box = getAvatarCropBoxRect();
        int maxW = box.width() - 24;
        int maxH = box.height() - 70;
        if (avatarCropSourceImage == null || avatarCropSourceImage.getWidth() <= 0 || avatarCropSourceImage.getHeight() <= 0) {
            return new Rect(box.x1 + 12, box.y1 + 24, box.x1 + 12 + maxW, box.y1 + 24 + maxH);
        }
        double scale = Math.min(maxW / (double) avatarCropSourceImage.getWidth(), maxH / (double) avatarCropSourceImage.getHeight());
        int drawW = Math.max(1, (int) Math.round(avatarCropSourceImage.getWidth() * scale));
        int drawH = Math.max(1, (int) Math.round(avatarCropSourceImage.getHeight() * scale));
        int x = box.x1 + (box.width() - drawW) / 2;
        int y = box.y1 + 24 + (maxH - drawH) / 2;
        return new Rect(x, y, x + drawW, y + drawH);
    }

    private Rect getAvatarCropSelectionRect() {
        return new Rect(avatarCropBoxX, avatarCropBoxY, avatarCropBoxX + avatarCropBoxSize, avatarCropBoxY + avatarCropBoxSize);
    }

    private Rect getAvatarCropCancelRect() {
        Rect box = getAvatarCropBoxRect();
        return new Rect(box.x1 + 16, box.y2 - 22, box.x1 + 92, box.y2 - 6);
    }

    private Rect getAvatarCropApplyRect() {
        Rect box = getAvatarCropBoxRect();
        return new Rect(box.x2 - 92, box.y2 - 22, box.x2 - 16, box.y2 - 6);
    }

    private Rect getEditorShowcaseSlotRect(Rect editor) {
        int x = editor.x1 + 126;
        int y = editor.y1 + 154;
        return new Rect(x, y, x + 20, y + 20);
    }

    private Rect getShowcasePickerRect(Rect editor) {
        int w = Math.min(184, this.width - 20);
        int h = 104;
        int x = (this.width - w) / 2;
        int y = Math.max(8, editor.y1 + 48);
        if (y + h > this.height - 8) {
            y = this.height - h - 8;
        }
        return new Rect(x, y, x + w, y + h);
    }

    private Rect getProfileViewRect() {
        int w = Math.min(286, this.width - 24);
        int h = Math.min(188, this.height - 20);
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        return new Rect(x, y, x + w, y + h);
    }

    private Rect getMessageScrollbarTrackRect(Rect messagesRect) {
        return new Rect(messagesRect.x2 - 4, messagesRect.y1 + 4, messagesRect.x2 - 2, messagesRect.y2 - 4);
    }

    private Rect getMessageScrollbarThumbRect(Rect messagesRect) {
        if (selectedContactId == null) {
            return null;
        }
        ChatMetrics metrics = buildChatMetrics(messagesRect);
        if (metrics.contentHeight() <= metrics.viewportHeight()) {
            return null;
        }

        Rect track = getMessageScrollbarTrackRect(messagesRect);
        int thumbHeight = getThumbHeight(metrics, track.height());
        int movable = Math.max(1, track.height() - thumbHeight);
        int top = track.y1 + (messageScroll * movable / Math.max(1, metrics.maxScroll()));
        return new Rect(track.x1, top, track.x2, top + thumbHeight);
    }

    private int getThumbHeight(ChatMetrics metrics, int trackHeight) {
        return Math.max(16, metrics.viewportHeight() * trackHeight / Math.max(metrics.contentHeight(), 1));
    }

    private void drawAvatarSquare(PoseStack poseStack, String avatarId, int x, int y, int size, boolean online, boolean drawStatus) {
        fill(poseStack, x, y, x + size, y + size, 0xFF2A3442);
        fill(poseStack, x + 1, y + 1, x + size - 1, y + size - 1, 0xFF101722);

        if (avatarId != null && avatarId.startsWith("image:")) {
            fill(poseStack, x + 2, y + 2, x + size - 2, y + size - 2, 0xFF1D2630);
            ResourceLocation customTexture = getOrCreateAvatarTexture(avatarId);
            if (customTexture != null) {
                RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, customTexture);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                GuiComponent.blit(poseStack, x + 2, y + 2, 0, 0, size - 4, size - 4, size - 4, size - 4);
            }
        } else {
            AvatarStyle style = decodeAvatarStyle(avatarId);
            fill(poseStack, x + 2, y + 2, x + size - 2, y + size - 2, style.bgColor);

            switch (style.kind) {
                case "steve" -> drawSteveFace(poseStack, x + 2, y + 2, size - 4);
                case "alex" -> drawAlexFace(poseStack, x + 2, y + 2, size - 4);
                case "smile" -> drawSmileFace(poseStack, x + 2, y + 2, size - 4);
                case "cat" -> drawCatFace(poseStack, x + 2, y + 2, size - 4, style.fgColor);
                case "skull" -> drawSkullFace(poseStack, x + 2, y + 2, size - 4);
                case "favorite_flag" -> drawFlagAvatar(poseStack, x + 2, y + 2, size - 4, 0xFFFFFFFF);
                default -> drawSmileFace(poseStack, x + 2, y + 2, size - 4);
            }
        }

        if (drawStatus) {
            int statusColor = online ? 0xFF2ECC71 : 0xFF7B8794;
            fill(poseStack, x + size - 6, y + size - 6, x + size - 2, y + size - 2, statusColor);
        }
    }

    private ResourceLocation getOrCreateAvatarTexture(String avatarId) {
        if (avatarId == null || !avatarId.startsWith("image:")) {
            return null;
        }
        ResourceLocation existing = avatarTextureCache.get(avatarId);
        if (existing != null) {
            return existing;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(avatarId.substring(6));
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register("max_avatar_" + Integer.toHexString(avatarId.hashCode()), texture);
            avatarTextureCache.put(avatarId, location);
            return location;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ResourceLocation getOrCreateAttachmentPreviewTexture(String attachmentId, byte[] previewBytes) {
        if (attachmentId == null || attachmentId.isBlank() || previewBytes == null || previewBytes.length == 0) {
            return null;
        }
        ResourceLocation existing = attachmentPreviewTextureCache.get(attachmentId);
        if (existing != null) {
            return existing;
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(previewBytes));
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register("max_attachment_" + attachmentId.replace('-', '_'), texture);
            attachmentPreviewTextureCache.put(attachmentId, location);
            attachmentPreviewSizes.put(attachmentId, new int[]{image.getWidth(), image.getHeight()});
            return location;
        } catch (Exception ignored) {
            return null;
        }
    }

    private AvatarStyle decodeAvatarStyle(String avatarId) {
        String id = avatarId == null || avatarId.isBlank() ? "stock_steve" : avatarId;
        return switch (id) {
            case "steve", "stock_steve" -> new AvatarStyle("steve", 0xFF6D4C41, 0xFFFFFFFF);
            case "alex", "stock_alex" -> new AvatarStyle("alex", 0xFFBF6D32, 0xFFFFFFFF);
            case "max_red", "stock_smile" -> new AvatarStyle("smile", 0xFFF4C542, 0xFF1E1E1E);
            case "max_purple", "stock_cat" -> new AvatarStyle("cat", 0xFFE88BC8, 0xFFFFFFFF);
            case "max_black", "stock_skull" -> new AvatarStyle("skull", 0xFF8A8F98, 0xFFFFFFFF);
            case "favorite_flag" -> new AvatarStyle("favorite_flag", 0xFF2673F2, 0xFFFFFFFF);
            default -> new AvatarStyle("smile", 0xFF5A6B7D, 0xFFFFFFFF);
        };
    }

    private void drawSteveFace(PoseStack poseStack, int x, int y, int size) {
        int skin = 0xFFD7A97B;
        int hair = 0xFF4E342E;
        int eye = 0xFF22313F;
        fill(poseStack, x + size / 5, y + size / 4, x + size - size / 5, y + size - size / 6, skin);
        fill(poseStack, x, y, x + size, y + size / 3, hair);
        fill(poseStack, x, y + size / 3, x + size / 6, y + size * 3 / 4, hair);
        fill(poseStack, x + size * 5 / 6, y + size / 3, x + size, y + size * 3 / 4, hair);
        fill(poseStack, x + size / 3, y + size / 2, x + size / 3 + 2, y + size / 2 + 2, eye);
        fill(poseStack, x + size * 2 / 3 - 2, y + size / 2, x + size * 2 / 3, y + size / 2 + 2, eye);
    }

    private void drawAlexFace(PoseStack poseStack, int x, int y, int size) {
        int skin = 0xFFE5B58A;
        int hair = 0xFFCE7A31;
        int eye = 0xFF2B2B2B;
        fill(poseStack, x + size / 5, y + size / 4, x + size - size / 5, y + size - size / 6, skin);
        fill(poseStack, x, y, x + size, y + size / 3, hair);
        fill(poseStack, x, y + size / 3, x + size / 5, y + size, hair);
        fill(poseStack, x + size * 4 / 5, y + size / 3, x + size, y + size * 4 / 5, hair);
        fill(poseStack, x + size / 3, y + size / 2, x + size / 3 + 2, y + size / 2 + 2, eye);
        fill(poseStack, x + size * 2 / 3 - 2, y + size / 2, x + size * 2 / 3, y + size / 2 + 2, eye);
    }

    private void drawSmileFace(PoseStack poseStack, int x, int y, int size) {
        int eye = 0xFF202020;
        fill(poseStack, x + size / 4, y + size / 3, x + size / 4 + 2, y + size / 3 + 2, eye);
        fill(poseStack, x + size * 3 / 4 - 2, y + size / 3, x + size * 3 / 4, y + size / 3 + 2, eye);
        fill(poseStack, x + size / 4, y + size * 2 / 3, x + size * 3 / 4, y + size * 2 / 3 + 1, 0xFF5D4037);
    }

    private void drawCatFace(PoseStack poseStack, int x, int y, int size, int fg) {
        fill(poseStack, x + size / 4, y + size / 5, x + size / 2, y + size / 3, fg);
        fill(poseStack, x + size / 2, y + size / 5, x + size * 3 / 4, y + size / 3, fg);
        fill(poseStack, x + size / 5, y + size / 3, x + size * 4 / 5, y + size * 4 / 5, 0xFFFFD1E8);
        fill(poseStack, x + size / 3, y + size / 2, x + size / 3 + 2, y + size / 2 + 2, 0xFF2A2A2A);
        fill(poseStack, x + size * 2 / 3 - 2, y + size / 2, x + size * 2 / 3, y + size / 2 + 2, 0xFF2A2A2A);
    }

    private void drawSkullFace(PoseStack poseStack, int x, int y, int size) {
        fill(poseStack, x + size / 5, y + size / 5, x + size * 4 / 5, y + size * 4 / 5, 0xFFDADDE2);
        fill(poseStack, x + size / 3, y + size / 3, x + size / 3 + 3, y + size / 3 + 3, 0xFF2B2F35);
        fill(poseStack, x + size * 2 / 3 - 3, y + size / 3, x + size * 2 / 3, y + size / 3 + 3, 0xFF2B2F35);
    }

    private void drawFlagAvatar(PoseStack poseStack, int x, int y, int size, int fg) {
        int poleX = x + size / 3;
        int topY = y + size / 4;
        fill(poseStack, poleX, topY, poleX + 2, y + size * 3 / 4, fg);
        fill(poseStack, poleX + 2, topY, x + size * 3 / 4, topY + 2, fg);
        fill(poseStack, poleX + 2, y + size / 2, x + size * 3 / 4, y + size / 2 + 2, fg);
        fill(poseStack, x + size * 3 / 4 - 2, topY, x + size * 3 / 4, y + size / 2 + 2, fg);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] paragraphs = text.replace("\r", "").split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                String piece = this.font.plainSubstrByWidth(remaining, maxWidth);
                if (piece.isEmpty()) {
                    piece = remaining.substring(0, 1);
                }
                int cut = piece.length();
                if (cut < remaining.length()) {
                    int lastSpace = piece.lastIndexOf(' ');
                    if (lastSpace > 3) {
                        piece = piece.substring(0, lastSpace);
                        cut = lastSpace + 1;
                    }
                }
                lines.add(piece);
                remaining = remaining.substring(cut).stripLeading();
            }
        }
        return lines;
    }

    private String trim(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    private String formatTime(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMAT);
    }

    private String formatDay(LocalDate day) {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        if (day.equals(now)) {
            return "Сегодня";
        }
        if (day.equals(now.minusDays(1))) {
            return "Вчера";
        }
        return day.format(DAY_FORMAT);
    }

    private String shortenUuid(UUID uuid) {
        if (uuid == null) {
            return "-";
        }
        String value = uuid.toString();
        return value.substring(0, 8) + "..." + value.substring(value.length() - 8);
    }

    private boolean isCaretVisible() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ViewMode {
        CHATS,
        REGISTER,
        PROFILE_VIEW,
        PROFILE_EDIT
    }

    private record SidebarContact(UUID userId, String displayName, String avatarId, String preview, int unreadCount, boolean online, boolean favorite) {
    }

    private record AvatarStyle(String kind, int bgColor, int fgColor) {
    }

    private record ChatMetrics(List<ChatEntry> entries, int contentHeight, int viewportHeight, int maxScroll) {
    }

    private record ChatEntry(String dayLabel, MaxChatMessage message, boolean own, List<String> lines, int bubbleWidth, int bubbleHeight, int height, String metaText) {
        static ChatEntry day(String label, int height) {
            return new ChatEntry(label, null, false, List.of(), 0, 0, height, "");
        }

        static ChatEntry message(MaxChatMessage message, boolean own, List<String> lines, int bubbleWidth, int bubbleHeight, int height, String metaText) {
            return new ChatEntry(null, message, own, lines, bubbleWidth, bubbleHeight, height, metaText);
        }
    }

    private record AvatarClickBox(String avatarId, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record ShowcaseClickBox(String itemId, ItemStack stack, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record MessageHitBox(long messageId, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record MessageActionBox(MessageAction action, long messageId, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record FileButtonBox(String attachmentId, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record GiftButtonBox(UUID giftId, boolean accept, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record GiftDraftSlotBox(int slotIndex, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record GiftInventoryBox(int inventoryIndex, ItemStack stack, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record AttachActionBox(AttachAction action, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record PendingAttachment(String fileName, String mimeType, Path path, long sizeBytes, byte[] previewBytes) {
    }

    private enum MessageAction {
        DELETE("Удалить"),
        EDIT("Изменить"),
        REPLY("Ответить"),
        FORWARD("Переслать");

        private final String label;

        MessageAction(String label) {
            this.label = label;
        }
    }

    private enum AttachAction {
        FILE,
        GIFT
    }

    private record ContactRow(UUID userId, String displayName, String avatarId, String preview, int unreadCount, boolean online, boolean favorite, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private void performSearch(String query) {
        searchResults.clear();
        currentSearchResultIndex = -1;
        if (query.isBlank() || selectedContactId == null) {
            return;
        }
        List<MaxChatMessage> conv = MaxClientState.getConversation(selectedContactId);
        for (int i = 0; i < conv.size(); i++) {
            MaxChatMessage msg = conv.get(i);
            if (!msg.isDeleted() && msg.getType() == MaxChatMessage.MessageType.TEXT) {
                if (msg.getRawText().toLowerCase(Locale.ROOT).contains(query)) {
                    searchResults.add(msg.getMessageId());
                }
            }
        }
        if (!searchResults.isEmpty()) {
            currentSearchResultIndex = searchResults.size() - 1; // start from bottom
            scrollToSearchResult();
        }
    }

    private void scrollToSearchResult() {
        if (currentSearchResultIndex >= 0 && currentSearchResultIndex < searchResults.size()) {
            jumpToMessageId = searchResults.get(currentSearchResultIndex);
        }
    }
    
    private void cycleSearch(int direction) {
        if (searchResults.isEmpty()) {
            return;
        }
        currentSearchResultIndex += direction;
        if (currentSearchResultIndex < 0) {
            currentSearchResultIndex = searchResults.size() - 1;
        } else if (currentSearchResultIndex >= searchResults.size()) {
            currentSearchResultIndex = 0;
        }
        scrollToSearchResult();
    }

    private record Rect(int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }

        int width() {
            return x2 - x1;
        }

        int height() {
            return y2 - y1;
        }

        int centerX() {
            return x1 + width() / 2;
        }

        int centerY() {
            return y1 + height() / 2;
        }
    }
}
