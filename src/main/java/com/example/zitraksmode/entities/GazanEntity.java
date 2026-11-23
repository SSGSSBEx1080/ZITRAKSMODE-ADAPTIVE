package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModItems;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode; // ✅ ИМПОРТ ДЛЯ ТРИГГЕРА
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import java.util.Random;

public class GazanEntity extends Villager {
    private int speechIndex = 0;
    private long lastSpeechTime = 0;
    private boolean tradesInitialized = false;

    public GazanEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("Газан"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.5D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    // ======================================================
    //  ТОРГОВЛЯ
    // ============================================================

    private void buildTrades() {
        MerchantOffers offers = new MerchantOffers();
        Random rand = new Random();

        // --- ПРОДАЖА блоков 6 и 7 ---
        offers.add(new MerchantOffer(
            new ItemStack(Items.EMERALD, 16),
            ItemStack.EMPTY,
            new ItemStack(ModItems.SIX_BLOCK_ITEM.get(), 1),
            9999, 1, 0.0F));

        offers.add(new MerchantOffer(
            new ItemStack(Items.EMERALD, 16),
            ItemStack.EMPTY,
            new ItemStack(ModItems.SEVEN_BLOCK_ITEM.get(), 1),
            9999, 1, 0.0F));

        // --- ПОКУПКА пластинок (рандом 16-32 изумрудов) ---
        Item[] discs = {
            Items.MUSIC_DISC_11,    Items.MUSIC_DISC_13,
            Items.MUSIC_DISC_CAT,   Items.MUSIC_DISC_BLOCKS,
            Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_FAR,
            Items.MUSIC_DISC_MALL,  Items.MUSIC_DISC_MELLOHI,
            Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_PIGSTEP,
            Items.MUSIC_DISC_STAL,  Items.MUSIC_DISC_STRAD,
            Items.MUSIC_DISC_WAIT,  Items.MUSIC_DISC_5
        };
 
        for (Item disc : discs) {
            int price = 16 + rand.nextInt(17); // 16..32 включительно
            offers.add(new MerchantOffer(
                new ItemStack(disc, 1),
                 ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, price),
                9999, 1, 0.0F));
        }

        this.setOffers(offers);
        tradesInitialized = true;
    }

    @Override
    protected void updateTrades() {
        buildTrades();
    }

    // ============================================================
    //  ЗВУКИ
    // ===================================================== =======

    @Override
    protected SoundEvent getAmbientSound() {
        long gameTime = level.getGameTime();
        
        // Определяем задержку перед следующей фразой
        long currentCooldown = (speechIndex == 5) ? 800L : 400L; 

        if (gameTime - lastSpeechTime < currentCooldown) return null;
        
        lastSpeechTime = gameTime;

        SoundEvent[] speeches = {
            ModSounds.GAZAN_SPEECH_1.get(),  ModSounds.GAZAN_SPEECH_2.get(),
             ModSounds.GAZAN_SPEECH_3.get(),  ModSounds.GAZAN_SPEECH_4.get(),
            ModSounds.GAZAN_SPEECH_5.get(),  ModSounds.GAZAN_SPEECH_6.get(),
            ModSounds.GAZAN_SPEECH_7.get(),  ModSounds.GAZAN_SPEECH_8.get(),
            ModSounds.GAZAN_SPEECH_9.get(),  ModSounds.GAZAN_SPEECH_10.get()
        };

        SoundEvent chosen = speeches[speechIndex % speeches.length];
        speechIndex = (speechIndex + 1) % speeches.length;
        return chosen;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return random.nextBoolean() ? ModSounds.GAZAN_HURT_1.get() : ModSounds.GAZAN_HURT_2.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GAZAN_DEATH.get();
    }

    /** 
     * Звук при ЗАВЕРШЕНИИ сделки (игрок кликнул на товар и забрал)
     */
    @Override
    public void notifyTrade(MerchantOffer offer) {
        SoundEvent tradeSound = random.nextBoolean()
            ? ModSounds.GAZAN_TRADE_1.get()
            : ModSounds.GAZAN_TRADE_2.get();
        this.playSound(tradeSound, 1.0F, 1.0F);
        
        // ✅ АЧИВКА ЗА ТОРГОВЛЮ (100% рабочий вариант для 1.19.2)
        net.minecraft.world.entity.player.Player trader = this.getTradingPlayer();
        if (trader instanceof net.minecraft.server.level.ServerPlayer sp) {
            ZitraksMode.GAZAN_TRADE.trigger(sp);
        }
        
        super.notifyTrade(offer);
    }

    /**
     * Звук "Хммм" — когда игрок ОТКРЫВАЕТ окно торговли.
     */
    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.GAZAN_TRADE_OPEN.get();
    }

    /**
     * Звук при клике "да/нет" на предложение торговли.
     */
    @Override
    public SoundEvent getTradeUpdatedSound(boolean isYesSound) {
        if (isYesSound) {
            return ModSounds.GAZAN_YES.get();
        } else {
            return ModSounds.GAZAN_NO.get();
        }
    }

    // ============================================================
    //  ОТКРЫТИЕ ТОРГОВЛИ — кастомный звук при ПКМ
    // ============== ==============================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Блокируем размножение
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.GOLDEN_CARROT)) {
            return InteractionResult.PASS;
        }

        // Играем кастомный звук при открытии торговли
        if (!this.level.isClientSide && !this.isBaby()) {
            this.playSound(ModSounds.GAZAN_TRADE_OPEN.get(), 1.0F, 1.0F);
        }

        return super.mobInteract(player, hand);
    }

    // ============================================================
    //  TICK + NBT
    // ============================================================

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide && !tradesInitialized) {
            buildTrades();
        }

        resetLove();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GazanTradesInit", tradesInitialized);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tradesInitialized = tag.getBoolean("GazanTradesInit");
        buildTrades(); // Пересобираем при загрузке (с новым рандомом!)
    }

    // ============================================================
    //  ПРОЧЕЕ
    // =========================== =================================

    @Override
    public void setVillagerData(VillagerData data) {}

    @Override
    public boolean canBeSeenAsEnemy() { return false; }

    @Override
    protected void customServerAiStep() {}

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {}

    @Override
    public boolean canBreed() { return false; }

    private void resetLove() {
        try {
            java.lang.reflect.Field f = Villager.class.getDeclaredField("love");
            f.setAccessible(true);
            f.setInt(this, 0);
        } catch (Exception ignored) {}
        try {
            Villager.class.getDeclaredMethod("setInLoveTime", int.class).invoke(this, 0);
        } catch (Exception ignored) {}
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.NETHERITE_DILDO.get()));
    }
}