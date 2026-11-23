package com.example.zitraksmode.entities;

import com.example.zitraksmode.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

public class AdunVillager extends Villager {

    public AdunVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();

        // Сразу фиксируем: без профессии, 1 уровень
        VillagerData data = super.getVillagerData();
        super.setVillagerData(new VillagerData(data.getType(), VillagerProfession.NONE, 1));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        // 1. Плавание
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 2. Торговля с игроком
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));

        // 3. Случайное перемещение
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.7D));

        // 4. Смотреть на игрока
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // 5. Смотреть по сторонам
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    // --- ЗВУКИ ---

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ADUN_AMBIENT.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.ADUN_TRADE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.ADUN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ADUN_DEATH.get();
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        this.playSound(ModSounds.ADUN_TRADE.get(), 1.0F, 1.0F);
        super.notifyTrade(offer);
    }

    // --- ТОРГОВЛЯ ---

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();

        if (offers == null) {
            offers = new MerchantOffers();
        } else {
            offers.clear();
        }

        // 52 земли -> 1 изумруд, только 3 раза
        offers.add(new MerchantOffer(
                new ItemStack(Items.DIRT, 52),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                3,      // maxUses
                0,      // villager XP = 0, НЕ учится
                0.0f
        ));

        // 1 изумруд -> 1 земля, практически бесконечно
        offers.add(new MerchantOffer(
                new ItemStack(Items.EMERALD, 1),
                ItemStack.EMPTY,
                new ItemStack(Items.DIRT, 1),
                999999, // maxUses
                0,      // villager XP = 0
                0.0f
        ));

        this.setOffers(offers);
    }

    // --- СПАВН ---

    public static boolean canSpawn(EntityType<AdunVillager> type, LevelAccessor world,
                                   MobSpawnType reason, BlockPos pos, RandomSource random) {
        // Ручной спавн не режем
        if (reason == MobSpawnType.SPAWN_EGG ||
            reason == MobSpawnType.COMMAND ||
            reason == MobSpawnType.DISPENSER ||
            reason == MobSpawnType.EVENT) {
            return true;
        }

        // В 10 раз реже натуральный спавн
        if (random.nextInt(10) != 0) {
            return false;
        }

        if (!Mob.checkMobSpawnRules(type, world, reason, pos, random)) {
            return false;
        }

        // Не больше одного адуна в радиусе 64 блоков
        return world.getEntitiesOfClass(AdunVillager.class, new AABB(pos).inflate(64)).isEmpty();
    }

    // --- ОТКЛЮЧЕНИЕ СТАНДАРТНОГО ПОВЕДЕНИЯ ---

    @Override
    public void setVillagerData(VillagerData data) {
        // Что бы ему ни пытались присвоить — он всегда NONE lvl 1
        VillagerData current = super.getVillagerData();
        super.setVillagerData(new VillagerData(
                data != null ? data.getType() : current.getType(),
                VillagerProfession.NONE,
                1
        ));
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        // Дополнительная страховка: не дать стать кем-то кроме NONE lvl 1
        VillagerData data = super.getVillagerData();
        if (data.getProfession() != VillagerProfession.NONE || data.getLevel() != 1) {
            super.setVillagerData(new VillagerData(data.getType(), VillagerProfession.NONE, 1));
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // Не даём опыт за трейды
    }

    // --- ОТКЛЮЧЕНИЕ РАЗМНОЖЕНИЯ ---

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // Сбрасываем "любовь", если вдруг что-то её включило
        resetLove();
    }

    private void resetLove() {
        try {
            java.lang.reflect.Field loveTimeField = Villager.class.getDeclaredField("love");
            loveTimeField.setAccessible(true);
            loveTimeField.setInt(this, 0);
        } catch (Exception ignored) {
        }

        setInLoveTime(0);
    }

    private void setInLoveTime(int time) {
        try {
            Villager.class.getDeclaredMethod("setInLoveTime", int.class).invoke(this, time);
        } catch (Exception ignored) {
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Игнорируем предметы, которыми обычно запускают размножение
        if (itemstack.is(Items.GOLDEN_APPLE) || itemstack.is(Items.GOLDEN_CARROT)) {
            return InteractionResult.PASS;
        }

        return super.mobInteract(player, hand);
    }
}