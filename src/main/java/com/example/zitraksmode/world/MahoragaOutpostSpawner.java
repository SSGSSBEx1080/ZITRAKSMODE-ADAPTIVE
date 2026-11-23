package com.example.zitraksmode.world;

import com.example.zitraksmode.ZitraksMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZitraksMode.MODID)
public class MahoragaOutpostSpawner {

    private static final String DATA_NAME = "mahoraga_outpost_status";

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);

        if (level == null) {
            System.err.println("OVERWORLD not loaded, aborting outpost placement!");
            return;
        }

        DimensionDataStorage dataStorage = level.getDataStorage();
        OutpostSavedData savedData = dataStorage.computeIfAbsent(
                OutpostSavedData::new,
                () -> new OutpostSavedData(),
                DATA_NAME
        );

        if (!savedData.isPlaced()) {
            placeOutpost(level, savedData);
        } else {
            System.out.println("[ZitraksMode] Outpost already placed at: " + savedData.getOutpostPos());
        }
    }

    private static void placeOutpost(ServerLevel level, OutpostSavedData savedData) {
    BlockPos spawnPos = level.getSharedSpawnPos();
    BlockPos strongholdPos = level.findNearestMapStructure(
            net.minecraft.tags.StructureTags.EYE_OF_ENDER_LOCATED,
            spawnPos,
            100,
            false
    );

    if (strongholdPos == null) {
        System.out.println("[ZitraksMode] Stronghold not found, using fallback position");
        strongholdPos = spawnPos.offset(500, 0, 500);
    }

    int avgX = (spawnPos.getX() + strongholdPos.getX()) / 2;
    int avgZ = (spawnPos.getZ() + strongholdPos.getZ()) / 2;

    // Принудительно загружаем чанк
    level.getChunk(avgX >> 4, avgZ >> 4);

    // Берём реальную высоту поверхности
    BlockPos surfacePos = level.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos(avgX, 0, avgZ)
    );
    BlockPos targetPos = surfacePos;

    System.out.println("[ZitraksMode] Spawn: " + spawnPos);
    System.out.println("[ZitraksMode] Stronghold: " + strongholdPos);
    System.out.println("[ZitraksMode] Surface Y: " + surfacePos.getY());
    System.out.println("[ZitraksMode] Placing Mahoraga Outpost at: " + targetPos);

    StructureTemplate template = level.getStructureManager()
            .getOrCreate(new ResourceLocation(ZitraksMode.MODID, "mahoraga_outpost"));

    template.placeInWorld(level, targetPos, targetPos,
            new StructurePlaceSettings().setIgnoreEntities(false),
            level.random, 2);

    savedData.setPlaced(true);
    savedData.setOutpostPos(targetPos);
    savedData.setDirty();

    System.out.println("[ZitraksMode] Mahoraga Outpost successfully placed!");
}

    public static class OutpostSavedData extends net.minecraft.world.level.saveddata.SavedData {
        private boolean placed = false;
        private BlockPos outpostPos = BlockPos.ZERO;

        public OutpostSavedData() {}

        public OutpostSavedData(CompoundTag tag) {
            this.placed = tag.getBoolean("placed");
            if (tag.contains("outpostX")) {
                this.outpostPos = new BlockPos(
                    tag.getInt("outpostX"),
                    tag.getInt("outpostY"),
                    tag.getInt("outpostZ")
                );
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("placed", placed);
            tag.putInt("outpostX", outpostPos.getX());
            tag.putInt("outpostY", outpostPos.getY());
            tag.putInt("outpostZ", outpostPos.getZ());
            return tag;
        }

        public boolean isPlaced() {
            return placed;
        }

        public void setPlaced(boolean value) {
            this.placed = value;
        }

        public BlockPos getOutpostPos() {
            return outpostPos;
        }

        public void setOutpostPos(BlockPos pos) {
            this.outpostPos = pos;
        }
    }
}