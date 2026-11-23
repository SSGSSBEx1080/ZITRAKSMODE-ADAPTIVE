package com.example.zitraksmode.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;
import java.util.stream.Collectors;

public class RandomArtPools {
    public static List<String> PRON = new ArrayList<>();
    public static List<String> HENTAI = new ArrayList<>();
    public static List<String> MEME = new ArrayList<>();
    
    private static boolean loaded = false;
    
    public static void init() {
        if (loaded) return;
        loaded = true;

        PRON = loadFromFolder("pron");
        HENTAI = loadFromFolder("hentai");
        MEME = loadFromFolder("meme");

        if (PRON.isEmpty()) PRON.add("1");
        if (HENTAI.isEmpty()) HENTAI.add("1");
        if (MEME.isEmpty()) MEME.add("1");
    }

    private static List<String> loadFromFolder(String folder) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        String base = "textures/block/" + folder + "/";
        // В 1.19.2 listResources возвращает Map<ResourceLocation, Resource>
        Map<ResourceLocation, Resource> map = manager.listResources(base,
            loc -> loc.getPath().endsWith(".png"));
        if (map == null) return new ArrayList<>();
        return map.keySet().stream()
            .map(ResourceLocation::getPath)
            .map(path -> {
                String name = path.substring(path.lastIndexOf('/') + 1);
                return name.replace(".png", "");
            })
            .collect(Collectors.toList());
    }
}