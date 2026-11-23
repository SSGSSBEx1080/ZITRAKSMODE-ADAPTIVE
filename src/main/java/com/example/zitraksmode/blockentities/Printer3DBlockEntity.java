package com.example.zitraksmode.blockentities;

import com.example.zitraksmode.ModBlockEntities;
import com.example.zitraksmode.blocks.Printer3DBlock;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.printer.BlockPlacement;
import com.example.zitraksmode.printer.Printer3DMenu;
import com.example.zitraksmode.printer.PrinterRecipe;
import com.example.zitraksmode.printer.PrinterStateHelper;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipe;
import com.example.zitraksmode.printer.custom.PlayerStoredPrinterRecipeRuntime;
import com.example.zitraksmode.network.ModPackets;
import com.example.zitraksmode.network.PrinterRecipeSyncChunkPacket;
import com.example.zitraksmode.network.SyncSelectedPrinterRecipePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class Printer3DBlockEntity extends BlockEntity implements MenuProvider {

    public static final int PRINTER_INTERNAL_SLOT_COUNT = 540;

    private final ItemStackHandler itemHandler = new ItemStackHandler(PRINTER_INTERNAL_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private PrinterRecipe selectedRecipe = null;
    private String selectedCustomRecipeId = null;

    @Nullable
    private PlayerStoredPrinterRecipe syncedCustomRecipe = null;

    private boolean isPrinting = false;
    private int printProgress = 0;
    private int totalBlocksToPlace = 0;
    private int placedBlocks = 0;
    private Queue<BlockPlacement> placementQueue = new LinkedList<>();
    private int tickCounter = 0;

    @Nullable
    private UUID printerUUID = null;

    @Nullable
    private UUID currentUser = null;

    @Nullable
    private String currentUserName = null;

    private final Set<BlockPos> alreadyPlaced = new HashSet<>();
    private final Map<BlockPos, Integer> chestRetryCount = new HashMap<>();

    private int barygaLevel = 0;
    private int nahuyaLevel = 0;
    private int zaebisLevel = 0;
    
    /**
     * Полный NBT предмета принтера до установки блока.
     * Здесь сохраняются все зачарования.
     */
    @Nullable
    private CompoundTag storedItemTag;

    @Nullable
    private String bargainItemId = null;
    private int bargainPercent = 0;

    public Printer3DBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTER_3D.get(), pos, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("3D Принтер");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        /*
         * The full custom recipe is intentionally absent from the automatic
         * block-entity update NBT to avoid the 2 MB packet limit. Send it here
         * in safe chunks whenever a player opens this printer instead.
         */
        if (!player.level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (syncedCustomRecipe != null) {
                ModPackets.sendToPlayer(
                        new SyncSelectedPrinterRecipePacket(worldPosition, syncedCustomRecipe),
                        serverPlayer
                );
                PrinterRecipeSyncChunkPacket.sendRecipe(
                        serverPlayer,
                        worldPosition,
                        syncedCustomRecipe
                );
            } else if (selectedCustomRecipeId == null || selectedCustomRecipeId.isBlank()) {
                ModPackets.sendToPlayer(
                        new SyncSelectedPrinterRecipePacket(worldPosition),
                        serverPlayer
                );
            }
        }

        return new Printer3DMenu(id, playerInv, this);
    }

    public boolean canOpenGui(Player player) {
        return currentUser == null || currentUser.equals(player.getUUID());
    }

    @Nullable
    public String getCurrentUserName() {
        return currentUserName;
    }

    public void setCurrentUser(@Nullable Player player) {
        if (player == null) {
            this.currentUser = null;
            this.currentUserName = null;
        } else {
            this.currentUser = player.getUUID();
            this.currentUserName = player.getGameProfile().getName();
        }
        setChanged();
        syncToClient();
    }

    public void releaseUserIfMatches(Player player) {
        if (currentUser != null && currentUser.equals(player.getUUID())) {
            this.currentUser = null;
            this.currentUserName = null;
            setChanged();
            syncToClient();
        }
    }

    public void setBarygaLevel(int level) {
        this.barygaLevel = Math.max(0, Math.min(2, level));
        if (this.barygaLevel <= 0) {
            clearBargain();
        } else {
            rerollBargainForCurrentRecipe();
        }
        setChanged();
        syncToClient();
    }

    public void setNahuyaLevel(int level) {
        this.nahuyaLevel = level;
        setChanged();
        syncToClient();
    }

    public void setZaebisLevel(int level) {
        this.zaebisLevel = level;
        setChanged();
        syncToClient();
    }

    public int getBarygaLevel() {
        return barygaLevel;
    }

    public boolean hasActiveBargain() {
        return bargainItemId != null && !bargainItemId.isBlank() && bargainPercent > 0;
    }

    @Nullable
    public String getBargainItemId() {
        return bargainItemId;
    }

    public int getBargainPercent() {
        return bargainPercent;
    }

    public int getDiscountedRequiredCount(Item item, int originalCount) {
        if (!hasActiveBargain() || item == null || item == Items.AIR || originalCount <= 0) {
            return originalCount;
        }

        ResourceLocation key = Registry.ITEM.getKey(item);
        if (key == null || !key.toString().equals(bargainItemId)) {
            return originalCount;
        }

        int discounted = originalCount - Math.max(1, Math.round(originalCount * (bargainPercent / 100.0f)));
        return Math.max(1, discounted);
    }

    @Nullable
    public Item getBargainItem() {
        if (bargainItemId == null || bargainItemId.isBlank()) {
            return null;
        }
        return Registry.ITEM.get(new ResourceLocation(bargainItemId));
    }

    @Nullable
    public Component getBargainDisplayName() {
        Item item = getBargainItem();
        if (item == null || item == Items.AIR) {
            return null;
        }
        return new ItemStack(item).getHoverName();
    }

    private void clearBargain() {
        this.bargainItemId = null;
        this.bargainPercent = 0;
    }

    private void rerollBargainForCurrentRecipe() {
        if (this.barygaLevel <= 0) {
            clearBargain();
            return;
        }

        Map<Item, Integer> materials = getCurrentRecipeMaterials();
        if (materials == null || materials.isEmpty()) {
            clearBargain();
            return;
        }

        int threshold = this.barygaLevel == 1 ? 20 : 10;
        int percent = this.barygaLevel == 1 ? 5 : 10;

        List<Item> eligible = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : materials.entrySet()) {
            if (entry.getKey() != null && entry.getKey() != Items.AIR && entry.getValue() > threshold) {
                eligible.add(entry.getKey());
            }
        }

        if (eligible.isEmpty()) {
            clearBargain();
            return;
        }

        int index = level != null ? level.random.nextInt(eligible.size()) : new java.util.Random().nextInt(eligible.size());
        Item chosen = eligible.get(index);
        ResourceLocation key = Registry.ITEM.getKey(chosen);

        if (key == null) {
            clearBargain();
            return;
        }

        this.bargainItemId = key.toString();
        this.bargainPercent = percent;
    }

    @Nullable
    private Map<Item, Integer> getCurrentRecipeMaterials() {
        if (selectedRecipe != null) {
            return selectedRecipe.getMaterials();
        }
        if (syncedCustomRecipe != null) {
            return PlayerStoredPrinterRecipeRuntime.toMaterialMap(syncedCustomRecipe);
        }
        return null;
    }

    private Map<Item, Integer> applyBargain(Map<Item, Integer> original) {
        if (original == null || original.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Item, Integer> adjusted = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : original.entrySet()) {
            adjusted.put(entry.getKey(), getDiscountedRequiredCount(entry.getKey(), entry.getValue()));
        }
        return adjusted;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putBoolean("printing", isPrinting);
        tag.putInt("progress", printProgress);
        tag.putInt("placedBlocks", placedBlocks);
        tag.putInt("totalBlocks", totalBlocksToPlace);
        if (selectedRecipe != null) {
            tag.putString("recipe", selectedRecipe.getId());
        }
        if (selectedCustomRecipeId != null && !selectedCustomRecipeId.isBlank()) {
            tag.putString("customRecipeId", selectedCustomRecipeId);
        }
        if (syncedCustomRecipe != null) {
            tag.put("syncedCustomRecipe", syncedCustomRecipe.save());
        }
        if (printerUUID != null) {
            tag.putUUID("printerUUID", printerUUID);
        }
        if (currentUser != null) {
            tag.putUUID("currentUser", currentUser);
        }
        if (currentUserName != null) {
            tag.putString("currentUserName", currentUserName);
        }
        tag.putInt("barygaLevel", barygaLevel);
        tag.putInt("nahuyaLevel", nahuyaLevel);
        tag.putInt("zaebisLevel", zaebisLevel);
        if (bargainItemId != null) {
            tag.putString("bargainItemId", bargainItemId);
        }
        tag.putInt("bargainPercent", bargainPercent);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        isPrinting = tag.getBoolean("printing");
        printProgress = tag.getInt("progress");
        placedBlocks = tag.getInt("placedBlocks");
        totalBlocksToPlace = tag.getInt("totalBlocks");
        if (tag.contains("recipe")) {
            selectedRecipe = PrinterRecipe.getById(tag.getString("recipe"));
        } else {
            selectedRecipe = null;
        }
        if (tag.contains("customRecipeId")) {
            selectedCustomRecipeId = tag.getString("customRecipeId");
        } else {
            selectedCustomRecipeId = null;
        }
        if (tag.contains("syncedCustomRecipe")) {
            syncedCustomRecipe = PlayerStoredPrinterRecipe.load(tag.getCompound("syncedCustomRecipe"));
        } else if (level == null || !level.isClientSide) {
            /*
             * Server/world load without the tag really means no selected custom
             * recipe. Client update tags intentionally omit the huge recipe so
             * they cannot wipe a preview assembled from chunk packets.
             */
            syncedCustomRecipe = null;
        }
        if (tag.hasUUID("printerUUID")) {
            printerUUID = tag.getUUID("printerUUID");
        } else {
            printerUUID = null;
        }
        if (tag.hasUUID("currentUser")) {
            currentUser = tag.getUUID("currentUser");
        } else {
            currentUser = null;
        }
        if (tag.contains("currentUserName")) {
            currentUserName = tag.getString("currentUserName");
        } else {
            currentUserName = null;
        }
        barygaLevel = tag.getInt("barygaLevel");
        nahuyaLevel = tag.getInt("nahuyaLevel");
        zaebisLevel = tag.getInt("zaebisLevel");
        bargainItemId = tag.contains("bargainItemId") ? tag.getString("bargainItemId") : null;
        bargainPercent = tag.getInt("bargainPercent");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag updateTag = saveWithoutMetadata();

        /*
         * A scanned custom recipe may contain thousands of block states.
         * Block entity update packets are capped at 2 MB, so never put the
         * complete recipe into the automatic update tag. It stays persisted in
         * player recipe storage and will be sent later through a chunked packet.
         */
        updateTag.remove("syncedCustomRecipe");
        return updateTag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, inventory);
    }

    private void placeBlockTracked(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
        if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel) {
            net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel) level;
            if (this.zaebisLevel > 0) {
                com.example.zitraksmode.max.PrintedBlocksSavedData.get(sl).setModifier(pos, (byte) 1);
            } else if (this.nahuyaLevel > 0) {
                com.example.zitraksmode.max.PrintedBlocksSavedData.get(sl).setModifier(pos, (byte) 2);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, Printer3DBlockEntity be) {
        if (!be.isPrinting) {
            return;
        }

        if (!be.placementQueue.isEmpty()) {
            be.tickCounter++;
            
            int requiredTicks = be.zaebisLevel > 0 ? 2 : 1;
            if (be.tickCounter >= requiredTicks) {
                be.tickCounter = 0;

                int blocksPerTick = be.nahuyaLevel > 0 ? 2 : 1;
                int safety = 0;
                int placedThisTick = 0;

                while (!be.placementQueue.isEmpty() && safety < 64 && placedThisTick < blocksPerTick) {
                    safety++;
                    BlockPlacement placement = be.placementQueue.poll();
                    if (placement == null) {
                        break;
                    }

                    if (placement.state().isAir()) {
                        continue;
                    }

                    if (be.alreadyPlaced.contains(placement.pos())) {
                        continue;
                    }

                    if (be.tryPlaceSpecialPair(level, placement)) {
                        be.setChanged();
                        placedThisTick++;
                        continue;
                    }

                    if (!be.canSafelyPlace(level, placement)) {
                        be.placementQueue.offer(placement);
                        continue;
                    }

                    be.placeBlockTracked(level, placement.pos(), placement.state());
                    be.alreadyPlaced.add(placement.pos());
                    be.placedBlocks++;
                    be.updateProgress();
                    be.setChanged();
                    placedThisTick++;
                }
            }
        }

        if (be.placementQueue.isEmpty()) {
            be.finishPrinting();
        }
    }

    public void startPrinting(Player player) {
        if (isPrinting) {
            return;
        }

        List<BlockPlacement> allPlacements = null;
        Map<Item, Integer> materials = null;

        if (selectedRecipe != null) {
            materials = selectedRecipe.getMaterials();
            allPlacements = getAllPlacementsRotatedAndSorted();
        } else if (selectedCustomRecipeId != null && !selectedCustomRecipeId.isBlank()) {
            PlayerStoredPrinterRecipe customRecipe = null;
            var library = PrinterRecipeCapability.getLibrary(player);
            if (library != null) {
                customRecipe = library.getRecipe(selectedCustomRecipeId).orElse(null);
            }
            if (customRecipe != null) {
                materials = PlayerStoredPrinterRecipeRuntime.toMaterialMap(customRecipe);
                allPlacements = getAllCustomPlacementsRotatedAndSorted(customRecipe);
            }
        }

        if (materials == null || allPlacements == null || allPlacements.isEmpty()) {
            return;
        }

        Map<Item, Integer> effectiveMaterials = applyBargain(materials);

        if (!hasEnoughMaterials(effectiveMaterials)) {
            return;
        }

        isPrinting = true;
        printProgress = 0;
        placedBlocks = 0;
        tickCounter = 0;
        placementQueue.clear();
        alreadyPlaced.clear();
        chestRetryCount.clear();
        printerUUID = player.getUUID();

        placementQueue.addAll(allPlacements);
        totalBlocksToPlace = allPlacements.size();

        consumeMaterials(effectiveMaterials);

        setChanged();
        syncToClient();
    }

    private void finishPrinting() {
        if (printerUUID != null && selectedRecipe != null && level != null) {
            Player player = level.getPlayerByUUID(printerUUID);
            if (player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer) player;
                switch (selectedRecipe.getId()) {
                    case "hammer": com.example.zitraksmode.ZitraksMode.PRINTER_HAMMER.trigger(sp); break;
                    case "dima_house": com.example.zitraksmode.ZitraksMode.PRINTER_HOUSE.trigger(sp); break;
                    case "prison": com.example.zitraksmode.ZitraksMode.PRINTER_PRISON.trigger(sp); break;
                }
            }
        }

        isPrinting = false;
        printProgress = 0;
        placedBlocks = 0;
        tickCounter = 0;
        placementQueue.clear();
        totalBlocksToPlace = 0;
        selectedRecipe = null;
        selectedCustomRecipeId = null;
        syncedCustomRecipe = null;
        printerUUID = null;
        alreadyPlaced.clear();
        chestRetryCount.clear();
        clearBargain();
        setChanged();
        syncToClient();
    }

    private boolean hasEnoughMaterials(Map<Item, Integer> requiredMaterials) {
        Map<Item, Integer> available = new HashMap<>();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                available.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (Map.Entry<Item, Integer> req : requiredMaterials.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void consumeMaterials(Map<Item, Integer> requiredMaterials) {
        Map<Item, Integer> toConsume = new HashMap<>(requiredMaterials);
        for (int i = 0; i < itemHandler.getSlots() && !toConsume.isEmpty(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (toConsume.containsKey(item)) {
                int need = toConsume.get(item);
                int take = Math.min(need, stack.getCount());
                stack.shrink(take);
                int left = need - take;
                if (left == 0) {
                    toConsume.remove(item);
                } else {
                    toConsume.put(item, left);
                }
            }
        }
        setChanged();
    }

    public void autoFillFromPlayer(Player player) {
        Map<Item, Integer> materialMap = null;
        if (selectedRecipe != null) {
            materialMap = applyBargain(selectedRecipe.getMaterials());
        } else if (selectedCustomRecipeId != null && !selectedCustomRecipeId.isBlank()) {
            PlayerStoredPrinterRecipe customRecipe = null;
            var library = PrinterRecipeCapability.getLibrary(player);
            if (library != null) {
                customRecipe = library.getRecipe(selectedCustomRecipeId).orElse(null);
            }
            if (customRecipe != null) {
                materialMap = applyBargain(PlayerStoredPrinterRecipeRuntime.toMaterialMap(customRecipe));
            }
        }

        if (materialMap == null || materialMap.isEmpty()) {
            return;
        }

        if (player.getAbilities().instabuild) {
            fillCreative(materialMap);
            setChanged();
            return;
        }

        Map<Item, Integer> need = new HashMap<>(materialMap);

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (need.containsKey(item)) {
                    int left = need.get(item) - stack.getCount();
                    if (left <= 0) {
                        need.remove(item);
                    } else {
                        need.put(item, left);
                    }
                }
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize() && !need.isEmpty(); i++) {
            ItemStack playerStack = player.getInventory().getItem(i);
            if (playerStack.isEmpty()) {
                continue;
            }
            Item item = playerStack.getItem();
            if (!need.containsKey(item)) {
                continue;
            }

            int needCount = need.get(item);
            int moveCount = Math.min(needCount, playerStack.getCount());

            int remaining = insertIntoPrinter(new ItemStack(item, moveCount));
            int inserted = moveCount - remaining;
            if (inserted > 0) {
                playerStack.shrink(inserted);
                int left = needCount - inserted;
                if (left <= 0) {
                    need.remove(item);
                } else {
                    need.put(item, left);
                }
            }
        }
        setChanged();
    }

    private void fillCreative(Map<Item, Integer> materialMap) {
        clearPrinter();
        int slot = 0;
        for (Map.Entry<Item, Integer> entry : materialMap.entrySet()) {
            int count = entry.getValue();
            while (count > 0 && slot < itemHandler.getSlots()) {
                int put = Math.min(64, count);
                itemHandler.setStackInSlot(slot, new ItemStack(entry.getKey(), put));
                count -= put;
                slot++;
            }
        }
    }

    private void clearPrinter() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private int insertIntoPrinter(ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, stack)) {
                int canAdd = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), remaining);
                if (canAdd > 0) {
                    slotStack.grow(canAdd);
                    remaining -= canAdd;
                    itemHandler.setStackInSlot(i, slotStack);
                    if (remaining <= 0) {
                        return 0;
                    }
                }
            }
        }
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                int put = Math.min(stack.getMaxStackSize(), remaining);
                itemHandler.setStackInSlot(i, new ItemStack(stack.getItem(), put));
                remaining -= put;
                if (remaining <= 0) {
                    return 0;
                }
            }
        }
        return remaining;
    }

    private Direction getPrinterFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(Printer3DBlock.FACING)) {
            return state.getValue(Printer3DBlock.FACING);
        }
        return Direction.NORTH;
    }

    private Rotation getRecipeRotation() {
        switch (getPrinterFacing()) {
            case NORTH: return Rotation.NONE;
            case EAST: return Rotation.CLOCKWISE_90;
            case SOUTH: return Rotation.CLOCKWISE_180;
            case WEST: return Rotation.COUNTERCLOCKWISE_90;
            default: return Rotation.NONE;
        }
    }

    private BlockPlacement rotatePlacement(BlockPlacement original) {
        BlockPos printer = worldPosition;
        Rotation rotation = getRecipeRotation();

        int relX = original.pos().getX() - printer.getX();
        int relY = original.pos().getY() - printer.getY();
        int relZ = original.pos().getZ() - printer.getZ();

        int rx = relX;
        int rz = relZ;
        switch (rotation) {
            case NONE:
                rx = relX;
                rz = relZ;
                break;
            case CLOCKWISE_90:
                rx = -relZ;
                rz = relX;
                break;
            case CLOCKWISE_180:
                rx = -relX;
                rz = -relZ;
                break;
            case COUNTERCLOCKWISE_90:
                rx = relZ;
                rz = -relX;
                break;
        }

        BlockPos rotatedPos = new BlockPos(printer.getX() + rx, printer.getY() + relY, printer.getZ() + rz);
        BlockState rotatedState = rotateStateSmart(original.state(), rotation);
        return new BlockPlacement(rotatedPos, rotatedState, original.requiredItem());
    }

    private BlockState rotateStateSmart(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return state;
        }

        BlockState result;
        try {
            result = state.rotate(rotation);
        } catch (Exception ignored) {
            result = state;
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction orig = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction curr = result.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? result.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : orig;
            if (curr == orig) {
                try {
                    result = result.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(orig));
                } catch (Exception ignored) {
                }
            }
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction orig = state.getValue(BlockStateProperties.FACING);
            Direction curr = result.hasProperty(BlockStateProperties.FACING)
                    ? result.getValue(BlockStateProperties.FACING)
                    : orig;
            if (curr == orig && orig.getAxis().isHorizontal()) {
                try {
                    result = result.setValue(BlockStateProperties.FACING, rotation.rotate(orig));
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    private List<BlockPlacement> getAllPlacementsRotatedAndSorted() {
        List<BlockPlacement> result = new ArrayList<>();
        if (selectedRecipe == null) {
            return result;
        }
        for (int layer = 0; layer < selectedRecipe.getTotalLayers(); layer++) {
            BlockPos start = worldPosition.offset(1, 0, 1);
            List<BlockPlacement> raw = selectedRecipe.getLayerBlocks(start, layer);
            for (BlockPlacement placement : raw) {
                BlockPlacement rotated = rotatePlacement(placement);
                if (!rotated.state().isAir()) {
                    result.add(rotated);
                }
            }
        }
        sortPlacements(result);
        return result;
    }

    private List<BlockPlacement> getAllCustomPlacementsRotatedAndSorted(PlayerStoredPrinterRecipe customRecipe) {
        List<BlockPlacement> result = new ArrayList<>();
        BlockPos start = worldPosition;
        List<BlockPlacement> raw = PlayerStoredPrinterRecipeRuntime.toPlacements(start, customRecipe);
        for (BlockPlacement placement : raw) {
            BlockPlacement rotated = rotatePlacement(placement);
            if (!rotated.state().isAir()) {
                result.add(rotated);
            }
        }
        sortPlacements(result);
        return result;
    }

    private void sortPlacements(List<BlockPlacement> placements) {
        placements.sort(Comparator
                .comparingInt(this::placementPriority)
                .thenComparingInt(p -> p.pos().getY())
                .thenComparingInt(p -> p.pos().getX())
                .thenComparingInt(p -> p.pos().getZ()));
    }

    private int placementPriority(BlockPlacement p) {
        BlockState state = p.state();

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.UPPER) return 100;
            return 20;
        }

        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            if (part == BedPart.HEAD) return 100;
            return 20;
        }

        if (state.hasProperty(BlockStateProperties.CHEST_TYPE)) {
            ChestType type = state.getValue(BlockStateProperties.CHEST_TYPE);
            if (type != ChestType.SINGLE) return 25;
        }

        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) return 50;
        if (state.hasProperty(BlockStateProperties.HANGING)) return 50;

        String id = String.valueOf(Registry.BLOCK.getKey(state.getBlock()));
        if (id.contains("button")) return 50;
        if (id.contains("wall_sign")) return 50;
        if (id.contains("sign")) return 45;
        if (id.contains("carpet")) return 60;
        if (id.contains("tripwire")) return 60;
        if (id.contains("lantern")) return 55;
        if (id.contains("end_rod")) return 55;
        if (id.contains("flower_pot") || id.contains("potted_")) return 70;

        return 0;
    }

    private boolean canSafelyPlace(Level level, BlockPlacement placement) {
        BlockState state = placement.state();
        BlockPos pos = placement.pos();

        if (alreadyPlaced.contains(pos)) return false;
        if (state.isAir()) return false;

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.UPPER) return false;
        }

        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            if (part == BedPart.HEAD) return false;
        }

        try {
            return state.canSurvive(level, pos);
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean tryPlaceSpecialPair(Level level, BlockPlacement placement) {
        BlockState state = placement.state();
        BlockPos pos = placement.pos();

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.LOWER) {
                BlockPos upperPos = pos.above();
                if (alreadyPlaced.contains(pos) || alreadyPlaced.contains(upperPos)) return true;
                BlockState upperState = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                if (state.canSurvive(level, pos)) {
                    placeBlockTracked(level, pos, state);
                    placeBlockTracked(level, upperPos, upperState);
                    alreadyPlaced.add(pos);
                    alreadyPlaced.add(upperPos);
                    placedBlocks += 2;
                    updateProgress();
                    return true;
                }
            }
        }

        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            if (part == BedPart.FOOT && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                BlockPos headPos = pos.relative(facing);
                if (alreadyPlaced.contains(pos) || alreadyPlaced.contains(headPos)) return true;
                BlockState headState = state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
                if (state.canSurvive(level, pos) && headState.canSurvive(level, headPos)) {
                    placeBlockTracked(level, pos, state);
                    placeBlockTracked(level, headPos, headState);
                    alreadyPlaced.add(pos);
                    alreadyPlaced.add(headPos);
                    placedBlocks += 2;
                    updateProgress();
                    return true;
                }
            }
        }

        if (state.hasProperty(BlockStateProperties.CHEST_TYPE)
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            ChestType type = state.getValue(BlockStateProperties.CHEST_TYPE);
            if (type != ChestType.SINGLE) {
                return handleChestPair(level, placement, type);
            }
        }

        return false;
    }

    private boolean handleChestPair(Level level, BlockPlacement placement, ChestType type) {
        BlockState state = placement.state();
        BlockPos pos = placement.pos();
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        Direction neighborDir = (type == ChestType.LEFT) ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos neighborPos = pos.relative(neighborDir);

        if (alreadyPlaced.contains(pos)) return true;

        if (alreadyPlaced.contains(neighborPos)) {
            if (state.canSurvive(level, pos)) {
                BlockState singleState = state.setValue(BlockStateProperties.CHEST_TYPE, ChestType.SINGLE);
                placeBlockTracked(level, pos, singleState);
                alreadyPlaced.add(pos);
                placedBlocks++;
                updateProgress();
            }
            return true;
        }

        ChestType expectedNeighborType = (type == ChestType.LEFT) ? ChestType.RIGHT : ChestType.LEFT;
        BlockPlacement neighborPlacement = findAndRemoveFromQueue(neighborPos,
                s -> s.getBlock() == state.getBlock()
                        && s.hasProperty(BlockStateProperties.CHEST_TYPE)
                        && s.getValue(BlockStateProperties.CHEST_TYPE) == expectedNeighborType
                        && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        && s.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing);

        if (neighborPlacement != null) {
            if (state.canSurvive(level, pos) && neighborPlacement.state().canSurvive(level, neighborPos)) {
                placeBlockTracked(level, pos, state);
                placeBlockTracked(level, neighborPos, neighborPlacement.state());
                alreadyPlaced.add(pos);
                alreadyPlaced.add(neighborPos);
                placedBlocks += 2;
                updateProgress();
                chestRetryCount.remove(pos);
                chestRetryCount.remove(neighborPos);
                return true;
            }
        }

        int retries = chestRetryCount.getOrDefault(pos, 0) + 1;
        chestRetryCount.put(pos, retries);

        if (retries >= 3) {
            if (state.canSurvive(level, pos)) {
                BlockState singleState = state.setValue(BlockStateProperties.CHEST_TYPE, ChestType.SINGLE);
                placeBlockTracked(level, pos, singleState);
                alreadyPlaced.add(pos);
                placedBlocks++;
                updateProgress();
                chestRetryCount.remove(pos);
            }
            return true;
        }

        placementQueue.offer(placement);
        return true;
    }

    @Nullable
    private BlockPlacement findAndRemoveFromQueue(BlockPos targetPos, Predicate<BlockState> predicate) {
        Iterator<BlockPlacement> it = placementQueue.iterator();
        while (it.hasNext()) {
            BlockPlacement p = it.next();
            if (p.pos().equals(targetPos) && predicate.test(p.state())) {
                it.remove();
                return p;
            }
        }
        return null;
    }

    void updateProgress() {
        printProgress = totalBlocksToPlace > 0
                ? (int) ((float) placedBlocks / totalBlocksToPlace * 100)
                : 0;
    }

    public List<BlockPlacement> getPreviewPlacements() {
        if (isPrinting) {
            return Collections.emptyList();
        }
        if (selectedRecipe != null) {
            return getAllPlacementsRotatedAndSorted();
        }
        if (syncedCustomRecipe != null) {
            return getAllCustomPlacementsRotatedAndSorted(syncedCustomRecipe);
        }
        return Collections.emptyList();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getPrintProgress() {
        return printProgress;
    }

    public boolean isPrinting() {
        return isPrinting;
    }

    public PrinterRecipe getSelectedRecipe() {
        return selectedRecipe;
    }

    @Nullable
    public String getSelectedCustomRecipeId() {
        return selectedCustomRecipeId;
    }

    public boolean hasSelectedCustomRecipe() {
        return selectedCustomRecipeId != null && !selectedCustomRecipeId.isBlank();
    }

    @Nullable
    public PlayerStoredPrinterRecipe getClientPreviewCustomRecipe() {
        return syncedCustomRecipe;
    }

    public void setSelectedRecipe(PrinterRecipe recipe) {
        this.selectedRecipe = recipe;
        this.selectedCustomRecipeId = null;
        this.syncedCustomRecipe = null;
        if (this.level != null && !this.level.isClientSide) {
            rerollBargainForCurrentRecipe();
        }
        setChanged();
        syncToClient();
    }

    public void setSelectedCustomRecipe(String recipeId, @Nullable Player player) {
        this.selectedRecipe = null;
        this.selectedCustomRecipeId = recipeId;
        this.syncedCustomRecipe = null;

        if (player != null && recipeId != null && !recipeId.isBlank()) {
            var library = PrinterRecipeCapability.getLibrary(player);
            if (library != null) {
                PlayerStoredPrinterRecipe recipe = library.getRecipe(recipeId).orElse(null);
                if (recipe != null) {
                    this.syncedCustomRecipe = recipe.copy();
                }
            }
        }

        if (this.level != null && !this.level.isClientSide) {
            rerollBargainForCurrentRecipe();
        }
        setChanged();
        syncToClient();
    }

    public void setSelectedCustomRecipe(String recipeId) {
        setSelectedCustomRecipe(recipeId, null);
    }

    public void setClientPreviewCustomRecipe(@Nullable PlayerStoredPrinterRecipe recipe) {
        this.syncedCustomRecipe = recipe == null ? null : recipe.copy();
    }

    public void clearSelectedCustomRecipeClientSide() {
        this.selectedCustomRecipeId = null;
        this.syncedCustomRecipe = null;
    }

    public void clearSelectedCustomRecipeSelection() {
        this.selectedCustomRecipeId = null;
        this.syncedCustomRecipe = null;
        clearBargain();
        setChanged();
        syncToClient();
    }
}
