package com.example.zitraksmode.util;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Persistent player-side state for every One Piece cloak. */
public final class LogiaGaugeCapability {
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "logia_gauge");
    public static final Capability<LogiaGauge> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() { });

    private LogiaGaugeCapability() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(LogiaGauge.class);
    }

    public static final class LogiaGauge implements INBTSerializable<CompoundTag> {
        private int gauge;
        private float regenFraction;
        private int ticksSinceSpend = 600;
        private int kizaruCooldown;
        /** Ten-second half-heart invulnerability window. */
        private int whitebeardLockTicks;
        /** Separate 120-second cooldown before Whitebeard's last stand can trigger again. */
        private int whitebeardCooldownTicks;
        private int obtainedCloaksMask;
        private boolean initialized;
        private long pendingExplosionTick = Long.MIN_VALUE;
        private boolean dirty = true;

        public int getGauge() {
            return gauge;
        }

        public void setGauge(int gauge) {
            int newValue = Math.max(0, gauge);
            if (this.gauge != newValue) {
                this.gauge = newValue;
                this.dirty = true;
            }
        }

        public boolean trySpend(int amount) {
            if (amount <= 0) return true;
            if (gauge < amount) return false;
            gauge -= amount;
            ticksSinceSpend = 0;
            dirty = true;
            return true;
        }

        /** Uses all remaining charge when the explosion cost exceeds it. */
        public boolean spendForExplosion(int amount, long gameTime) {
            if (gauge <= 0) return false;
            gauge = Math.max(0, gauge - Math.max(1, amount));
            ticksSinceSpend = 0;
            pendingExplosionTick = gameTime;
            dirty = true;
            return true;
        }

        public boolean consumeExplosionShield(long gameTime) {
            if (pendingExplosionTick != gameTime) return false;
            pendingExplosionTick = Long.MIN_VALUE;
            return true;
        }

        public void addRegen(float amount, int cap) {
            if (amount <= 0.0F || cap <= 0 || gauge >= cap) return;
            regenFraction += amount;
            int whole = (int) regenFraction;
            if (whole > 0) {
                regenFraction -= whole;
                int old = gauge;
                gauge = Math.min(cap, gauge + whole);
                dirty |= old != gauge;
            }
        }

        public int getTicksSinceSpend() {
            return ticksSinceSpend;
        }

        public void tick() {
            // No artificial cap: cloak regeneration may keep accelerating forever
            // while the player has not spent gauge.
            if (ticksSinceSpend < Integer.MAX_VALUE) ticksSinceSpend++;
            if (kizaruCooldown > 0) kizaruCooldown--;
            if (whitebeardLockTicks > 0) whitebeardLockTicks--;
            if (whitebeardCooldownTicks > 0) whitebeardCooldownTicks--;
        }

        public int getKizaruCooldown() {
            return kizaruCooldown;
        }

        public void setKizaruCooldown(int ticks) {
            kizaruCooldown = Math.max(0, ticks);
            dirty = true;
        }

        public int getWhitebeardLockTicks() {
            return whitebeardLockTicks;
        }

        public void setWhitebeardLockTicks(int ticks) {
            whitebeardLockTicks = Math.max(0, ticks);
            dirty = true;
        }

        public int getWhitebeardCooldownTicks() {
            return whitebeardCooldownTicks;
        }

        public void setWhitebeardCooldownTicks(int ticks) {
            whitebeardCooldownTicks = Math.max(0, ticks);
            dirty = true;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
            dirty = true;
        }

        public boolean markCloakObtained(int bit) {
            int before = obtainedCloaksMask;
            obtainedCloaksMask |= bit;
            if (before != obtainedCloaksMask) dirty = true;
            return before != obtainedCloaksMask;
        }

        public boolean hasCloakObtained(int bit) {
            return (obtainedCloaksMask & bit) != 0;
        }

        public boolean hasAllCloaks() {
            return (obtainedCloaksMask & 0b11_1111) == 0b11_1111;
        }

        public boolean isDirty() {
            return dirty;
        }

        public void clearDirty() {
            dirty = false;
        }

        public void copyFrom(LogiaGauge other) {
            deserializeNBT(other.serializeNBT());
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Gauge", gauge);
            tag.putFloat("RegenFraction", regenFraction);
            tag.putInt("TicksSinceSpend", ticksSinceSpend);
            tag.putInt("KizaruCooldown", kizaruCooldown);
            tag.putInt("WhitebeardLockTicks", whitebeardLockTicks);
            tag.putInt("WhitebeardCooldownTicks", whitebeardCooldownTicks);
            tag.putInt("ObtainedCloaksMask", obtainedCloaksMask);
            tag.putBoolean("Initialized", initialized);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            gauge = Math.max(0, tag.getInt("Gauge"));
            regenFraction = Math.max(0.0F, tag.getFloat("RegenFraction"));
            ticksSinceSpend = Math.max(0, tag.getInt("TicksSinceSpend"));
            kizaruCooldown = Math.max(0, tag.getInt("KizaruCooldown"));
            whitebeardLockTicks = Math.max(0, tag.getInt("WhitebeardLockTicks"));
            whitebeardCooldownTicks = Math.max(0, tag.getInt("WhitebeardCooldownTicks"));
            obtainedCloaksMask = tag.getInt("ObtainedCloaksMask");
            initialized = tag.getBoolean("Initialized");
            dirty = true;
        }
    }

    public static final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final LogiaGauge data = new LogiaGauge();
        private final LazyOptional<LogiaGauge> optional = LazyOptional.of(() -> data);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
            return CAPABILITY.orEmpty(capability, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }
    }

    @Mod.EventBusSubscriber(modid = ZitraksMode.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class AttachHandler {
        private AttachHandler() {
        }

        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof net.minecraft.world.entity.player.Player) {
                event.addCapability(ID, new Provider());
            }
        }
    }
}
