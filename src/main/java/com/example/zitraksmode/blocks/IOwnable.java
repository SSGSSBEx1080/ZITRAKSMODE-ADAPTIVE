package com.example.zitraksmode.blocks;

import java.util.UUID;
import javax.annotation.Nullable;

public interface IOwnable {
    @Nullable
    UUID getOwner();
    void setOwner(UUID owner);
    
    default boolean isOwner(UUID uuid) {
        return getOwner() != null && getOwner().equals(uuid);
    }
}