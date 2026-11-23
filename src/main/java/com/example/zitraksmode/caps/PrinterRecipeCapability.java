package com.example.zitraksmode.caps;

import com.example.zitraksmode.printer.custom.PlayerPrinterRecipeLibrary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoRegisterCapability
public class PrinterRecipeCapability {

    public static final ResourceLocation ID = new ResourceLocation("zitraksmode", "player_printer_recipes");

    public static final Capability<PlayerPrinterRecipeLibrary> PLAYER_PRINTER_RECIPES =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private PrinterRecipeCapability() {
    }

    public static LazyOptional<PlayerPrinterRecipeLibrary> get(Player player) {
        return player.getCapability(PLAYER_PRINTER_RECIPES);
    }

    @Nullable
    public static PlayerPrinterRecipeLibrary getLibrary(Player player) {
        return get(player).orElse(null);
    }

    public static void copy(Player original, Player clone) {
        PlayerPrinterRecipeLibrary oldLibrary = getLibrary(original);
        PlayerPrinterRecipeLibrary newLibrary = getLibrary(clone);
        if (oldLibrary != null && newLibrary != null) {
            newLibrary.copyFrom(oldLibrary);
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerPrinterRecipeLibrary library = new PlayerPrinterRecipeLibrary();
        private final LazyOptional<PlayerPrinterRecipeLibrary> optional = LazyOptional.of(() -> library);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            if (cap == PLAYER_PRINTER_RECIPES) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return library.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            library.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
