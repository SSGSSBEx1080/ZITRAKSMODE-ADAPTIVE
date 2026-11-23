package com.example.zitraksmode.caps;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnderCapability {

    public static final Capability<ItemStackHandler> BIG_ENDER_INVENTORY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final ItemStackHandler inventory = new ItemStackHandler(54);
        private final LazyOptional<ItemStackHandler> optional = LazyOptional.of(() -> inventory);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == BIG_ENDER_INVENTORY) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return inventory.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            inventory.deserializeNBT(nbt);
        }

        public ItemStackHandler getInventory() {
            return inventory;
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}