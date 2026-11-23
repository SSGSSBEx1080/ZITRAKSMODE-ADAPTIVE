package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModBlockEntities;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnderFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    private UUID owner;

    /**
     * Сколько ещё ПРЕДМЕТОВ можно переплавить текущим топливом.
     * Око = 64
     * Жемчуг = 32
     */
    private int remainingSmelts = 0;

    /**
     * Скорость текущего топлива:
     * Око = 200
     * Жемчуг = 100
     */
    private int currentFuelSpeed = 0;

    /**
     * Максимальная ёмкость текущего топлива для GUI:
     * Око = 64
     * Жемчуг = 32
     */
    private int currentFuelCapacity = 0;

    public EnderFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_FURNACE.get(), pos, state, RecipeType.SMELTING);
    }

    public @Nullable UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }

        this.remainingSmelts = tag.getInt("RemainingSmelts");
        this.currentFuelSpeed = tag.getInt("CurrentFuelSpeed");
        this.currentFuelCapacity = tag.getInt("CurrentFuelCapacity");

        // Синхронизируем GUI-поля после загрузки
        this.dataAccess.set(0, this.remainingSmelts);
        this.dataAccess.set(1, this.currentFuelCapacity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }

        tag.putInt("RemainingSmelts", this.remainingSmelts);
        tag.putInt("CurrentFuelSpeed", this.currentFuelSpeed);
        tag.putInt("CurrentFuelCapacity", this.currentFuelCapacity);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.zitraksmode.ender_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv) {
        return new EnderFurnaceMenu(id, playerInv, this, this.dataAccess);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // В слот топлива пускаем только око/жемчуг
        if (slot == 1) {
            return isEnderFuel(stack);
        }

        // В слот результата класть нельзя
        if (slot == 2) {
            return false;
        }

        return super.canPlaceItem(slot, stack);
    }

    private static boolean isEnderFuel(ItemStack stack) {
        return stack.is(Items.ENDER_EYE) || stack.is(Items.ENDER_PEARL);
    }

    // ========== СИНХРОНИЗАЦИЯ ==========

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.load(tag);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    // ========== ОСНОВНАЯ ЛОГИКА ==========

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderFurnaceBlockEntity blockEntity) {
        boolean changed = false;

        Recipe<Container> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, blockEntity, level)
                .orElse(null);

        int maxStackSize = blockEntity.getMaxStackSize();

        // Если зарядов нет, но можем плавить — пытаемся зарядиться новым топливом
        if (blockEntity.remainingSmelts <= 0 && canBurn(recipe, blockEntity, maxStackSize)) {
            ItemStack fuelStack = blockEntity.items.get(1);

            if (!fuelStack.isEmpty() && isEnderFuel(fuelStack)) {
                blockEntity.remainingSmelts = getFuelSmelts(fuelStack);
                blockEntity.currentFuelSpeed = getFuelSpeed(fuelStack);
                blockEntity.currentFuelCapacity = blockEntity.remainingSmelts;

                if (fuelStack.hasCraftingRemainingItem()) {
                    blockEntity.items.set(1, fuelStack.getCraftingRemainingItem());
                } else {
                    fuelStack.shrink(1);
                }

                changed = true;
            }
        }

        // Для GUI пламени
        blockEntity.dataAccess.set(0, blockEntity.remainingSmelts);
        blockEntity.dataAccess.set(1, blockEntity.currentFuelCapacity);

        // Если есть заряды и можно плавить — плавим
        if (blockEntity.remainingSmelts > 0 && canBurn(recipe, blockEntity, maxStackSize)) {
            int totalCookTime = getTotalCookTime(level, blockEntity);
            blockEntity.dataAccess.set(3, totalCookTime);

            int speed = blockEntity.currentFuelSpeed > 0 ? blockEntity.currentFuelSpeed : 1;
            int currentProgress = blockEntity.dataAccess.get(2) + speed;

            if (currentProgress >= totalCookTime) {
                blockEntity.dataAccess.set(2, 0);
                burn(recipe, blockEntity, maxStackSize);

                // ВАЖНО: тратим заряд только за реально завершённую переплавку
                blockEntity.remainingSmelts--;

                if (blockEntity.remainingSmelts <= 0) {
                    blockEntity.remainingSmelts = 0;
                    blockEntity.currentFuelSpeed = 0;
                    blockEntity.currentFuelCapacity = 0;
                }

                blockEntity.dataAccess.set(0, blockEntity.remainingSmelts);
                blockEntity.dataAccess.set(1, blockEntity.currentFuelCapacity);

                changed = true;
            } else {
                blockEntity.dataAccess.set(2, currentProgress);
            }
        } else {
            // Если сейчас плавить нельзя — прогресс медленно откатывается,
            // но топливо НЕ тратится
            if (blockEntity.dataAccess.get(2) > 0) {
                blockEntity.dataAccess.set(2, Math.max(0, blockEntity.dataAccess.get(2) - 2));
            }
        }

        boolean isBurningNow = blockEntity.remainingSmelts > 0;
        boolean wasBurning = state.getValue(AbstractFurnaceBlock.LIT);

        if (wasBurning != isBurningNow) {
            state = state.setValue(AbstractFurnaceBlock.LIT, isBurningNow);
            level.setBlock(pos, state, 3);
            changed = true;
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    private static int getFuelSmelts(ItemStack fuel) {
        if (fuel.is(Items.ENDER_EYE)) return 64;
        if (fuel.is(Items.ENDER_PEARL)) return 32;
        return 0;
    }

    private static int getFuelSpeed(ItemStack fuel) {
        if (fuel.is(Items.ENDER_EYE)) return 200;
        if (fuel.is(Items.ENDER_PEARL)) return 100;
        return 0;
    }

    private static boolean canBurn(@Nullable Recipe<Container> recipe, EnderFurnaceBlockEntity entity, int maxStack) {
        if (entity.items.get(0).isEmpty() || recipe == null) return false;

        ItemStack resultStack = recipe.assemble(entity);
        if (resultStack.isEmpty()) return false;

        ItemStack outputStack = entity.items.get(2);
        if (outputStack.isEmpty()) return true;

        if (!ItemStack.isSameItemSameTags(outputStack, resultStack)) return false;

        return (outputStack.getCount() + resultStack.getCount()) <= maxStack
                && (outputStack.getCount() + resultStack.getCount()) <= outputStack.getMaxStackSize();
    }

    private static void burn(@Nullable Recipe<Container> recipe, EnderFurnaceBlockEntity entity, int maxStack) {
        if (recipe != null && canBurn(recipe, entity, maxStack)) {
            ItemStack input = entity.items.get(0);
            ItemStack result = recipe.assemble(entity);
            ItemStack output = entity.items.get(2);

            if (output.isEmpty()) {
                entity.items.set(2, result.copy());
            } else {
                output.grow(result.getCount());
            }

            input.shrink(1);
        }
    }

    private static int getTotalCookTime(Level level, Container container) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, container, level)
                .map(r -> r instanceof AbstractCookingRecipe ? ((AbstractCookingRecipe) r).getCookingTime() : 200)
                .orElse(200);
    }
}