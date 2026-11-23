package com.example.zitraksmode.items;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.entities.BulletEntity;
import com.example.zitraksmode.weapons.ModToolTiers;
import com.example.zitraksmode.weapons.SniperUltimateAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class SniperItem extends Item {

    private final Tier tier;

    private static final int COOLDOWN_IRON = 40;
    private static final int COOLDOWN_GOLD = 30;
    private static final int COOLDOWN_DIAMOND = 50;
    private static final int COOLDOWN_NETHERITE = 60;

    private static final float DAMAGE_IRON = 20.0f;
    private static final float DAMAGE_GOLD = 35.0f;
    private static final float DAMAGE_DIAMOND = 45.0f;
    private static final float DAMAGE_NETHERITE = 100.0f;

    private static final float BULLET_SPEED = 8.0F;
    private static final double PIERCE_CHECK_DISTANCE = 200.0D;

    public SniperItem(Tier tier, Properties properties) {
        super(properties.defaultDurability(tier.getUses()));
        this.tier = tier;
    }

    public Tier getTier() {
        return this.tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            if (!level.isClientSide) {
                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
            return InteractionResultHolder.fail(itemstack);
        }

        if (!hasAmmo(player)) {
            if (!level.isClientSide) {
                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
            return InteractionResultHolder.fail(itemstack);
        }

        player.startUsingItem(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(itemstack);
        }

        int doublePenetration = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DOUBLE_PENETRATION.get(), itemstack);
        int pierced = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.HOW_HE_PIERCED_ME.get(), itemstack);

        float shotDamage = getBulletDamage();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 eyePos = player.getEyePosition(1.0F);

        Vec3 spawnPos = pierced > 0
                ? computePiercedSpawnPosition(level, player, eyePos, direction)
                : eyePos;

        if (doublePenetration > 0) {
            float splitDamage = shotDamage * 0.60f;

            fireBullet(level, player, spawnPos, direction, splitDamage, BULLET_SPEED, false, 0);

            MinecraftServer server = level.getServer();
            if (server != null) {
                UUID shooterId = player.getUUID();
                Vec3 savedDirection = direction;
                Vec3 savedSpawn = spawnPos;
                server.tell(new TickTask(server.getTickCount() + 2, () -> {
                    Player shooter = server.getPlayerList().getPlayer(shooterId);
                    if (shooter == null || !shooter.isAlive()) {
                        return;
                    }

                    fireBullet(shooter.level, shooter, savedSpawn, savedDirection, splitDamage, BULLET_SPEED, false, 0);
                }));
            }
        } else {
            fireBullet(level, player, spawnPos, direction, shotDamage, BULLET_SPEED, false, 0);
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.SNIPER_SHOT.get(),
                SoundSource.PLAYERS,
                4.0F,
                0.8F
        );

        player.getCooldowns().addCooldown(this, applyReloadReduction(getBaseCooldownTicks(), itemstack));
        consumeAmmo(player);

        return InteractionResultHolder.consume(itemstack);
    }

    private Vec3 computePiercedSpawnPosition(Level level, Player player, Vec3 eyePos, Vec3 direction) {
        Vec3 traceEnd = eyePos.add(direction.scale(PIERCE_CHECK_DISTANCE));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos,
                traceEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return eyePos;
        }

        Vec3 endForEntityCheck = blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level,
                player,
                eyePos,
                endForEntityCheck,
                new AABB(eyePos, endForEntityCheck).inflate(1.0D),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != player
        );

        // Если живая цель ДО блока — стреляем как обычно, не перескакиваем блок
        if (entityHit != null) {
            return eyePos;
        }

        BlockState hitState = level.getBlockState(blockHit.getBlockPos());
        if (!canShootThrough(hitState)) {
            return eyePos;
        }

        return getPositionBehindSingleBlock(blockHit, direction);
    }

    private Vec3 getPositionBehindSingleBlock(BlockHitResult hit, Vec3 direction) {
        BlockPos hitPos = hit.getBlockPos();
        Vec3 pos = hit.getLocation();

        // Выходим из текущего блока шагами по 1 пикселю
        for (int i = 0; i < 64; i++) {
            pos = pos.add(direction.scale(0.0625D));
            if (!new BlockPos(pos).equals(hitPos)) {
                break;
            }
        }

        // Ещё немного вперёд, чтобы точно не липло к грани
        // ~ 1 пиксель + маленький запас
        return pos.add(direction.scale(0.08D));
    }

    private boolean canShootThrough(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.REINFORCED_DEEPSLATE)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)) {
            return false;
        }

        return true;
    }

    private void fireBullet(Level level, Player player, Vec3 eyePos, Vec3 direction, float damage, float speed, boolean onFire, int fireSeconds) {
        BulletEntity bullet = new BulletEntity(level, player);
        bullet.setBulletDamage(damage);
        bullet.pickup = AbstractArrow.Pickup.DISALLOWED;
        bullet.setNoGravity(true);

        if (onFire && fireSeconds > 0) {
            bullet.setSecondsOnFire(fireSeconds);
        }

        bullet.setPos(eyePos.x, eyePos.y, eyePos.z);
        bullet.xo = eyePos.x;
        bullet.yo = eyePos.y;
        bullet.zo = eyePos.z;

        bullet.shoot(direction.x, direction.y, direction.z, speed, 0.0F);

        bullet.yRotO = bullet.getYRot();
        bullet.xRotO = bullet.getXRot();

        level.addFreshEntity(bullet);
    }

    public void activateUltimate(Player player, InteractionHand hand) {
        Level level = player.level;
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return;
        }

        if (this.tier != ModToolTiers.SNIPER_NETHERITE) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            return;
        }

        if (!hasUltimateAmmo(player)) {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            return;
        }

        startUltimateAnimation(player, stack);
    }

    private boolean hasUltimateAmmo(Player player) {
        if (player.getAbilities().instabuild) return true;

        int totalBullets = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BulletItem) {
                totalBullets += stack.getCount();
                if (totalBullets >= 64) return true;
            }
        }
        return false;
    }

    private void consumeUltimateAmmo(Player player) {
        if (player.getAbilities().instabuild) return;

        int bulletsNeeded = 64;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BulletItem) {
                int stackCount = stack.getCount();
                if (stackCount >= bulletsNeeded) {
                    stack.shrink(bulletsNeeded);
                    break;
                } else {
                    bulletsNeeded -= stackCount;
                    stack.shrink(stackCount);
                }
            }
        }
    }

    private void startUltimateAnimation(Player player, ItemStack stack) {
        Level level = player.level;

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.SNIPER_ULTIMATE_CHARGE.get(),
                SoundSource.PLAYERS,
                2.0F,
                1.0F
        );

        performUltimateShot(player, stack);
    }

    private void performUltimateShot(Player player, ItemStack stack) {
        Level level = player.level;

        SniperUltimateAbility.fire(player, getBulletDamage());

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.SNIPER_ULTIMATE_SHOT.get(),
                SoundSource.PLAYERS,
                6.0F,
                0.7F
        );

        player.getCooldowns().addCooldown(this, applyReloadReduction(600, stack));
        consumeUltimateAmmo(player);
    }

    private boolean hasAmmo(Player player) {
        if (player.getAbilities().instabuild) return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BulletItem && !stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void consumeAmmo(Player player) {
        if (player.getAbilities().instabuild) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BulletItem && !stack.isEmpty()) {
                stack.shrink(1);
                break;
            }
        }
    }

    private float getBulletDamage() {
        if (this.tier == ModToolTiers.SNIPER_IRON) return DAMAGE_IRON;
        else if (this.tier == ModToolTiers.SNIPER_GOLD) return DAMAGE_GOLD;
        else if (this.tier == ModToolTiers.SNIPER_DIAMOND) return DAMAGE_DIAMOND;
        else if (this.tier == ModToolTiers.SNIPER_NETHERITE) return DAMAGE_NETHERITE;
        else return DAMAGE_IRON;
    }

    private int getBaseCooldownTicks() {
        if (this.tier == ModToolTiers.SNIPER_IRON) return COOLDOWN_IRON;
        else if (this.tier == ModToolTiers.SNIPER_GOLD) return COOLDOWN_GOLD;
        else if (this.tier == ModToolTiers.SNIPER_DIAMOND) return COOLDOWN_DIAMOND;
        else if (this.tier == ModToolTiers.SNIPER_NETHERITE) return COOLDOWN_NETHERITE;
        else return COOLDOWN_IRON;
    }

    private int applyReloadReduction(int baseCooldown, ItemStack stack) {
        int lvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.NIMBLE_FINGERS.get(), stack);
        float multiplier = switch (lvl) {
            case 1 -> 0.95f;
            case 2 -> 0.90f;
            case 3 -> 0.85f;
            default -> 1.0f;
        };
        return Math.max(1, Math.round(baseCooldown * multiplier));
    }

    @Override
    public int getEnchantmentValue() {
        return this.tier.getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return this.tier.getRepairIngredient().test(repair);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }
}