package com.example.zitraksmode.max;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;

public class PrintedBlocksSavedData extends SavedData {
    private final Map<Long, Byte> modifiers = new HashMap<>();

    public static PrintedBlocksSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PrintedBlocksSavedData::load, PrintedBlocksSavedData::new, "zitraksmode_printed_blocks");
    }

    public byte getModifier(BlockPos pos) {
        return modifiers.getOrDefault(pos.asLong(), (byte) 0);
    }

    public void setModifier(BlockPos pos, byte mod) {
        modifiers.put(pos.asLong(), mod);
        setDirty();
    }

    public void removeModifier(BlockPos pos) {
        if (modifiers.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] keys = new long[modifiers.size()];
        byte[] values = new byte[modifiers.size()];
        int i = 0;
        for (Map.Entry<Long, Byte> entry : modifiers.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        tag.putLongArray("Positions", keys);
        tag.putByteArray("Modifiers", values);
        return tag;
    }

    public static PrintedBlocksSavedData load(CompoundTag tag) {
        PrintedBlocksSavedData data = new PrintedBlocksSavedData();
        long[] keys = tag.getLongArray("Positions");
        byte[] values = tag.getByteArray("Modifiers");
        int len = Math.min(keys.length, values.length);
        for (int i = 0; i < len; i++) {
            data.modifiers.put(keys[i], values[i]);
        }
        return data;
    }
}
