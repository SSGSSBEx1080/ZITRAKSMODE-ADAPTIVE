package com.example.zitraksmode.events;

import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.client.IntimacyVisuals;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "zitraksmode")
public final class IntimacyManager {
    private static final Map<UUID, Invitation> INVITATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> REFRACTORY_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COMMAND_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Session> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private static final int RADIUS = 2;
    private static final int DURATION_TICKS = 200;
    private static final int REFRACTORY_SECONDS = 60;
    private static final int COMMAND_COOLDOWN_SECONDS = 5;
    private static final int INVITATION_TIMEOUT_SECONDS = 30;
    private static final int ANIMATION_FREQUENCY = 5; // 5 колебаний в секунду

    private IntimacyManager() {}

    public static void sendInvitation(Player sender, Entity target, boolean isBlowjob) {
        if (isOnRefractoryCooldown(sender)) {
            sender.displayClientMessage(Component.literal(
                    "Ты ещё не отошёл после прошлого раза. Подожди ещё " + getCooldownSeconds(sender) + " сек."
            ), false);
            return;
        }

        if (isOnCommandCooldown(sender)) {
            sender.displayClientMessage(Component.literal(
                    "Подожди 5 секунд перед следующим приглашением."
            ), false);
            return;
        }

        if (sender.distanceTo(target) > RADIUS) {
            sender.displayClientMessage(Component.literal(
                    "Слишком далеко. Подойди ближе."
            ), false);
            return;
        }

        // Проверяем рефракторный период для игроков
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (isOnRefractoryCooldown(targetPlayer)) {
                sender.displayClientMessage(Component.literal(
                        "Этот игрок сейчас не может принять приглашение."
                ), false);
                return;
            }
        }

        // Если цель - моб, сразу начинаем сцену
        if (!(target instanceof Player)) {
            String targetName = getEntityName(target);
            
            sender.displayClientMessage(Component.literal(
                    "Ты начинаешь ебаться с " + targetName + "!"
            ), false);
            
            Invitation inv = new Invitation(
                    sender.getUUID(),
                    target.getUUID(),
                    false,
                    isBlowjob,
                    System.currentTimeMillis() / 1000 + INVITATION_TIMEOUT_SECONDS
            );
            
            startSession(sender, target, inv);
            setCommandCooldown(sender);
            return;
        }

        // Для игроков - отправляем приглашение
        INVITATIONS.values().removeIf(inv -> inv.getSender().equals(sender.getUUID()));

        Invitation inv = new Invitation(
                sender.getUUID(),
                target.getUUID(),
                true,
                isBlowjob,
                System.currentTimeMillis() / 1000 + INVITATION_TIMEOUT_SECONDS
        );
        INVITATIONS.put(target.getUUID(), inv);

        String targetName = target.getName().getString();
        ((ServerPlayer) target).displayClientMessage(Component.literal(
                sender.getName().getString() + " хочет заняться близостью. Введи /yes или /no"
        ), false);
        sender.displayClientMessage(Component.literal("Приглашение отправлено " + targetName), false);

