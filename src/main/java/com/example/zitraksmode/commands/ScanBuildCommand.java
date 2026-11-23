package com.example.zitraksmode.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ScanBuildCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scanbuild")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
            .then(Commands.argument("width", IntegerArgumentType.integer(1, 100))
            .then(Commands.argument("height", IntegerArgumentType.integer(1, 100))
            .then(Commands.argument("depth", IntegerArgumentType.integer(1, 100))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                String name = StringArgumentType.getString(context, "name");
                int width = IntegerArgumentType.getInteger(context, "width");
                int height = IntegerArgumentType.getInteger(context, "height");
                int depth = IntegerArgumentType.getInteger(context, "depth");

                scanAndGenerate(player, name, width, height, depth);
                return 1;
            })))))
        );
    }

    private static void scanAndGenerate(ServerPlayer player, String name, int width, int height, int depth) {
        Level level = player.level;
        BlockPos startPos = player.blockPosition();

        StringBuilder code = new StringBuilder();
        Map<Item, Integer> materials = new HashMap<>();

        code.append("// ════════════════════════════════════════════════════════════\n");
        code.append("// ПОСТРОЙКА: ").append(name.toUpperCase()).append(" (").append(width).append("×").append(height).append("×").append(depth).append(")\n");
        code.append("// С ПОЛНЫМИ BLOCKSTATE\n");
        code.append("// ════════════════════════════════════════════════════════════\n\n");

        code.append("private void build").append(capitalize(name)).append("Layer(List<BlockPlacement> list, BlockPos pos, int layer) {\n");

        for (int y = 0; y < height; y++) {
            code.append("    if (layer == ").append(y).append(") {\n");

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos checkPos = startPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (!state.isAir()) {
                        Item item = getCountedItem(state);
                        if (item != null && item != Items.AIR) {
                            materials.merge(item, 1, Integer::sum);
                        }

                        String stateString = blockStateToString(state);

                        code.append("        list.add(com.example.zitraksmode.printer.PrinterStateHelper.placement(pos, ")
                                .append(x).append(", ").append(y).append(", ").append(z).append(", \"")
                                .append(escape(stateString)).append("\"));\n");
                    }
                }
            }

            code.append("    }\n");
        }

        code.append("}\n\n");

        code.append("// Материалы:\n");
        code.append("Map.of(\n");

        int i = 0;
        for (Map.Entry<Item, Integer> entry : materials.entrySet()) {
            String itemName = Registry.ITEM.getKey(entry.getKey()).getPath().toUpperCase();

            code.append("    net.minecraft.world.item.Items.")
                    .append(itemName)
                    .append(", ")
                    .append(entry.getValue());

            if (i < materials.size() - 1) {
                code.append(",");
            }
            code.append("\n");
            i++;
        }

        code.append(")\n");

        String filename = "scanned_" + name + ".txt";

        try {
            Files.writeString(Paths.get(filename), code.toString());
            player.sendSystemMessage(Component.literal("§a✔ Код сохранён в файл: " + filename));
            player.sendSystemMessage(Component.literal("§eТеперь states сохраняются. Двери/кровати в материалах не должны дублироваться."));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cОшибка сохранения: " + e.getMessage()));
        }
    }

    private static Item getCountedItem(BlockState state) {
        // Не считаем верхнюю половину двублочных блоков (двери и подобное)
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.UPPER) {
                return null;
            }
        }

        // У кровати считаем только FOOT
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            if (part == BedPart.HEAD) {
                return null;
            }
        }

        Item item = state.getBlock().asItem();
        return item == Items.AIR ? null : item;
    }

    private static String blockStateToString(BlockState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(Registry.BLOCK.getKey(state.getBlock()));

        if (!state.getProperties().isEmpty()) {
            sb.append("[");
            int i = 0;
            for (Property<?> property : state.getProperties()) {
                if (i > 0) sb.append(",");
                sb.append(property.getName()).append("=").append(getPropertyValueName(state, property));
                i++;
            }
            sb.append("]");
        }

        return sb.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String getPropertyValueName(BlockState state, Property<?> property) {
        return ((Property) property).getName(state.getValue((Property) property));
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}