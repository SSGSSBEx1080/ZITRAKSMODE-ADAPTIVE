package com.example.zitraksmode.blockentities;

import com.example.zitraksmode.ModBlockEntities;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.blocks.RandomArtPools;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HentaiBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float MEME_CHANCE = 0.05f;
    private static final double USED_WEIGHT_PENALTY = 0.3;
    private static final String FALLBACK_TEXTURE = "hentai/1";
    private static final List<String> FACE_KEYS = Arrays.asList("north", "south", "east", "west", "up", "down");

    private final Map<String, String> textures = new LinkedHashMap<>();
    private final Map<String, Boolean> lockedFaces = new LinkedHashMap<>();

    public HentaiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HENTAI_BLOCK.get(), pos, state);
        for (String face : FACE_KEYS) {
            textures.put(face, FALLBACK_TEXTURE);
            lockedFaces.put(face, false);
        }
    }

    public void randomizeTextures(RandomSource random) {
        Random javaRandom = new Random(random.nextLong());

        List<String> unlockedFaces = new ArrayList<>();
        for (String face : FACE_KEYS) {
            if (!isFaceLocked(face)) {
                unlockedFaces.add(face);
            } else if (textures.get(face) == null || textures.get(face).isEmpty()) {
                textures.put(face, FALLBACK_TEXTURE);
            }
        }

        if (unlockedFaces.isEmpty()) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return;
        }

        Collections.shuffle(unlockedFaces, javaRandom);

        int pronCount = unlockedFaces.size() / 2;
        int hentaiCount = unlockedFaces.size() - pronCount;

        List<String> pronFaces = unlockedFaces.subList(0, pronCount);
        List<String> hentaiFaces = unlockedFaces.subList(pronCount, pronCount + hentaiCount);

        WeightedPool pronPool = new WeightedPool(RandomArtPools.PRON);
        WeightedPool hentaiPool = new WeightedPool(RandomArtPools.HENTAI);
        WeightedPool memePool = new WeightedPool(RandomArtPools.MEME);

        for (String face : pronFaces) {
            if (javaRandom.nextFloat() < MEME_CHANCE && !memePool.isEmpty()) {
                textures.put(face, "meme/" + memePool.pickAndPenalize(javaRandom));
            } else if (!pronPool.isEmpty()) {
                textures.put(face, "pron/" + pronPool.pickAndPenalize(javaRandom));
            } else {
                textures.put(face, "pron/1");
            }
        }

        for (String face : hentaiFaces) {
            if (javaRandom.nextFloat() < MEME_CHANCE && !memePool.isEmpty()) {
                textures.put(face, "meme/" + memePool.pickAndPenalize(javaRandom));
            } else if (!hentaiPool.isEmpty()) {
                textures.put(face, "hentai/" + hentaiPool.pickAndPenalize(javaRandom));
            } else {
                textures.put(face, "hentai/1");
            }
        }

        int memeCount = 0;
        for (String texture : textures.values()) {
            if (texture != null && texture.startsWith("meme/")) {
                memeCount++;
            }
        }

        if (memeCount >= 2 && level != null && !level.isClientSide) {
            Player nearest = level.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 8.0, false);
            if (nearest instanceof net.minecraft.server.level.ServerPlayer sp) {
                ZitraksMode.HENTAI_MEME_FACES.trigger(sp);
            }
        }

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean toggleFaceLock(String face) {
        if (!lockedFaces.containsKey(face)) {
            return false;
        }

        boolean newValue = !lockedFaces.getOrDefault(face, false);
        lockedFaces.put(face, newValue);

        if (newValue && (textures.get(face) == null || textures.get(face).isEmpty())) {
            textures.put(face, FALLBACK_TEXTURE);
        }

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return newValue;
    }

    public boolean isFaceLocked(String face) {
        return lockedFaces.getOrDefault(face, false);
    }

    public boolean hasAnyLockedFaces() {
        for (String face : FACE_KEYS) {
            if (lockedFaces.getOrDefault(face, false)) {
                return true;
            }
        }
        return false;
    }

    public String getTextureKey(String face) {
        return textures.getOrDefault(face, FALLBACK_TEXTURE);
    }

    public String getFaceDisplayName(String face) {
        return buildFaceDisplayName(getTextureKey(face));
    }

    public static String buildFaceDisplayName(String textureKey) {
        if (textureKey == null || textureKey.isEmpty()) {
            return "Грань";
        }

        String[] parts = textureKey.split("/", 2);
        String category = parts.length > 0 ? parts[0] : "unknown";
        String rawName = parts.length > 1 ? parts[1] : textureKey;

        String prefix = switch (category) {
            case "pron" -> "Порно грань ";
            case "hentai" -> "Хентай грань ";
            case "meme" -> "Мем грань ";
            default -> "Грань ";
        };

        return prefix + formatTextureName(rawName);
    }

    private static String formatTextureName(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "неизвестно";
        }
        if (rawName.matches("\\d+")) {
            return "№" + rawName;
        }
        return rawName.replace('_', ' ');
    }

    public Map<String, String> getTextureKeys() {
        return textures;
    }

    public void saveToItemTag(CompoundTag stackTag) {
        if (!hasAnyLockedFaces()) {
            return;
        }
        stackTag.put("BlockEntityTag", this.saveWithoutMetadata());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        for (Map.Entry<String, String> entry : textures.entrySet()) {
            tag.putString(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Boolean> entry : lockedFaces.entrySet()) {
            tag.putBoolean(entry.getKey() + "_locked", entry.getValue());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        for (String face : FACE_KEYS) {
            if (tag.contains(face)) {
                textures.put(face, tag.getString(face));
            } else {
                textures.put(face, FALLBACK_TEXTURE);
            }
            lockedFaces.put(face, tag.getBoolean(face + "_locked"));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    private static class WeightedPool {
        private final List<String> items;
        private final double[] weights;

        WeightedPool(List<String> source) {
            this.items = new ArrayList<>(source);
            this.weights = new double[items.size()];
            Arrays.fill(this.weights, 1.0);
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        String pickAndPenalize(Random random) {
            double totalWeight = 0.0;
            for (double w : weights) {
                totalWeight += w;
            }

            if (totalWeight <= 0.0) {
                return items.get(random.nextInt(items.size()));
            }

            double roll = random.nextDouble() * totalWeight;
            double acc = 0.0;

            for (int i = 0; i < items.size(); i++) {
                acc += weights[i];
                if (roll < acc) {
                    String result = items.get(i);
                    weights[i] *= USED_WEIGHT_PENALTY;
                    return result;
                }
            }

            return items.get(items.size() - 1);
        }
    }
}