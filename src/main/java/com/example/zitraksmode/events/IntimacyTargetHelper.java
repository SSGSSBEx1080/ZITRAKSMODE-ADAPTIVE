package com.example.zitraksmode.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;

public final class IntimacyTargetHelper {

    private static final int RADIUS = 8;

    private IntimacyTargetHelper() {}

    public static Entity findTarget(Player sender, String name) {
        Level level = sender.level;
        String lowerName = name.toLowerCase();

        // 1. Сначала ищем игрока
        for (Player player : level.players()) {
            if (player != sender &&
                    player.getName().getString().equalsIgnoreCase(name) &&
                    sender.distanceTo(player) <= RADIUS) {
                return player;
            }
        }

        // 2. Ищем животное
        List<Animal> animals = level.getEntitiesOfClass(Animal.class,
                new AABB(sender.blockPosition()).inflate(RADIUS),
                animal -> animal.isAlive() && !animal.isBaby()
        );
        for (Animal animal : animals) {
            String animalName = animal.getDisplayName().getString();
            ResourceLocation animalTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
            
            if (animalTypeKey != null) {
                String animalType = animalTypeKey.getPath();
                if (animalName.equalsIgnoreCase(name) || animalType.equalsIgnoreCase(lowerName)) {
                    return animal;
                }
            } else if (animalName.equalsIgnoreCase(name)) {
                return animal;
            }
        }

        // 3. Ищем всех жителей (обычных и странствующих торговцев)
        List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class,
                new AABB(sender.blockPosition()).inflate(RADIUS),
                villager -> villager.isAlive()
        );
        
        // Сортируем по расстоянию, чтобы найти ближайшего
        villagers.sort(Comparator.comparingDouble(sender::distanceTo));
        
        for (AbstractVillager villager : villagers) {
            if (isVillagerMatch(villager, lowerName)) {
                return villager;
            }
        }

        return null;
    }

    private static boolean isVillagerMatch(AbstractVillager villager, String lowerName) {
        // Проверяем кастомное имя
        if (villager.hasCustomName()) {
            String customName = villager.getCustomName().getString();
            if (customName.equalsIgnoreCase(lowerName)) {
                return true;
            }
        }
        
        // Проверяем тип сущности
        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(villager.getType());
        if (entityTypeKey != null) {
            String entityType = entityTypeKey.getPath();
            if (entityType.equalsIgnoreCase(lowerName)) {
                return true;
            }
        }
        
        // Проверяем жителей по общим названиям
        switch (lowerName) {
            case "villager":
            case "villagers":
            case "npc":
            case "trader":
            case "tradesman":
            case "merchant":
            case "житель": // на случай если кто-то введет русскими буквами
            case "жители":
                return true;
        }
        
        // Проверяем профессии для обычных жителей
        if (villager instanceof Villager) {
            Villager regularVillager = (Villager) villager;
            String profession = regularVillager.getVillagerData().getProfession().toString().toLowerCase();
            
            // Проверяем профессию напрямую
            if (profession.equalsIgnoreCase(lowerName)) {
                return true;
            }
            
            // Проверяем профессии с префиксом "minecraft:"
            if (("minecraft:" + profession).equalsIgnoreCase(lowerName)) {
                return true;
            }
            
            // Проверяем альтернативные названия профессий
            switch (lowerName) {
                case "farmer":
                    return profession.equals("farmer");
                case "librarian":
                    return profession.equals("librarian");
                case "priest":
                case "cleric":
                    return profession.equals("cleric");
                case "blacksmith":
                case "smith":
                    return profession.equals("blacksmith") || 
                           profession.equals("armorer") || 
                           profession.equals("weaponsmith") || 
                           profession.equals("toolsmith");
                case "armorer":
                    return profession.equals("armorer");
                case "weaponsmith":
                    return profession.equals("weaponsmith");
                case "toolsmith":
                    return profession.equals("toolsmith");
                case "butcher":
                    return profession.equals("butcher");
                case "cartographer":
                    return profession.equals("cartographer");
                case "leatherworker":
                case "leather":
                    return profession.equals("leatherworker");
                case "mason":
                case "stone_mason":
                case "stonemason":
                    return profession.equals("mason") || profession.equals("stone_mason");
                case "shepherd":
                    return profession.equals("shepherd");
                case "fisherman":
                case "fisher":
                    return profession.equals("fisherman");
                case "fletcher":
                    return profession.equals("fletcher");
                case "brewer":
                    return profession.equals("brewer");
                case "unemployed":
                case "none":
                case "jobless":
                    return profession.equals("none");
                case "nitwit":
                case "idiot":
                case "fool":
                    return profession.equals("nitwit");
            }
        }
        
        // Для странствующего торговца
        if (villager instanceof WanderingTrader) {
            switch (lowerName) {
                case "wandering_trader":
                case "wandering":
                case "wanderer":
                case "trader":
                case "merchant":
                    return true;
            }
        }
        
        return false;
    }
}