package com.example.zitraksmode.blockentities;

import com.example.zitraksmode.ModBlockEntities;
import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.items.DildoMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DildoBlockEntity extends BlockEntity {

    private DildoMaterial material;
    private int morichekLevel;
    private int bedWarsLevel;

    /*
     * Полный NBT предмета, который был установлен.
     * Здесь сохраняются все чары, а не только Morichek и Bed Wars.
     */
    @Nullable
    private CompoundTag storedItemTag;

    public DildoBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, DildoMaterial.STONE);
    }

    public DildoBlockEntity(
            BlockPos pos,
            BlockState state,
            DildoMaterial material
    ) {
        super(
                ModBlockEntities.DILDO_BLOCK.get(),
                pos,
                state
        );

        this.material = material;
    }

    public DildoMaterial getMaterial() {
        return material;
    }

    public void setMaterial(@Nullable DildoMaterial material) {
        this.material = material == null
                ? DildoMaterial.STONE
                : material;

        setChanged();
    }

    public int getMorichekLevel() {
        return morichekLevel;
    }

    public void setMorichekLevel(int morichekLevel) {
        this.morichekLevel = Math.max(0, morichekLevel);
        setChanged();
    }

    public int getBedWarsLevel() {
        return bedWarsLevel;
    }

    public void setBedWarsLevel(int bedWarsLevel) {
        this.bedWarsLevel = Math.max(0, bedWarsLevel);
        setChanged();
    }

    /**
     * Сохраняет все данные зачарованного предмета,
     * который был поставлен как блок.
     */
    public void setStoredItemTag(@Nullable CompoundTag tag) {
        this.storedItemTag = tag == null
                ? null
                : tag.copy();

        setChanged();
    }

    /**
     * Накладывает сохранённые чары на выпавший предмет.
     */
    public void applyStoredItemData(ItemStack stack) {
        if (storedItemTag != null) {
            stack.setTag(storedItemTag.copy());
            return;
        }

        /*
         * Совместимость со старыми уже установленными дилдаками:
         * если полный NBT ещё не сохранялся, восстанавливаем хотя бы
         * два старых зачарования из отдельных полей.
         */
        Map<Enchantment, Integer> enchantments = new HashMap<>();

        if (morichekLevel > 0) {
            enchantments.put(
                    ModEnchantments.MORICHEK.get(),
                    morichekLevel
            );
        }

        if (bedWarsLevel > 0) {
            enchantments.put(
                    ModEnchantments.BED_WARS.get(),
                    bedWarsLevel
            );
        }

        if (!enchantments.isEmpty()) {
            EnchantmentHelper.setEnchantments(
                    enchantments,
                    stack
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString(
                "Material",
                material.name()
        );

        tag.putInt(
                "MorichekLevel",
                morichekLevel
        );

        tag.putInt(
                "BedWarsLevel",
                bedWarsLevel
        );

        if (storedItemTag != null) {
            tag.put(
                    "StoredItemTag",
                    storedItemTag.copy()
            );
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        try {
            this.material = DildoMaterial.valueOf(
                    tag.getString("Material")
            );
        } catch (Exception ignored) {
            this.material = DildoMaterial.STONE;
        }

        this.morichekLevel = tag.getInt("MorichekLevel");
        this.bedWarsLevel = tag.getInt("BedWarsLevel");

        if (tag.contains("StoredItemTag")) {
            this.storedItemTag = tag
                    .getCompound("StoredItemTag")
                    .copy();
        } else {
            this.storedItemTag = null;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}