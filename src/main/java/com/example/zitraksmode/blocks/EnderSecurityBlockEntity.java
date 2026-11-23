package com.example.zitraksmode.blocks;

import com.example.zitraksmode.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Универсальная сущность для "простых" эндер-блоков (кнопки, плиты, люки).
 * Позволяет этим блокам хранить данные о владельце в мире.
 */
public class EnderSecurityBlockEntity extends BlockEntity implements IOwnable {
    private UUID owner;

    public EnderSecurityBlockEntity(BlockPos pos, BlockState state) {
        // Используем общую регистрацию для всех защищенных блоков
        super(ModBlockEntities.ENDER_SECURITY.get(), pos, state);
    }

    @Override
    public @Nullable UUID getOwner() {
        return owner;
    }

    @Override
    public void setOwner(UUID owner) {
        this.owner = owner;
        this.setChanged(); // Сообщаем игре, что данные изменились и их нужно сохранить
    }

    // Сохранение данных в файл мира
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
    }

    // Загрузка данных из файла мира
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }
    }
}