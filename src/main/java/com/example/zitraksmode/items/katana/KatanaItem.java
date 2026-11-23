package com.example.zitraksmode.items.katana;

import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.client.katana.KatanaRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.slf4j.Logger;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public class KatanaItem extends SwordItem implements IAnimatable {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    public static final int ULTIMATE_DURABILITY_COST = 67;

    private final AnimationFactory factory = new AnimationFactory(this);

    private static final List<String> COMBO_ANIMS = Arrays.asList(
            "diagonal_left", "diagonal_right", "swing_horizontal"
    );

    private static final Map<UUID, String> CURRENT_ACTION = new HashMap<>();
    private static final Map<UUID, Boolean> IS_ULTIMATING = new HashMap<>();
    private static final Map<UUID, Integer> LAST_COMBO_INDEX = new HashMap<>();

    public KatanaItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            triggerComboAnimation(player);
        }
        return true;
    }

    private void triggerComboAnimation(Player player) {
        UUID uuid = player.getUUID();
        int last = LAST_COMBO_INDEX.getOrDefault(uuid, -1);
        int next;

        do {
            next = RANDOM.nextInt(COMBO_ANIMS.size());
        } while (next == last && COMBO_ANIMS.size() > 1);

        LAST_COMBO_INDEX.put(uuid, next);
        String anim = COMBO_ANIMS.get(next);

        CURRENT_ACTION.put(uuid, anim);
        LOGGER.info("⚔️ COMBO → {}", anim);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && !player.level.isClientSide) {
            SlashAttackEntity slash = new SlashAttackEntity(player.level, player, this.getDamage());
            player.level.addFreshEntity(slash);
            playRandomSlashSound(player.level, player);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            return startUltimate(level, player, stack, hand);
        }

        if (ParryManager.canParry(player)) {
            ParryManager.startParry(player);
            CURRENT_ACTION.put(player.getUUID(), "parry");
            LOGGER.info("🛡️ PARRY WINDOW START");
        }

        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> startUltimate(Level level, Player player, ItemStack stack, InteractionHand hand) {
        if (!KatanaCooldownManager.isUltimateReady(player)) {
            return InteractionResultHolder.fail(stack);
        }

        int remainingDurability = stack.getMaxDamage() - stack.getDamageValue();
        if (remainingDurability < ULTIMATE_DURABILITY_COST) {
            return InteractionResultHolder.fail(stack);
        }

        UUID uuid = player.getUUID();
        CURRENT_ACTION.put(uuid, "ultimate_charge");
        IS_ULTIMATING.put(uuid, true);

        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), ModSounds.KATANA_CHARGE.get(), SoundSource.PLAYERS, 2.5f, 0.8f);
        }

        player.startUsingItem(hand);
        LOGGER.info("🔥 ULTIMATE CHARGING...");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player)) return;

        UUID uuid = player.getUUID();

        if ((this.getUseDuration(stack) - timeLeft) >= 20) {
            CURRENT_ACTION.put(uuid, "ultimate_release");
            LOGGER.info("⚡ ULTIMATE RELEASE");

            if (!level.isClientSide) {
                Vec3 dir = player.getLookAngle();

                int worldSlashLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.WORLD_SLASH.get(), stack);
                boolean miningIndustry = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.MINING_INDUSTRY.get(), stack) > 0;

                level.addFreshEntity(new VerticalSlashEntity(level, player, dir, worldSlashLevel, miningIndustry));
                ZitraksMode.KATANA_ULTIMATE.trigger((ServerPlayer) player);

                stack.hurtAndBreak(ULTIMATE_DURABILITY_COST, player,
                        p -> p.broadcastBreakEvent(player.getUsedItemHand()));

                KatanaCooldownManager.setUltimateCooldown(player);
                level.playSound(null, player.blockPosition(), ModSounds.KATANA_CHARGED_SLASH.get(), SoundSource.PLAYERS, 2.0f, 1.0f);
            }
        } else {
            CURRENT_ACTION.remove(uuid);
        }

        IS_ULTIMATING.remove(uuid);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "idle_controller", 0, event -> {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }));

        data.addAnimationController(new AnimationController<>(this, "action_controller", 0, this::actionPredicate));
    }

    private PlayState actionPredicate(AnimationEvent<KatanaItem> event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;

        UUID uuid = player.getUUID();
        String action = CURRENT_ACTION.getOrDefault(uuid, "");

        if (action.isEmpty()) return PlayState.STOP;

        AnimationBuilder builder = new AnimationBuilder();

        if (Boolean.TRUE.equals(IS_ULTIMATING.get(uuid))) {
            builder.addAnimation("ultimate_charge", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME);
        } else {
            builder.addAnimation(action, ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }

        event.getController().setAnimation(builder);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    private void playRandomSlashSound(Level level, Player player) {
        var sound = switch (RANDOM.nextInt(3)) {
            case 0 -> ModSounds.KATANA_SLASH_1.get();
            case 1 -> ModSounds.KATANA_SLASH_2.get();
            default -> ModSounds.KATANA_SLASH_3.get();
        };
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private KatanaRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new KatanaRenderer();
                return renderer;
            }
        });
    }
}