package com.example.zitraksmode.client.gui;

import com.example.zitraksmode.client.printer.ClientPrinterRecipeLibrary;
import com.example.zitraksmode.client.printer.PrinterRecipeFileStorage;
import com.example.zitraksmode.network.AutofillPrinterPacket;
import com.example.zitraksmode.network.DeletePrinterRecipePacket;
import com.example.zitraksmode.network.ImportPrinterRecipePacket;
import com.example.zitraksmode.network.ModPackets;
import com.example.zitraksmode.network.RenamePrinterRecipePacket;
import com.example.zitraksmode.network.ScanPrinterRecipePacket;
import com.example.zitraksmode.network.SelectRecipePacket;
import com.example.zitraksmode.network.SharePrinterRecipePacket;
import com.example.zitraksmode.network.StartPrintingPacket;
import com.example.zitraksmode.network.SyncPrinterScrollPacket;
import com.example.zitraksmode.printer.Printer3DMenu;
import com.example.zitraksmode.printer.PrinterRecipe;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Printer3DScreen extends AbstractContainerScreen<Printer3DMenu> {

    private static final int CUSTOM_VISIBLE_ROWS = 3;
    private static final int MATERIAL_VISIBLE_ROWS = 8;
    private static final int SHARE_VISIBLE_ROWS = 6;
    private static final int IMPORT_VISIBLE_ROWS = 5;

    private Button hammerButton;
    private Button dimaHouseButton;
    private Button prisonButton;
    private Button autofillButton;
    private PrintButton printButton;
    private Button modeToggleButton;

    private Button exportRecipeButton;
    private Button deleteRecipeButton;
    private Button renameRecipeButton;
    private Button shareRecipeButton;

    private Button scanButton;
    private Button renameConfirmButton;
    private Button renameCancelButton;
    private Button shareCancelButton;
    private Button importCancelButton;

    private EditBox scanNameBox;
    private EditBox scanXBox;
    private EditBox scanYBox;
    private EditBox scanZBox;
    private EditBox renameEditBox;

    private boolean scannerMode = false;
    private boolean renameDialogOpen = false;
    private boolean shareDialogOpen = false;
    private boolean importDialogOpen = false;

    private int customRecipeScroll = 0;
    private int materialScroll = 0;
    private int sharePlayerScroll = 0;
    private int importFileScroll = 0;

    private final List<HoveredMaterial> hoveredMaterials = new ArrayList<>();
    private final List<CustomRecipeRowHitbox> customRecipeHitboxes = new ArrayList<>();
    private final List<ImportRowHitbox> importRowHitboxes = new ArrayList<>();
    private final List<SharePlayerRowHitbox> sharePlayerHitboxes = new ArrayList<>();
    private final List<ImportFileRowHitbox> importFileHitboxes = new ArrayList<>();
    private final List<PrinterRecipeFileStorage.ImportEntry> importableFiles = new ArrayList<>();

    public Printer3DScreen(Printer3DMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 420;
        this.imageHeight = 256;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        int left = guiLeft();
        int top = guiTop();

        hammerButton = new FancyButton(left + 8, top + 24, 82, 16, Component.literal("Пенис"), btn -> selectRecipe(PrinterRecipe.HAMMER));
        dimaHouseButton = new FancyButton(left + 8, top + 44, 82, 16, Component.literal("Дом Димы"), btn -> selectRecipe(PrinterRecipe.DIMA_HOUSE));
        prisonButton = new FancyButton(left + 8, top + 64, 82, 16, Component.literal("Тюряга"), btn -> selectRecipe(PrinterRecipe.PRISON));

        autofillButton = new AutoButton(left + 100, top + 126, 80, 16, Component.literal("Авто"), btn -> autofill());
        printButton = new PrintButton(left + 314, top + 230, 90, 18, Component.literal("▶ Печать"), btn -> startPrinting());
        modeToggleButton = new FancyButton(left + 366, top + 6, 44, 16, Component.literal("Скан"), btn -> toggleMode());

        exportRecipeButton = new IconButton(left + 226, top + 8, 16, 16, Component.literal("⇩"), btn -> exportSelectedRecipe());
        deleteRecipeButton = new IconButton(left + 246, top + 8, 16, 16, Component.literal("✖"), btn -> deleteSelectedCustomRecipe());
        renameRecipeButton = new IconButton(left + 266, top + 8, 16, 16, Component.literal("✎"), btn -> openRenameDialog());
        shareRecipeButton = new IconButton(left + 286, top + 8, 16, 16, Component.literal("↗"), btn -> openShareDialog());

        scanNameBox = new EditBox(this.font, left + 118, top + 68, 198, 14, Component.literal("Название"));
        scanNameBox.setMaxLength(48);
        scanNameBox.visible = false;

        scanXBox = numberBox(left + 120, top + 120, 44, 14);
        scanYBox = numberBox(left + 181, top + 120, 44, 14);
        scanZBox = numberBox(left + 242, top + 120, 44, 14);

        scanButton = new FancyButton(left + 120, top + 164, 166, 16, Component.literal("Сканировать"), btn -> scanRecipe());
        scanButton.visible = false;

        renameEditBox = new EditBox(this.font, left + 150, top + 102, 130, 16, Component.literal("Новое имя"));
        renameEditBox.setMaxLength(48);
        renameEditBox.visible = false;

        renameConfirmButton = new FancyButton(left + 150, top + 128, 56, 18, Component.literal("OK"), btn -> confirmRename());
        renameConfirmButton.visible = false;

        renameCancelButton = new FancyButton(left + 224, top + 128, 70, 18, Component.literal("Отмена"), btn -> closeRenameDialog());
        renameCancelButton.visible = false;

        shareCancelButton = new FancyButton(left + 168, top + 172, 84, 18, Component.literal("Закрыть"), btn -> closeShareDialog());
        shareCancelButton.visible = false;

        importCancelButton = new FancyButton(left + 160, top + 170, 96, 18, Component.literal("Закрыть"), btn -> closeImportDialog());
        importCancelButton.visible = false;

        addRenderableWidget(hammerButton);
        addRenderableWidget(dimaHouseButton);
        addRenderableWidget(prisonButton);
        addRenderableWidget(autofillButton);
        addRenderableWidget(printButton);
        addRenderableWidget(modeToggleButton);

        addRenderableWidget(exportRecipeButton);
        addRenderableWidget(deleteRecipeButton);
        addRenderableWidget(renameRecipeButton);
        addRenderableWidget(shareRecipeButton);

        addRenderableWidget(scanNameBox);
        addRenderableWidget(scanXBox);
        addRenderableWidget(scanYBox);
        addRenderableWidget(scanZBox);
        addRenderableWidget(scanButton);

        addRenderableWidget(renameEditBox);
        addRenderableWidget(renameConfirmButton);
        addRenderableWidget(renameCancelButton);

        addRenderableWidget(shareCancelButton);
        addRenderableWidget(importCancelButton);

        applyModeVisibility();
    }

    private EditBox numberBox(int x, int y, int w, int h) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.empty());
        box.setMaxLength(4);
        box.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        box.visible = false;
        return box;
    }

    private int guiLeft() {
        return (width - imageWidth) / 2;
    }

    private int guiTop() {
        return (height - imageHeight) / 2;
    }

    private void applyModeVisibility() {
        boolean modalOpen = renameDialogOpen || shareDialogOpen || importDialogOpen;
        boolean printMode = !scannerMode && !modalOpen;

        hammerButton.visible = printMode;
        dimaHouseButton.visible = printMode;
        prisonButton.visible = printMode;
        autofillButton.visible = printMode;
        printButton.visible = printMode;
        printButton.active = printMode;

        modeToggleButton.visible = !modalOpen;

        scanNameBox.visible = scannerMode;
        scanXBox.visible = scannerMode;
        scanYBox.visible = scannerMode;
        scanZBox.visible = scannerMode;
        scanButton.visible = scannerMode;
        scanButton.active = scannerMode;

        boolean customSelected = printMode && menu.getBlockEntity().hasSelectedCustomRecipe();
        boolean hasPreview = menu.getBlockEntity().getClientPreviewCustomRecipe() != null;

        exportRecipeButton.visible = customSelected && hasPreview;
        deleteRecipeButton.visible = customSelected;
        renameRecipeButton.visible = customSelected;
        shareRecipeButton.visible = customSelected;

        renameEditBox.visible = renameDialogOpen;
        renameConfirmButton.visible = renameDialogOpen;
        renameCancelButton.visible = renameDialogOpen;

        shareCancelButton.visible = shareDialogOpen;
        importCancelButton.visible = importDialogOpen;

        modeToggleButton.setMessage(Component.literal(scannerMode ? "Назад" : "Скан"));
    }

    private void toggleMode() {
        scannerMode = !scannerMode;
        renameDialogOpen = false;
        shareDialogOpen = false;
        importDialogOpen = false;
        setFocused(null);
        applyModeVisibility();
    }

    private void selectRecipe(PrinterRecipe recipe) {
        renameDialogOpen = false;
        shareDialogOpen = false;
        importDialogOpen = false;
        materialScroll = 0;
        ModPackets.sendToServer(new SelectRecipePacket(menu.getBlockEntity().getBlockPos(), recipe));
        menu.getBlockEntity().setSelectedRecipe(recipe);
        applyModeVisibility();
    }

    private void selectCustomRecipe(String recipeId) {
        renameDialogOpen = false;
        shareDialogOpen = false;
        importDialogOpen = false;
        materialScroll = 0;
        ModPackets.sendToServer(new SelectRecipePacket(menu.getBlockEntity().getBlockPos(), recipeId));
        applyModeVisibility();
    }

    private void startPrinting() {
        ModPackets.sendToServer(new StartPrintingPacket(menu.getBlockEntity().getBlockPos()));
    }

    private void autofill() {
        ModPackets.sendToServer(new AutofillPrinterPacket(menu.getBlockEntity().getBlockPos()));
    }

    private void scanRecipe() {
        int sizeX = parse(scanXBox.getValue());
        int sizeY = parse(scanYBox.getValue());
        int sizeZ = parse(scanZBox.getValue());

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) return;

        ModPackets.sendToServer(new ScanPrinterRecipePacket(
                menu.getBlockEntity().getBlockPos(),
                scanNameBox.getValue(),
                sizeX,
                sizeY,
                sizeZ
        ));
    }

    private void deleteSelectedCustomRecipe() {
        String id = menu.getBlockEntity().getSelectedCustomRecipeId();
        if (id == null || id.isBlank()) return;
        ModPackets.sendToServer(new DeletePrinterRecipePacket(menu.getBlockEntity().getBlockPos(), id));
    }

    private void exportSelectedRecipe() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        PlayerStoredPrinterRecipe recipe = menu.getBlockEntity().getClientPreviewCustomRecipe();
        if (recipe == null || !recipe.isValid()) {
            minecraft.player.displayClientMessage(Component.literal("§cНет выбранного кастомного рецепта для экспорта"), false);
            return;
        }

        try {
            PrinterRecipeFileStorage.ExportResult result = PrinterRecipeFileStorage.exportRecipe(recipe);
            minecraft.player.displayClientMessage(Component.literal("§aРецепт экспортирован: §e" + result.fileName()), false);
        } catch (Exception e) {
            minecraft.player.displayClientMessage(Component.literal("§cОшибка экспорта: " + e.getMessage()), false);
        }
    }

    private void openRenameDialog() {
        String id = menu.getBlockEntity().getSelectedCustomRecipeId();
        if (id == null || id.isBlank()) return;

        String currentName = ClientPrinterRecipeLibrary.getById(id)
                .map(ClientPrinterRecipeLibrary.RecipeSummary::displayName)
                .orElse("");

        renameDialogOpen = true;
        shareDialogOpen = false;
        importDialogOpen = false;
        renameEditBox.setValue(currentName);
        setFocused(renameEditBox);
        applyModeVisibility();
    }

    private void closeRenameDialog() {
        renameDialogOpen = false;
        setFocused(null);
        applyModeVisibility();
    }

    private void confirmRename() {
        String id = menu.getBlockEntity().getSelectedCustomRecipeId();
        if (id == null || id.isBlank()) return;

        ModPackets.sendToServer(new RenamePrinterRecipePacket(
                menu.getBlockEntity().getBlockPos(),
                id,
                renameEditBox.getValue()
        ));

        renameDialogOpen = false;
        setFocused(null);
        applyModeVisibility();
    }

    private void openShareDialog() {
        shareDialogOpen = true;
        renameDialogOpen = false;
        importDialogOpen = false;
        sharePlayerScroll = 0;
        applyModeVisibility();
    }

    private void closeShareDialog() {
        shareDialogOpen = false;
        applyModeVisibility();
    }

    private void openImportDialog() {
        if (ClientPrinterRecipeLibrary.getRecipes().size() >= 10) {
            return;
        }

        importDialogOpen = true;
        renameDialogOpen = false;
        shareDialogOpen = false;
        importFileScroll = 0;
        refreshImportableFiles();
        applyModeVisibility();
    }

    private void closeImportDialog() {
        importDialogOpen = false;
        applyModeVisibility();
    }

    private void refreshImportableFiles() {
        importableFiles.clear();

        try {
            importableFiles.addAll(PrinterRecipeFileStorage.listImportableFiles());
        } catch (Exception e) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§cОшибка чтения экспортов: " + e.getMessage()), false);
            }
        }
    }

    private void importRecipeFromFile(String fileName) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        try {
            PlayerStoredPrinterRecipe recipe = PrinterRecipeFileStorage.readRecipeFromFile(fileName);
            ModPackets.sendToServer(new ImportPrinterRecipePacket(menu.getBlockEntity().getBlockPos(), recipe));
            closeImportDialog();
        } catch (Exception e) {
            minecraft.player.displayClientMessage(Component.literal("§cОшибка импорта: " + e.getMessage()), false);
        }
    }

    private void shareSelectedRecipeWith(UUID targetPlayerId) {
        String recipeId = menu.getBlockEntity().getSelectedCustomRecipeId();
        if (recipeId == null || idIsBlank(recipeId)) return;
        ModPackets.sendToServer(new SharePrinterRecipePacket(recipeId, targetPlayerId));
        closeShareDialog();
    }

    private boolean idIsBlank(String value) {
        return value == null || value.isBlank();
    }

    private int parse(String value) {
        if (value == null || value.isBlank()) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    protected void renderBg(@NotNull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = guiLeft();
        int y = guiTop();

        fill(poseStack, x, y, x + imageWidth, y + imageHeight, 0xFF141414);
        fill(poseStack, x, y, x + imageWidth, y + 2, 0xFF000000);
        fill(poseStack, x, y + imageHeight - 2, x + imageWidth, y + imageHeight, 0xFF000000);
        fill(poseStack, x, y, x + 2, y + imageHeight, 0xFF000000);
        fill(poseStack, x + imageWidth - 2, y, x + imageWidth, y + imageHeight, 0xFF000000);

        fill(poseStack, x + 6, y + 22, x + 94, y + 150, 0xFF232323);
        fill(poseStack, x + 98, y + 22, x + 266, y + 126, 0xFF1F1F1F);
        fill(poseStack, x + 286, y + 22, x + 414, y + 152, 0xFF232323);

        for (int row = 0; row < Printer3DMenu.VISIBLE_PRINTER_ROWS; row++) {
            for (int col = 0; col < Printer3DMenu.PRINTER_COLUMNS; col++) {
                int sx = x + 104 + col * 18;
                int sy = y + 30 + row * 18;
                fill(poseStack, sx, sy, sx + 16, sy + 16, 0xFF353535);
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float delta) {
        hoveredMaterials.clear();
        customRecipeHitboxes.clear();
        importRowHitboxes.clear();
        sharePlayerHitboxes.clear();
        importFileHitboxes.clear();

        renderBackground(poseStack);

        if (!scannerMode) {
            renderBg(poseStack, delta, mouseX, mouseY);
        }

        int left = guiLeft();
        int top = guiTop();

        if (scannerMode) {
            renderScannerOverlay(poseStack, left, top);
            renderOverlayWidgets(poseStack, mouseX, mouseY, delta);
            renderTooltip(poseStack, mouseX, mouseY);
            return;
        }

        if (renameDialogOpen) {
            renderBg(poseStack, delta, mouseX, mouseY);
            renderTitleAndPanels(poseStack, left, top, mouseX, mouseY);
            renderRenameOverlay(poseStack, left, top);
            renderOverlayWidgets(poseStack, mouseX, mouseY, delta);
            renderTooltip(poseStack, mouseX, mouseY);
            return;
        }

        if (shareDialogOpen) {
            renderBg(poseStack, delta, mouseX, mouseY);
            renderTitleAndPanels(poseStack, left, top, mouseX, mouseY);
            renderShareOverlay(poseStack, left, top, mouseX, mouseY);
            renderOverlayWidgets(poseStack, mouseX, mouseY, delta);
            renderTooltip(poseStack, mouseX, mouseY);
            return;
        }

        if (importDialogOpen) {
            renderBg(poseStack, delta, mouseX, mouseY);
            renderTitleAndPanels(poseStack, left, top, mouseX, mouseY);
            renderImportOverlay(poseStack, left, top, mouseX, mouseY);
            renderOverlayWidgets(poseStack, mouseX, mouseY, delta);
            renderTooltip(poseStack, mouseX, mouseY);

            for (ImportFileRowHitbox hitbox : importFileHitboxes) {
                if (hitbox.contains(mouseX, mouseY)) {
                    renderTooltip(poseStack, Component.literal(hitbox.tooltipText), mouseX, mouseY);
                    break;
                }
            }
            return;
        }

        super.render(poseStack, mouseX, mouseY, delta);
        renderTitleAndPanels(poseStack, left, top, mouseX, mouseY);
        renderTooltip(poseStack, mouseX, mouseY);

        for (HoveredMaterial hovered : hoveredMaterials) {
            if (mouseX >= hovered.x && mouseX <= hovered.x + 16 && mouseY >= hovered.y && mouseY <= hovered.y + 16) {
                renderTooltip(poseStack, hovered.stack, mouseX, mouseY);
                break;
            }
        }

        if (exportRecipeButton.visible && exportRecipeButton.isMouseOver(mouseX, mouseY)) {
            renderTooltip(poseStack, Component.literal("Экспортировать рецепт в файл"), mouseX, mouseY);
        }
        if (deleteRecipeButton.visible && deleteRecipeButton.isMouseOver(mouseX, mouseY)) {
            renderTooltip(poseStack, Component.literal("Удалить рецепт"), mouseX, mouseY);
        }
        if (renameRecipeButton.visible && renameRecipeButton.isMouseOver(mouseX, mouseY)) {
            renderTooltip(poseStack, Component.literal("Переименовать рецепт"), mouseX, mouseY);
        }
        if (shareRecipeButton.visible && shareRecipeButton.isMouseOver(mouseX, mouseY)) {
            renderTooltip(poseStack, Component.literal("Поделиться рецептом"), mouseX, mouseY);
        }

        for (ImportRowHitbox hitbox : importRowHitboxes) {
            if (hitbox.contains(mouseX, mouseY)) {
                renderTooltip(poseStack, Component.literal("Импортировать рецепт из файла"), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderOverlayWidgets(PoseStack poseStack, int mouseX, int mouseY, float delta) {
    if (!shareDialogOpen && !renameDialogOpen && !importDialogOpen) {
        modeToggleButton.render(poseStack, mouseX, mouseY, delta);
    }

    if (scannerMode) {
        scanNameBox.render(poseStack, mouseX, mouseY, delta);
        scanXBox.render(poseStack, mouseX, mouseY, delta);
        scanYBox.render(poseStack, mouseX, mouseY, delta);
        scanZBox.render(poseStack, mouseX, mouseY, delta);
        scanButton.render(poseStack, mouseX, mouseY, delta);
    }

    if (renameDialogOpen) {
        renameEditBox.render(poseStack, mouseX, mouseY, delta);
        renameConfirmButton.render(poseStack, mouseX, mouseY, delta);
        renameCancelButton.render(poseStack, mouseX, mouseY, delta);
    }

    if (shareDialogOpen) {
        shareCancelButton.render(poseStack, mouseX, mouseY, delta);
    }

    if (importDialogOpen) {
        importCancelButton.render(poseStack, mouseX, mouseY, delta);
    }
}

    private void renderTitleAndPanels(PoseStack poseStack, int left, int top, int mouseX, int mouseY) {
        var be = menu.getBlockEntity();
        PrinterRecipe selectedBuiltin = be.getSelectedRecipe();
        String selectedCustomId = be.getSelectedCustomRecipeId();

        String title = "3D Принтер";
        if (selectedBuiltin != null) title = selectedBuiltin.getDisplayName();
        else if (selectedCustomId != null) title = ClientPrinterRecipeLibrary.getById(selectedCustomId).map(ClientPrinterRecipeLibrary.RecipeSummary::displayName).orElse("Кастомный рецепт");

        font.draw(poseStack, title, left + 100, top + 8, 0xFFFFFF);
        font.draw(poseStack, "Рецепты", left + 16, top + 8, 0xD0D0D0);
        font.draw(poseStack, "Материалы", left + 308, top + 8, 0xD0D0D0);

        renderCustomRecipeList(poseStack, left, top, mouseX, mouseY);

        if (selectedBuiltin != null) {
            renderMaterialPanelBuiltin(poseStack, selectedBuiltin, left + 292, top + 28);
        } else if (be.getClientPreviewCustomRecipe() != null) {
            renderMaterialPanelCustom(poseStack, be.getClientPreviewCustomRecipe(), left + 292, top + 28);
        }

        if (be.hasActiveBargain()) {
            Component bargainName = be.getBargainDisplayName();
            String line = "Скидка " + be.getBargainPercent() + "%: " + (bargainName == null ? "?" : bargainName.getString());
            font.draw(poseStack, shorten(line, 17), left + 292, top + 140, 0x55FF55);
        }

        if (be.isPrinting()) {
            drawCenteredString(poseStack, font, "Печать: " + be.getPrintProgress() + "%", left + imageWidth / 2, top + 236, 0x00FF66);
        }

        renderInventoryScrollIndicator(poseStack, left + 268, top + 30);
    }

    private void renderScannerOverlay(PoseStack poseStack, int left, int top) {
        int x = left + 104;
        int y = top + 28;
        int w = 226;
        int h = 164;

        fill(poseStack, x, y, x + w, y + h, 0xEE111111);
        fill(poseStack, x, y, x + w, y + 1, 0xFF5A5A5A);
        fill(poseStack, x, y + h - 1, x + w, y + h, 0xFF5A5A5A);
        fill(poseStack, x, y, x + 1, y + h, 0xFF5A5A5A);
        fill(poseStack, x + w - 1, y, x + w, y + h, 0xFF5A5A5A);

        font.draw(poseStack, "Сканер области", x + 12, y + 10, 0xFFFFFF);
        font.draw(poseStack, "Имя:", x + 12, y + 28, 0xCFCFCF);
        font.draw(poseStack, "Размеры от принтера", x + 12, y + 62, 0xAAAAAA);

        drawCenteredString(poseStack, font, "X", x + 38, y + 80, 0xFFFFFF);
        drawCenteredString(poseStack, font, "Y", x + 99, y + 80, 0xFFFFFF);
        drawCenteredString(poseStack, font, "Z", x + 160, y + 80, 0xFFFFFF);

        font.draw(poseStack, "1,1,1 = первый блок рабочей зоны", x + 12, y + 116, 0x8FD98F);
        font.draw(poseStack, "слева-спереди от принтера", x + 12, y + 128, 0x8FD98F);
    }

    private void renderRenameOverlay(PoseStack poseStack, int left, int top) {
        int x = left + 132;
        int y = top + 86;
        int w = 176;
        int h = 66;

        fill(poseStack, x, y, x + w, y + h, 0xEE101010);
        fill(poseStack, x, y, x + w, y + 1, 0xFF5A5A5A);
        fill(poseStack, x, y + h - 1, x + w, y + h, 0xFF5A5A5A);
        fill(poseStack, x, y, x + 1, y + h, 0xFF5A5A5A);
        fill(poseStack, x + w - 1, y, x + w, y + h, 0xFF5A5A5A);

        font.draw(poseStack, "Переименовать", x + 12, y + 10, 0xFFFFFF);
    }

    private void renderShareOverlay(PoseStack poseStack, int left, int top, int mouseX, int mouseY) {
        int x = left + 112;
        int y = top + 36;
        int w = 196;
        int h = 154;

        fill(poseStack, x, y, x + w, y + h, 0xEE101010);
        fill(poseStack, x, y, x + w, y + 1, 0xFF5A5A5A);
        fill(poseStack, x, y + h - 1, x + w, y + h, 0xFF5A5A5A);
        fill(poseStack, x, y, x + 1, y + h, 0xFF5A5A5A);
        fill(poseStack, x + w - 1, y, x + w, y + h, 0xFF5A5A5A);

        font.draw(poseStack, "Поделиться рецептом", x + 12, y + 10, 0xFFFFFF);
        font.draw(poseStack, "Выбери игрока:", x + 12, y + 24, 0xD0D0D0);

        List<SharePlayerEntry> players = buildSharePlayerEntries();
        int maxScroll = Math.max(0, players.size() - SHARE_VISIBLE_ROWS);
        if (sharePlayerScroll > maxScroll) sharePlayerScroll = maxScroll;

        int listY = y + 40;
        for (int i = 0; i < Math.min(SHARE_VISIBLE_ROWS, players.size()); i++) {
            int index = sharePlayerScroll + i;
            SharePlayerEntry entry = players.get(index);

            int rowX1 = x + 10;
            int rowY1 = listY + i * 18;
            int rowX2 = x + w - 10;
            int rowY2 = rowY1 + 16;
            boolean hovered = mouseX >= rowX1 && mouseX <= rowX2 && mouseY >= rowY1 && mouseY <= rowY2;

            fill(poseStack, rowX1, rowY1, rowX2, rowY2, hovered ? 0xFF3C3C68 : 0xFF2A2A2A);
            renderPlayerHead(poseStack, entry, rowX1 + 2, rowY1);
            font.draw(poseStack, entry.name, rowX1 + 22, rowY1 + 4, 0xFFFFFF);

            sharePlayerHitboxes.add(new SharePlayerRowHitbox(entry.uuid, rowX1, rowY1, rowX2, rowY2));
        }

        if (players.size() > SHARE_VISIBLE_ROWS) {
            int barX = x + w - 8;
            int barTop = listY;
            int barHeight = SHARE_VISIBLE_ROWS * 18 - 2;
            fill(poseStack, barX, barTop, barX + 2, barTop + barHeight, 0xFF1E1E1E);
            int thumbHeight = Math.max(8, barHeight * SHARE_VISIBLE_ROWS / players.size());
            int travel = Math.max(1, barHeight - thumbHeight);
            int thumbY = barTop + (maxScroll == 0 ? 0 : (travel * sharePlayerScroll / maxScroll));
            fill(poseStack, barX, thumbY, barX + 2, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderImportOverlay(PoseStack poseStack, int left, int top, int mouseX, int mouseY) {
        int x = left + 102;
        int y = top + 34;
        int w = 208;
        int h = 154;

        fill(poseStack, x, y, x + w, y + h, 0xEE101010);
        fill(poseStack, x, y, x + w, y + 1, 0xFF5A5A5A);
        fill(poseStack, x, y + h - 1, x + w, y + h, 0xFF5A5A5A);
        fill(poseStack, x, y, x + 1, y + h, 0xFF5A5A5A);
        fill(poseStack, x + w - 1, y, x + w, y + h, 0xFF5A5A5A);

        font.draw(poseStack, "Импорт рецепта", x + 12, y + 10, 0xFFFFFF);
        font.draw(poseStack, "Папка: ." + PrinterRecipeFileStorage.getRelativeFolderDisplay(), x + 12, y + 24, 0xAAAAAA);
        font.draw(poseStack, "Выбери файл:", x + 12, y + 36, 0xD0D0D0);

        int maxScroll = Math.max(0, importableFiles.size() - IMPORT_VISIBLE_ROWS);
        if (importFileScroll > maxScroll) importFileScroll = maxScroll;

        int listY = y + 52;
        if (importableFiles.isEmpty()) {
            font.draw(poseStack, "Нет файлов экспорта", x + 12, listY + 6, 0x9A9A9A);
            return;
        }

        for (int i = 0; i < Math.min(IMPORT_VISIBLE_ROWS, importableFiles.size()); i++) {
            int index = importFileScroll + i;
            PrinterRecipeFileStorage.ImportEntry entry = importableFiles.get(index);

            int rowX1 = x + 10;
            int rowY1 = listY + i * 18;
            int rowX2 = x + w - 10;
            int rowY2 = rowY1 + 16;
            boolean hovered = mouseX >= rowX1 && mouseX <= rowX2 && mouseY >= rowY1 && mouseY <= rowY2;

            fill(poseStack, rowX1, rowY1, rowX2, rowY2, hovered ? 0xFF4C4C4C : 0xFF2E2E2E);

            String line = shorten(entry.displayName() + " [" + entry.sizeX() + "x" + entry.sizeY() + "x" + entry.sizeZ() + "]", 23);
            font.draw(poseStack, line, rowX1 + 4, rowY1 + 4, 0xFFFFFF);

            importFileHitboxes.add(new ImportFileRowHitbox(
                    entry.fileName(),
                    entry.fileName() + " | " + entry.displayName(),
                    rowX1, rowY1, rowX2, rowY2
            ));
        }

        if (importableFiles.size() > IMPORT_VISIBLE_ROWS) {
            int barX = x + w - 8;
            int barTop = listY;
            int barHeight = IMPORT_VISIBLE_ROWS * 18 - 2;
            fill(poseStack, barX, barTop, barX + 2, barTop + barHeight, 0xFF1E1E1E);

            int thumbHeight = Math.max(8, barHeight * IMPORT_VISIBLE_ROWS / importableFiles.size());
            int travel = Math.max(1, barHeight - thumbHeight);
            int thumbY = barTop + (maxScroll == 0 ? 0 : (travel * importFileScroll / maxScroll));
            fill(poseStack, barX, thumbY, barX + 2, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private List<SharePlayerEntry> buildSharePlayerEntries() {
        List<SharePlayerEntry> result = new ArrayList<>();
        if (minecraft == null || minecraft.getConnection() == null || minecraft.player == null) {
            return result;
        }

        UUID selfId = minecraft.player.getUUID();

        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            result.add(new SharePlayerEntry(info.getProfile().getId(), info.getProfile().getName(), info));
        }

        result.sort((a, b) -> {
            if (a.uuid.equals(selfId) && !b.uuid.equals(selfId)) return -1;
            if (!a.uuid.equals(selfId) && b.uuid.equals(selfId)) return 1;
            return a.name.compareToIgnoreCase(b.name);
        });

        return result;
    }

    private void renderPlayerHead(PoseStack poseStack, SharePlayerEntry entry, int x, int y) {
        if (entry.info == null) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, entry.info.getSkinLocation());
        blit(poseStack, x, y, 16, 16, 8.0F, 8.0F, 8, 8, 64, 64);
        blit(poseStack, x, y, 16, 16, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    private void renderCustomRecipeList(PoseStack poseStack, int left, int top, int mouseX, int mouseY) {
        List<ClientPrinterRecipeLibrary.RecipeSummary> recipes = ClientPrinterRecipeLibrary.getRecipes();
        boolean showImportRow = recipes.size() < 10;

        int totalEntries = recipes.size() + (showImportRow ? 1 : 0);
        int visibleCount = Math.min(CUSTOM_VISIBLE_ROWS, totalEntries);
        int maxScroll = Math.max(0, totalEntries - CUSTOM_VISIBLE_ROWS);
        if (customRecipeScroll > maxScroll) customRecipeScroll = maxScroll;

        font.draw(poseStack, "Мои", left + 10, top + 86, 0xDADADA);

        for (int i = 0; i < visibleCount; i++) {
            int index = customRecipeScroll + i;

            int rowX1 = left + 8;
            int rowY1 = top + 98 + i * 18;
            int rowX2 = left + 88;
            int rowY2 = rowY1 + 16;

            boolean hovered = mouseX >= rowX1 && mouseX <= rowX2 && mouseY >= rowY1 && mouseY <= rowY2;

            if (index < recipes.size()) {
                ClientPrinterRecipeLibrary.RecipeSummary recipe = recipes.get(index);
                boolean selected = recipe.recipeId().equals(menu.getBlockEntity().getSelectedCustomRecipeId());

                int bg = selected ? 0xFF4E6FD1 : (hovered ? 0xFF474781 : 0xFF313131);
                fill(poseStack, rowX1, rowY1, rowX2, rowY2, bg);

                String name = shorten(recipe.displayName(), 11);
                font.draw(poseStack, name, rowX1 + 3, rowY1 + 4, 0xFFFFFF);
                customRecipeHitboxes.add(new CustomRecipeRowHitbox(recipe.recipeId(), rowX1, rowY1, rowX2, rowY2));
            } else {
                int bg = hovered ? 0xFF575757 : 0xFF3B3B3B;
                fill(poseStack, rowX1, rowY1, rowX2, rowY2, bg);
                drawCenteredString(poseStack, font, "+", (rowX1 + rowX2) / 2, rowY1 + 4, 0xFFFFFF);
                importRowHitboxes.add(new ImportRowHitbox(rowX1, rowY1, rowX2, rowY2));
            }
        }

        if (totalEntries > CUSTOM_VISIBLE_ROWS) {
            int barX = left + 90;
            int barTop = top + 98;
            int barHeight = CUSTOM_VISIBLE_ROWS * 18 - 2;
            fill(poseStack, barX, barTop, barX + 2, barTop + barHeight, 0xFF1E1E1E);

            int thumbHeight = Math.max(8, barHeight * CUSTOM_VISIBLE_ROWS / totalEntries);
            int travel = Math.max(1, barHeight - thumbHeight);
            int thumbY = barTop + (maxScroll == 0 ? 0 : (travel * customRecipeScroll / maxScroll));
            fill(poseStack, barX, thumbY, barX + 2, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderMaterialPanelBuiltin(PoseStack poseStack, PrinterRecipe recipe, int panelX, int panelY) {
        var handler = menu.getBlockEntity().getItemHandler();
        Map<Item, Integer> available = new HashMap<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(recipe.getMaterials().entrySet());
        entries.sort(Comparator.<Map.Entry<Item, Integer>>comparingInt(Map.Entry::getValue).reversed());
        drawMaterialEntries(poseStack, entries, available, panelX, panelY);
    }

    private void renderMaterialPanelCustom(PoseStack poseStack, PlayerStoredPrinterRecipe recipe, int panelX, int panelY) {
        var handler = menu.getBlockEntity().getItemHandler();
        Map<Item, Integer> available = new HashMap<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        List<Map.Entry<Item, Integer>> resolved = new ArrayList<>();
        for (var entry : recipe.getMaterials().entrySet()) {
            Item item = Registry.ITEM.get(new ResourceLocation(entry.getKey()));
            if (item != null && item != Items.AIR) {
                resolved.add(Map.entry(item, entry.getValue()));
            }
        }
        resolved.sort(Comparator.<Map.Entry<Item, Integer>>comparingInt(Map.Entry::getValue).reversed());
        drawMaterialEntries(poseStack, resolved, available, panelX, panelY);
    }

    private void drawMaterialEntries(PoseStack poseStack, List<Map.Entry<Item, Integer>> entries, Map<Item, Integer> available, int panelX, int panelY) {
        int maxScroll = Math.max(0, entries.size() - MATERIAL_VISIBLE_ROWS);
        if (materialScroll > maxScroll) materialScroll = maxScroll;

        int yOff = 0;
        for (int i = materialScroll; i < entries.size() && i < materialScroll + MATERIAL_VISIBLE_ROWS; i++) {
            var entry = entries.get(i);
            Item item = entry.getKey();
            int need = entry.getValue();
            int discountedNeed = menu.getBlockEntity().getDiscountedRequiredCount(item, need);
            int have = available.getOrDefault(item, 0);

            ItemStack displayStack = new ItemStack(item);
            int iconX = panelX + 4;
            int iconY = panelY + 4 + yOff;

            minecraft.getItemRenderer().renderGuiItem(displayStack, iconX, iconY);
            hoveredMaterials.add(new HoveredMaterial(displayStack, iconX, iconY));

            String havePrefix = have + "/";
            int haveColor = have >= discountedNeed ? 0x55FF55 : 0xFF5555;
            int textX = panelX + 24;
            int textY = panelY + 5 + yOff;
            font.draw(poseStack, havePrefix, textX, textY, haveColor);
            int nextX = textX + font.width(havePrefix);

            if (discountedNeed != need) {
                String oldText = String.valueOf(need);
                font.draw(poseStack, oldText, nextX, textY, 0xFF5555);
                int oldWidth = font.width(oldText);
                fill(poseStack, nextX, textY + 3, nextX + oldWidth, textY + 4, 0xFFFF5555);
                font.draw(poseStack, " " + discountedNeed, nextX + oldWidth + 2, textY, 0x55FF55);
            } else {
                font.draw(poseStack, String.valueOf(need), nextX, textY, 0xFFFFFF);
            }

            yOff += 14;
        }

        renderMaterialScrollbar(poseStack, panelX + 110, panelY + 4, entries.size());
    }

    private void renderMaterialScrollbar(PoseStack poseStack, int x, int y, int totalEntries) {
        if (totalEntries <= MATERIAL_VISIBLE_ROWS) return;

        int barHeight = 112;
        fill(poseStack, x, y, x + 2, y + barHeight, 0xFF1E1E1E);

        int thumbHeight = Math.max(8, barHeight * MATERIAL_VISIBLE_ROWS / totalEntries);
        int maxScroll = Math.max(1, totalEntries - MATERIAL_VISIBLE_ROWS);
        int travel = Math.max(1, barHeight - thumbHeight);
        int thumbY = y + (travel * materialScroll / maxScroll);

        fill(poseStack, x, thumbY, x + 2, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    private void renderInventoryScrollIndicator(PoseStack poseStack, int x, int y) {
        int max = menu.getMaxPrinterScrollRow();
        if (max <= 0) return;

        int barHeight = 88;
        fill(poseStack, x, y, x + 2, y + barHeight, 0xFF1E1E1E);

        int totalRows = (int) Math.ceil((double) menu.getBlockEntity().getItemHandler().getSlots() / Printer3DMenu.PRINTER_COLUMNS);
        int thumbHeight = Math.max(8, barHeight * Printer3DMenu.VISIBLE_PRINTER_ROWS / totalRows);
        int travel = Math.max(1, barHeight - thumbHeight);
        int thumbY = y + (travel * menu.getPrinterScrollRow() / max);

        fill(poseStack, x, thumbY, x + 2, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    private String shorten(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (importDialogOpen) {
            for (ImportFileRowHitbox hitbox : importFileHitboxes) {
                if (hitbox.contains(mouseX, mouseY)) {
                    importRecipeFromFile(hitbox.fileName);
                    return true;
                }
            }
            return handleWidgetsMouseClicked(mouseX, mouseY, button);
        }

        if (scannerMode || renameDialogOpen) {
            return handleWidgetsMouseClicked(mouseX, mouseY, button);
        }

        if (shareDialogOpen) {
            for (SharePlayerRowHitbox hitbox : sharePlayerHitboxes) {
                if (hitbox.contains(mouseX, mouseY)) {
                    shareSelectedRecipeWith(hitbox.targetPlayerId);
                    return true;
                }
            }
            return handleWidgetsMouseClicked(mouseX, mouseY, button);
        }

        for (ImportRowHitbox hitbox : importRowHitboxes) {
            if (hitbox.contains(mouseX, mouseY)) {
                openImportDialog();
                return true;
            }
        }

        for (CustomRecipeRowHitbox hitbox : customRecipeHitboxes) {
            if (hitbox.contains(mouseX, mouseY)) {
                selectCustomRecipe(hitbox.recipeId);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleWidgetsMouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(child);
                if (button == 0) this.setDragging(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scannerMode || renameDialogOpen || shareDialogOpen || importDialogOpen) {
            this.setDragging(false);
            for (GuiEventListener child : this.children()) {
                if (child.mouseReleased(mouseX, mouseY, button)) return true;
            }
            return false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = guiLeft();
        int top = guiTop();

        if (importDialogOpen) {
            int x = left + 102;
            int y = top + 86;
            int w = 208;
            int h = IMPORT_VISIBLE_ROWS * 18;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                int maxScroll = Math.max(0, importableFiles.size() - IMPORT_VISIBLE_ROWS);
                if (delta < 0 && importFileScroll < maxScroll) importFileScroll++;
                else if (delta > 0 && importFileScroll > 0) importFileScroll--;
                return true;
            }
            return false;
        }

        if (shareDialogOpen) {
            int x = left + 112;
            int y = top + 76;
            int w = 196;
            int h = SHARE_VISIBLE_ROWS * 18;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                int total = buildSharePlayerEntries().size();
                int maxScroll = Math.max(0, total - SHARE_VISIBLE_ROWS);
                if (delta < 0 && sharePlayerScroll < maxScroll) sharePlayerScroll++;
                else if (delta > 0 && sharePlayerScroll > 0) sharePlayerScroll--;
                return true;
            }
            return false;
        }

        if (!scannerMode && !renameDialogOpen) {
            int totalCustomEntries = ClientPrinterRecipeLibrary.getRecipes().size() + (ClientPrinterRecipeLibrary.getRecipes().size() < 10 ? 1 : 0);

            if (mouseX >= left + 8 && mouseX <= left + 90 && mouseY >= top + 98 && mouseY <= top + 98 + CUSTOM_VISIBLE_ROWS * 18) {
                int maxScroll = Math.max(0, totalCustomEntries - CUSTOM_VISIBLE_ROWS);
                if (delta < 0 && customRecipeScroll < maxScroll) customRecipeScroll++;
                else if (delta > 0 && customRecipeScroll > 0) customRecipeScroll--;
                return true;
            }

            if (mouseX >= left + 104 && mouseX <= left + 104 + 9 * 18 && mouseY >= top + 30 && mouseY <= top + 30 + Printer3DMenu.VISIBLE_PRINTER_ROWS * 18) {
                int current = menu.getPrinterScrollRow();
                int max = menu.getMaxPrinterScrollRow();
                int newRow = current;
                if (delta < 0 && current < max) newRow = current + 1;
                else if (delta > 0 && current > 0) newRow = current - 1;
                if (newRow != current) {
                    menu.setPrinterScrollRow(newRow);
                    ModPackets.sendToServer(new SyncPrinterScrollPacket(menu.getBlockEntity().getBlockPos(), newRow));
                }
                return true;
            }

            if (mouseX >= left + 292 && mouseX <= left + 408 && mouseY >= top + 28 && mouseY <= top + 140) {
                int totalEntries = getCurrentMaterialEntryCount();
                int maxScroll = Math.max(0, totalEntries - MATERIAL_VISIBLE_ROWS);
                if (delta < 0 && materialScroll < maxScroll) materialScroll++;
                else if (delta > 0 && materialScroll > 0) materialScroll--;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int getCurrentMaterialEntryCount() {
        var be = menu.getBlockEntity();
        if (be.getSelectedRecipe() != null) return be.getSelectedRecipe().getMaterials().size();
        if (be.getClientPreviewCustomRecipe() != null) return be.getClientPreviewCustomRecipe().getMaterials().size();
        return 0;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (renameDialogOpen) {
            return renameEditBox.isFocused() && renameEditBox.charTyped(codePoint, modifiers);
        }

        if (scannerMode) {
            if (scanNameBox.isFocused() && scanNameBox.charTyped(codePoint, modifiers)) return true;
            if (scanXBox.isFocused() && scanXBox.charTyped(codePoint, modifiers)) return true;
            if (scanYBox.isFocused() && scanYBox.charTyped(codePoint, modifiers)) return true;
            if (scanZBox.isFocused() && scanZBox.charTyped(codePoint, modifiers)) return true;
            return false;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (importDialogOpen) {
            if (keyCode == 256) {
                closeImportDialog();
                return true;
            }
            return false;
        }

        if (shareDialogOpen) {
            if (keyCode == 256) {
                closeShareDialog();
                return true;
            }
            return false;
        }

        if (renameDialogOpen) {
            if (keyCode == 256) {
                closeRenameDialog();
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                confirmRename();
                return true;
            }
            if (renameEditBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            return false;
        }

        if (scannerMode) {
            if (scanNameBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (scanXBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (scanYBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (scanZBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            return false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (scannerMode || renameDialogOpen || shareDialogOpen || importDialogOpen) return false;
        return super.checkHotbarKeyPressed(keyCode, scanCode);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (printButton != null) printButton.tick();
        if (scanNameBox != null) scanNameBox.tick();
        if (scanXBox != null) scanXBox.tick();
        if (scanYBox != null) scanYBox.tick();
        if (scanZBox != null) scanZBox.tick();
        if (renameEditBox != null) renameEditBox.tick();
        updateActionButtonsVisibility();
    }

    private void updateActionButtonsVisibility() {
        applyModeVisibility();
    }

    private class AutoButton extends Button {
        public AutoButton(int x, int y, int w, int h, Component text, OnPress cb) {
            super(x, y, w, h, text, cb);
        }

        @Override
        public void renderButton(PoseStack ps, int mx, int my, float t) {
            if (!visible) return;
            int bg = isHoveredOrFocused() ? 0xFF33AA33 : 0xFF227722;
            fill(ps, x, y, x + width, y + height, bg);
            drawCenteredString(ps, Printer3DScreen.this.font, getMessage(), x + width / 2, y + 4, 0xFFFFFF);
        }
    }

    private class FancyButton extends Button {
        public FancyButton(int x, int y, int w, int h, Component text, OnPress cb) {
            super(x, y, w, h, text, cb);
        }

        @Override
        public void renderButton(PoseStack ps, int mx, int my, float t) {
            if (!visible) return;
            int bg = isHoveredOrFocused() ? 0xFF5555AA : 0xFF333377;
            fill(ps, x, y, x + width, y + height, bg);
            drawCenteredString(ps, Printer3DScreen.this.font, getMessage(), x + width / 2, y + 4, 0xFFFFFF);
        }
    }

    private class IconButton extends Button {
        public IconButton(int x, int y, int w, int h, Component text, OnPress cb) {
            super(x, y, w, h, text, cb);
        }

        @Override
        public void renderButton(PoseStack ps, int mx, int my, float t) {
            if (!visible) return;
            int bg = isHoveredOrFocused() ? 0xFF6A6A9A : 0xFF404060;
            fill(ps, x, y, x + width, y + height, bg);
            drawCenteredString(ps, Printer3DScreen.this.font, getMessage(), x + width / 2, y + 4, 0xFFFFFF);
        }
    }

    private class PrintButton extends Button {
        private int holdTicks = 0;
        private boolean pressed = false;

        public PrintButton(int x, int y, int w, int h, Component text, OnPress cb) {
            super(x, y, w, h, text, cb);
        }

        @Override
        public void onPress() {
        }

        @Override
        public void renderButton(PoseStack ps, int mx, int my, float t) {
            if (!visible) return;
            int bg = pressed ? 0xFF00AA00 : (isHoveredOrFocused() ? 0xFFCC8800 : 0xFF884400);
            fill(ps, x, y, x + width, y + height, bg);
            if (pressed) {
                float pct = (float) holdTicks / 40;
                fill(ps, x + 2, y + height - 3, x + (int) ((width - 4) * pct), y + height - 1, 0xFF00FF00);
            }
            drawCenteredString(ps, Printer3DScreen.this.font, getMessage(), x + width / 2, y + 5, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (!visible) return false;
            if (isMouseOver(mx, my)) {
                pressed = true;
                holdTicks = 0;
                playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int btn) {
            if (pressed) {
                if (holdTicks >= 40) onPress.onPress(this);
                pressed = false;
                holdTicks = 0;
                return true;
            }
            return false;
        }

        public void tick() {
            if (pressed && holdTicks < 40) holdTicks++;
        }
    }

    private record HoveredMaterial(ItemStack stack, int x, int y) {
    }

    private record CustomRecipeRowHitbox(String recipeId, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record ImportRowHitbox(int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record SharePlayerEntry(UUID uuid, String name, PlayerInfo info) {
    }

    private record SharePlayerRowHitbox(UUID targetPlayerId, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }

    private record ImportFileRowHitbox(String fileName, String tooltipText, int x1, int y1, int x2, int y2) {
        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }
}