        setCommandCooldown(sender);
    }

    public static void acceptInvitation(Player player) {
        if (isOnRefractoryCooldown(player)) {
            player.displayClientMessage(Component.literal(
                    "Ты ещё не отошёл после прошлого раза. Подожди ещё " + getCooldownSeconds(player) + " сек."
            ), false);
            return;
        }

        Invitation inv = INVITATIONS.remove(player.getUUID());
        if (inv == null) {
            player.displayClientMessage(Component.literal("У тебя нет активных приглашений."), false);
            return;
        }

        if (System.currentTimeMillis() / 1000 > inv.getExpiryTime()) {
            player.displayClientMessage(Component.literal("Приглашение истекло."), false);
            return;
        }

        Player sender = player.getServer().getPlayerList().getPlayer(inv.getSender());
        if (sender == null) {
            player.displayClientMessage(Component.literal("Отправитель вышел из игры."), false);
            return;
        }

        if (player.distanceTo(sender) > 3.0) {
            player.displayClientMessage(Component.literal("Партнёр отошёл слишком далеко."), false);
            return;
        }

        if (isOnRefractoryCooldown(sender)) {
            player.displayClientMessage(Component.literal("Партнёр ещё не отошёл после прошлого раза."), false);
            return;
        }

        startSession(sender, player, inv);
    }

    public static void declineInvitation(Player player) {
        Invitation inv = INVITATIONS.remove(player.getUUID());
        if (inv == null) {
            player.displayClientMessage(Component.literal("У тебя нет активных приглашений."), false);
            return;
        }

        Player sender = player.getServer().getPlayerList().getPlayer(inv.getSender());
        if (sender != null) {
            sender.displayClientMessage(Component.literal(
                    player.getName().getString() + " отказался."
            ), false);
        }
        player.displayClientMessage(Component.literal("Ты отказался."), false);
        IntimacyAchievements.unlockRejected(player);
    }

    private static void startSession(Player sender, Entity receiver, Invitation inv) {
    // Создаем сессию
    Session session = new Session(sender, receiver, inv, System.currentTimeMillis() + 10000);
    ACTIVE_SESSIONS.put(sender.getUUID(), session);
    if (receiver instanceof Player) {
        ACTIVE_SESSIONS.put(receiver.getUUID(), session);
    }

    // Эффекты обездвижки для отправителя
    sender.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 255, false, false));
    sender.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION_TICKS, 0, false, false));
    sender.setInvulnerable(true);

    // Если получатель - игрок
    if (receiver instanceof Player) {
        Player receiverPlayer = (Player) receiver;
        receiverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 255, false, false));
        receiverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION_TICKS, 0, false, false));
        receiverPlayer.setInvulnerable(true);

        // Для blowjob принимающий приседает
        if (inv.isBlowjob()) {
            receiverPlayer.setPose(Pose.CROUCHING);
        }
    } else {
        // Если получатель - моб
        if (receiver instanceof Animal) {
            Animal animal = (Animal) receiver;
            animal.setNoAi(true);
            animal.getNavigation().stop();
        } else if (receiver instanceof AbstractVillager) {
            AbstractVillager villager = (AbstractVillager) receiver;
            villager.setNoAi(true);
            villager.getNavigation().stop();
            villager.playSound(SoundEvents.VILLAGER_TRADE, 0.5F, 1.5F);
        }
    }

    // Визуал
    IntimacyVisuals.spawnHeartParticles(sender, 30);
    IntimacyVisuals.spawnHeartParticles(receiver, 30);
    IntimacyVisuals.spawnSparkParticles(sender, 15);
    IntimacyVisuals.spawnSparkParticles(receiver, 15);

    // Звук
    if (inv.isBlowjob()) {
        sender.level.playSound(null, sender.blockPosition(), ModSounds.BLOWJOB_SOUND.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        receiver.level.playSound(null, receiver.blockPosition(), ModSounds.BLOWJOB_SOUND.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
    } else {
        sender.level.playSound(null, sender.blockPosition(), ModSounds.INTIMATE_SOUND.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        receiver.level.playSound(null, receiver.blockPosition(), ModSounds.INTIMATE_SOUND.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
    }

    // Сообщения - разные для секса и бловджоба
    String receiverName = getEntityName(receiver);
    
    if (inv.isBlowjob()) {
        // Сообщения для бловджоба
        if (receiver instanceof Player) {
            Player receiverPlayer = (Player) receiver;
            // Отправитель получает минет
            sender.displayClientMessage(Component.literal(
                    "Вам отсасывает хуй игрок " + receiverName
            ), false);
            // Получатель делает минет
            receiverPlayer.displayClientMessage(Component.literal(
                    "Вы отсасываете хуй у игрока " + sender.getName().getString()
            ), false);
        } else {
            // С мобом
            sender.displayClientMessage(Component.literal(
                    "Вам отсасывает хуй " + receiverName
            ), false);
        }
    } else {
        // Сообщения для обычного секса
        sender.displayClientMessage(Component.literal(
                "Вы ебёте " + receiverName
        ), false);
        
        if (receiver instanceof Player) {
            Player receiverPlayer = (Player) receiver;
            receiverPlayer.displayClientMessage(Component.literal(
                    "Вас ебёт игрок " + sender.getName().getString()
            ), false);
        }
    }

    // Ачивки
    if (receiver instanceof Player) {
        IntimacyAchievements.unlockFirstTime(sender, (Player) receiver);
        IntimacyAchievements.registerSex(sender, (Player) receiver);
        if (inv.isBlowjob()) {
            IntimacyAchievements.unlockSubmitted((Player) receiver);
        }
        IntimacyAchievements.unlockTrusting((Player) receiver);
    }

    if (!inv.isPlayerTarget()) {
        if (receiver instanceof Animal) {
            IntimacyAchievements.unlockAnimalLover(sender, (Animal) receiver);
            IntimacyAchievements.incrementSexCount(sender);
            IntimacyAchievements.unlockGaveUp(sender);
        } else if (receiver instanceof AbstractVillager) {
            IntimacyAchievements.unlockDiplomat(sender);
            IntimacyAchievements.incrementSexCount(sender);
        }
    }

    // Рефракторный период
    setRefractoryCooldown(sender);
    if (receiver instanceof Player) {
        setRefractoryCooldown((Player) receiver);
    }

    // Таймер окончания
    new Thread(() -> {
        try {
            Thread.sleep(10000);
            sender.getServer().execute(() -> {
                ACTIVE_SESSIONS.remove(sender.getUUID());
                if (receiver instanceof Player) {
                    ACTIVE_SESSIONS.remove(receiver.getUUID());
                }

                sender.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                sender.removeEffect(MobEffects.BLINDNESS);
                sender.setInvulnerable(false);

                if (receiver instanceof Player) {
                    Player receiverPlayer = (Player) receiver;
                    receiverPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    receiverPlayer.removeEffect(MobEffects.BLINDNESS);
                    receiverPlayer.setInvulnerable(false);
                    if (inv.isBlowjob()) {
                        receiverPlayer.setPose(Pose.STANDING);
                    }
                } else if (receiver instanceof Animal) {
                    Animal animal = (Animal) receiver;
                    animal.setNoAi(false);
                    animal.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                } else if (receiver instanceof AbstractVillager) {
                    AbstractVillager villager = (AbstractVillager) receiver;
                    villager.setNoAi(false);
                    villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                    IntimacyAchievements.unlockCaring(sender);
                }

                // Сообщения об окончании
                if (inv.isBlowjob()) {
                    sender.displayClientMessage(Component.literal("Вы закончили получать минет."), false);
                    if (receiver instanceof Player) {
                        ((ServerPlayer) receiver).displayClientMessage(Component.literal("Вы закончили отсасывать."), false);
                    }
                } else {
                    sender.displayClientMessage(Component.literal("Вы закончили ебаться."), false);
                    if (receiver instanceof Player) {
                        ((ServerPlayer) receiver).displayClientMessage(Component.literal("Вас закончили ебать."), false);
                    }
                }

                IntimacyVisuals.spawnFireworks(sender);
                IntimacyVisuals.spawnFireworks(receiver);
            });
        } catch (InterruptedException ignored) {}
    }).start();
}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session != null && session.isActive()) {
            // Полная блокировка управления
            player.setDeltaMovement(0, 0, 0);
            player.xxa = 0;
            player.zza = 0;
            player.setJumping(false);
            player.setSprinting(false);
            
            // Анимация движения
            animateSession(session, player);
        }
        
        if (isOnRefractoryCooldown(player)) {
            int seconds = getCooldownSeconds(player);
            player.displayClientMessage(Component.literal("⏳ Рефракторный период: " + seconds + " сек"), true);
        }
    }

    private static void animateSession(Session session, Player player) {
        long currentTime = System.currentTimeMillis();
        double frequency = ANIMATION_FREQUENCY * 2 * Math.PI; // 5 колебаний в секунду
        double amplitude = 0.3; // Амплитуда движения
        
        double offset = Math.sin((currentTime / 1000.0) * frequency) * amplitude;
        
        Entity mover;
        Entity target;
        
        if (session.isBlowjob()) {
            // При бловджобе моб повернут лицом и делает движения головой
            if (session.receiver instanceof Player) {
                // Между игроками: один стоит, другой приседает и двигает головой
                Player receiver = (Player) session.receiver;
                Player sender = (Player) session.sender;
                
                // Приседающий двигает головой вперед-назад
                receiver.setXRot((float) (receiver.getXRot() + Math.sin((currentTime / 1000.0) * frequency) * 20));
                
                // Стоящий двигается вперед-назад
                sender.setPos(sender.getX(), sender.getY(), sender.getZ() + offset);
            } else {
                // С мобом: моб повернут лицом к игроку
                Entity mob = session.receiver;
                Entity senderEntity = session.sender;
                
                // Разворачиваем моба лицом к игроку
                faceEntity(mob, senderEntity);
                
                // Моб двигает головой вперед-назад
                mob.setXRot((float) (mob.getXRot() + Math.sin((currentTime / 1000.0) * frequency) * 30));
                
                // Игрок двигается вперед-назад
                senderEntity.setPos(senderEntity.getX(), senderEntity.getY(), senderEntity.getZ() + offset);
            }
        } else {
            // При обычном сексе моб развернут задом и двигается всем телом
            if (session.receiver instanceof Player) {
                // Между игроками: оба двигаются
                Player receiver = (Player) session.receiver;
                Player sender = (Player) session.sender;
                
                // Тот кто сзади двигается вперед-назад
                sender.setPos(sender.getX(), sender.getY(), sender.getZ() + offset);
                
                // Тот кто спереди двигается в противоположной фазе
                receiver.setPos(receiver.getX(), receiver.getY(), receiver.getZ() - offset);
            } else {
                // С мобом: моб развернут задом и двигается всем телом
                Entity mob = session.receiver;
                Entity senderEntity = session.sender;
                
                // Разворачиваем моба задом к игроку
                faceEntityAway(mob, senderEntity);
                
                // Моб двигается всем телом вперед-назад
                mob.setPos(mob.getX(), mob.getY(), mob.getZ() + offset);
                
                // Игрок двигается в противоположной фазе
                senderEntity.setPos(senderEntity.getX(), senderEntity.getY(), senderEntity.getZ() - offset);
            }
        }
    }

    private static void faceEntity(Entity entity, Entity target) {
        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        entity.setYRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
    }

    private static void faceEntityAway(Entity entity, Entity target) {
        double dx = entity.getX() - target.getX();
        double dz = entity.getZ() - target.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        entity.setYRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
    }

    private static String getEntityName(Entity entity) {
        if (entity instanceof Player) {
            return entity.getName().getString();
        } else if (entity instanceof Animal) {
            return ((Animal) entity).getDisplayName().getString();
        } else if (entity instanceof WanderingTrader) {
            return "Странствующий торговец";
        } else if (entity instanceof Villager) {
            return "Житель";
        } else if (entity instanceof AbstractVillager) {
            return "Житель";
        } else {
            return "Существо";
        }
    }

    public static boolean isOnRefractoryCooldown(Player player) {
        Long end = REFRACTORY_COOLDOWN.get(player.getUUID());
        return end != null && System.currentTimeMillis() < end;
    }

    public static int getCooldownSeconds(Player player) {
        Long end = REFRACTORY_COOLDOWN.get(player.getUUID());
        if (end == null) return 0;
        return (int) ((end - System.currentTimeMillis()) / 1000);
    }

    private static void setRefractoryCooldown(Player player) {
        REFRACTORY_COOLDOWN.put(player.getUUID(),
                System.currentTimeMillis() + REFRACTORY_SECONDS * 1000L
        );
    }

    private static boolean isOnCommandCooldown(Player player) {
        Long end = COMMAND_COOLDOWN.get(player.getUUID());
        return end != null && System.currentTimeMillis() < end;
    }

    private static void setCommandCooldown(Player player) {
        COMMAND_COOLDOWN.put(player.getUUID(),
                System.currentTimeMillis() + COMMAND_COOLDOWN_SECONDS * 1000L
        );
    }

    private static class Invitation {
        private final UUID sender;
        private final UUID target;
        private final boolean playerTarget;
        private final boolean blowjob;
        private final long expiryTime;

        public Invitation(UUID sender, UUID target, boolean playerTarget, boolean blowjob, long expiryTime) {
            this.sender = sender;
            this.target = target;
            this.playerTarget = playerTarget;
            this.blowjob = blowjob;
            this.expiryTime = expiryTime;
        }

        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public boolean isPlayerTarget() { return playerTarget; }
        public boolean isBlowjob() { return blowjob; }
        public long getExpiryTime() { return expiryTime; }
    }

    private static class Session {
        private final Entity sender;
        private final Entity receiver;
        private final Invitation invitation;
        private final long endTime;

        public Session(Entity sender, Entity receiver, Invitation invitation, long endTime) {
            this.sender = sender;
            this.receiver = receiver;
            this.invitation = invitation;
            this.endTime = endTime;
        }

        public boolean isActive() {
            return System.currentTimeMillis() < endTime;
        }

        public boolean isBlowjob() {
            return invitation.isBlowjob();
        }
    }
}