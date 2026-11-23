package com.example.zitraksmode.items;

import com.example.zitraksmode.client.renderer.DeathNoteRenderer;
import com.example.zitraksmode.network.ModMessages;
import com.example.zitraksmode.network.OpenDeathNotePacket;
import com.example.zitraksmode.util.DeathNoteHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class DeathNoteItem extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    private static final Map<UUID, String> CURRENT_ANIM = new HashMap<>();
    private static final Map<UUID, Boolean> ANIM_LOCK = new HashMap<>();
    private static final Map<UUID, Integer> ANIM_COUNTER = new HashMap<>();
    private final Map<UUID, Integer> lastProcessedCounter = new HashMap<>();

    public DeathNoteItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (DeathNoteHelper.isAnyDeathNoteActive(player)) {
                player.sendSystemMessage(Component.literal("§cТетрадь уже активна, дождись исполнения казни."));
                return InteractionResultHolder.fail(stack);
            }

            setAnim(player, "open", true);
            ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenDeathNotePacket(stack, false));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void setAnim(Player player, String anim, boolean lock) {
        UUID uuid = player.getUUID();
        CURRENT_ANIM.put(uuid, anim);
        ANIM_COUNTER.put(uuid, ANIM_COUNTER.getOrDefault(uuid, 0) + 1);
        if (lock) ANIM_LOCK.put(uuid, true);
        else ANIM_LOCK.remove(uuid);
    }

    public static void clearAnim(Player player) {
        UUID uuid = player.getUUID();
        CURRENT_ANIM.remove(uuid);
        ANIM_LOCK.remove(uuid);
        ANIM_COUNTER.remove(uuid);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return stack.getOrCreateTag().getInt("maxUses");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (stack.hasTag()) {
            int kills = stack.getTag().getInt("kills");
            tooltip.add(Component.literal("Убийств: " + kills).withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "idle_controller", 0, event -> {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }));
        data.addAnimationController(new AnimationController<>(this, "action_controller", 0, this::actionPredicate));
    }

    private PlayState actionPredicate(AnimationEvent<DeathNoteItem> event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        UUID uuid = player.getUUID();
        String anim = CURRENT_ANIM.get(uuid);
        if (anim == null || anim.isEmpty()) return PlayState.STOP;

        int currentCounter = ANIM_COUNTER.getOrDefault(uuid, 0);
        int lastCounter = lastProcessedCounter.getOrDefault(uuid, -1);
        if (currentCounter == lastCounter && ANIM_LOCK.getOrDefault(uuid, false)) {
            return PlayState.CONTINUE;
        }
        lastProcessedCounter.put(uuid, currentCounter);

        AnimationBuilder builder = new AnimationBuilder();
        if (anim.equals("open")) {
            builder.addAnimation("open", ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME);
        } else if (anim.equals("close")) {
            builder.addAnimation("close", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        event.getController().setAnimation(builder);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private DeathNoteRenderer renderer;
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new DeathNoteRenderer();
                return renderer;
            }
        });
    }
}