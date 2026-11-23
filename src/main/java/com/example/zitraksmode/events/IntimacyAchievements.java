package com.example.zitraksmode.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class IntimacyAchievements {
    private static final Set<UUID> FIRST_TIME = new HashSet<>();
    private static final Set<UUID> FIRST_TIME_RECEIVE = new HashSet<>();
    private static final Set<UUID> ANIMAL_SET = new HashSet<>();
    private static final Set<String> ANIMAL_TYPES = new HashSet<>();
    private static final Set<UUID> DIPLOMAT_SET = new HashSet<>();
    private static final Map<UUID, Integer> VILLAGER_COUNT = new HashMap<>();
    private static final Set<UUID> REJECTED_SET = new HashSet<>();
    private static final Set<UUID> SUBMITTED_SET = new HashSet<>();
    private static final Set<UUID> CARING_SET = new HashSet<>();
    private static final Map<UUID, Integer> SEX_COUNT = new HashMap<>();
    private static final Map<UUID, Integer> PARTNER_COUNT = new HashMap<>();
    private static final Set<UUID> TRUSTING_SET = new HashSet<>();

    private IntimacyAchievements() {}

    public static void unlockFirstTime(Player initiator, Player partner) {
        if (initiator instanceof ServerPlayer sp) {
            if (!FIRST_TIME.contains(initiator.getUUID())) {
                FIRST_TIME.add(initiator.getUUID());
                grant(sp, "00_guest/first_time");
            }
        }
        if (partner instanceof ServerPlayer sp) {
            if (!FIRST_TIME_RECEIVE.contains(partner.getUUID())) {
                FIRST_TIME_RECEIVE.add(partner.getUUID());
                grant(sp, "00_guest/first_time_receive");
            }
        }
        checkBiome(initiator);
        checkBiome(partner);
    }

    public static void unlockAnimalLover(Player player, Animal animal) {
        if (!(player instanceof ServerPlayer sp)) return;
        
        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        if (entityTypeKey != null) {
            String type = entityTypeKey.getPath();
            ANIMAL_TYPES.add(type);
        }
        
        if (!ANIMAL_SET.contains(player.getUUID())) {
            ANIMAL_SET.add(player.getUUID());
            grant(sp, "00_guest/animal_lover");
        }
        
        if (ANIMAL_TYPES.size() >= 9) {
            grantIfNot(player, "00_guest/farmer_pervert");
        }
        
        checkBiome(player);
        incrementSexCount(player);
        grantIfNot(player, "00_guest/gave_up");
    }

    public static void unlockDiplomat(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        
        if (!DIPLOMAT_SET.contains(player.getUUID())) {
            DIPLOMAT_SET.add(player.getUUID());
            grant(sp, "00_guest/diplomat");
        }
        
        UUID uuid = player.getUUID();
        int count = VILLAGER_COUNT.getOrDefault(uuid, 0) + 1;
        VILLAGER_COUNT.put(uuid, count);
        
        if (count >= 10) {
            grantIfNot(player, "00_guest/mayor");
        }
        
        incrementSexCount(player);
    }

    public static void unlockRejected(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!REJECTED_SET.contains(player.getUUID())) {
            REJECTED_SET.add(player.getUUID());
            grant(sp, "00_guest/rejected");
        }
    }

    public static void unlockSubmitted(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!SUBMITTED_SET.contains(player.getUUID())) {
            SUBMITTED_SET.add(player.getUUID());
            grant(sp, "00_guest/submitted");
        }
    }

    public static void unlockCaring(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!CARING_SET.contains(player.getUUID())) {
            CARING_SET.add(player.getUUID());
            grant(sp, "00_guest/caring");
        }
    }

    public static void unlockGaveUp(Player player) {
        grantIfNot(player, "00_guest/gave_up");
    }

    public static void unlockTrusting(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!TRUSTING_SET.contains(player.getUUID())) {
            TRUSTING_SET.add(player.getUUID());
            grant(sp, "00_guest/trusting");
        }
    }

    public static void incrementSexCount(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        UUID uuid = player.getUUID();
        int count = SEX_COUNT.getOrDefault(uuid, 0) + 1;
        SEX_COUNT.put(uuid, count);
        
        if (count >= 10) {
            grantIfNot(player, "00_guest/tired");
        }
    }

    public static void incrementPartnerCount(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        UUID uuid = player.getUUID();
        int count = PARTNER_COUNT.getOrDefault(uuid, 0) + 1;
        PARTNER_COUNT.put(uuid, count);
        
        if (count >= 10) {
            grantIfNot(player, "00_guest/maniac");
        }
    }

    public static void registerSex(Player initiator, Player partner) {
        if (initiator instanceof ServerPlayer) {
            incrementSexCount(initiator);
            incrementPartnerCount(initiator);
        }
        if (partner instanceof ServerPlayer) {
            incrementSexCount(partner);
            incrementPartnerCount(partner);
            unlockTrusting(partner);
        }
    }

    private static void checkBiome(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        Level level = player.level;
        BlockPos pos = player.blockPosition();
        
        // 1. Проверяем измерение
        String dimension = level.dimension().location().toString();
        System.out.println("[DEBUG] Dimension: " + dimension);
        
        if (dimension.contains("the_end")) {
            grantIfNot(player, "00_guest/ender_bang");
            System.out.println("[DEBUG] Granted ender_bang");
        }
        if (dimension.contains("the_nether")) {
            grantIfNot(player, "00_guest/nether_bang");
            System.out.println("[DEBUG] Granted nether_bang");
        }
        
        // 2. Проверяем воду
        if (player.isInWater()) {
            grantIfNot(player, "00_guest/water_bang");
            System.out.println("[DEBUG] Granted water_bang");
        }
        
        // 3. Проверяем высоту
        if (player.getY() < -30) {
            grantIfNot(player, "00_guest/cave_bang");
            System.out.println("[DEBUG] Granted cave_bang");
        }
        
        // 4. Проверяем биом через ForgeRegistries
        Holder<Biome> biomeHolder = level.getBiome(pos);
        if (biomeHolder != null) {
            ResourceLocation biomeKey = ForgeRegistries.BIOMES.getKey(biomeHolder.value());
            
            if (biomeKey != null) {
                String biomePath = biomeKey.getPath();
                System.out.println("[DEBUG] Biome: " + biomePath);
                
                // Лес и тайга
                if (biomePath.contains("forest") || biomePath.contains("taiga")) {
                    grantIfNot(player, "00_guest/forest_king");
                    System.out.println("[DEBUG] Granted forest_king");
                }
                
                // Джунгли и болото
                if (biomePath.contains("jungle") || biomePath.contains("swamp")) {
                    grantIfNot(player, "00_guest/nature_guardian");
                    System.out.println("[DEBUG] Granted nature_guardian");
                }
            }
        }
    }

    private static void grantIfNot(Player player, String path) {
        if (player instanceof ServerPlayer sp) {
            grant(sp, path);
        }
    }

    private static void grant(ServerPlayer player, String path) {
        Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation("zitraksmode", path));
        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
                System.out.println("[DEBUG] Granted achievement: " + path);
            } else {
                System.out.println("[DEBUG] Already has achievement: " + path);
            }
        } else {
            System.out.println("[DEBUG] Achievement not found: " + path);
        }
    }
}