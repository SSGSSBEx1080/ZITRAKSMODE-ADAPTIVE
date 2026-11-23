package com.example.zitraksmode.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LabubuFigurineItem extends Item {

    public final LabubuType type;

    public LabubuFigurineItem(Properties properties, LabubuType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return type.rarity.minecraftRarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (type) {
            // === COMMON ===
            case RED -> {
                addAura(tooltip, "даёт +1 урон с ударов");
            }
            case BLUE -> {
                addAura(tooltip, "даёт высокий прыжок");
            }
            case YELLOW -> {
                addAura(tooltip, "даёт +7% скорости бега");
            }
            case WHITE -> {
                addAura(tooltip, "даёт -6% входящего урона");
            }
            case BLACK -> {
                addAura(tooltip, "даёт +5% исходящего урона");
            }
            case GREEN -> {
                addAura(tooltip, "даёт реген здоровья");
                addAura(tooltip, "≈ полсердца раз в 5 сек");
            }

            // === UNCOMMON ===
            case PURPLE -> {
                addAura(tooltip, "даёт +10% скорости ломания");
            }
            case BLACK_WHITE -> {
                addAura(tooltip, "даёт +5% урона");
                addAura(tooltip, "даёт -6% входящего урона");
            }
            case STRIPED -> {
                addAura(tooltip, "ночью +10% урона");
                addAura(tooltip, "днём -10% входящего урона");
            }

            // === RARE ===
            case KAZAKH -> {
                addAura(tooltip, "даёт +100% урона по коням");
                addAura(tooltip, "даёт +100% скорости на коне");
            }
            case UKRAINIAN -> {
                addAura(tooltip, "даёт +100% урона по свиньям");
                addAura(tooltip, "даёт +250% скорости на свинье");
                addAura(tooltip, "даёт +100% скорости поедания свинины");
            }
            case JAPANESE -> {
                addAura(tooltip, "даёт +100% удачи в рыбалке");
                addAura(tooltip, "даёт клёв в 2 раза чаще");
                addAura(tooltip, "режет поле зрения на 50%");
            }
            case TOP_HAT -> {
                addAura(tooltip, "усиливает шлем и его чары на 75%");
            }
            case FASELESS -> {
                addAura(tooltip, "замедляет вражеские снаряды");
                addAura(tooltip, "ускоряет снаряды хозяина");
            }

            // === EPIC ===
            case RUSSIAN -> {
                addAura(tooltip, "даёт +150% скорости поедания");
                addAura(tooltip, "даёт +50% урона с руки");
                addAura(tooltip, "даёт ядовитую камеру");
            }
            case NEON -> {
                addAura(tooltip, "даёт ночное зрение");
            }
            case LITVIN -> {
                addAura(tooltip, "в приседе даёт +75% урона");
                addAura(tooltip, "даёт +50% скорости атаки");
                addAura(tooltip, "бьёт хозяина раз в 10 сек");
            }
            case ANONYMOUS -> {
                addAura(tooltip, "скрывает от враждебных мобов");
                addAura(tooltip, "скрывает ник над головой");
                addAura(tooltip, "радиус ауры: 3 блока");
            }
            case DEMON -> {
                addAura(tooltip, "даёт иммунитет к огню и лаве");
                addAura(tooltip, "даёт +100% урона по горящим");
                addAura(tooltip, "поджигает врагов рядом");
            }
            case ANGEL -> {
                addAura(tooltip, "воскрешает раз в 180 сек");
                addAura(tooltip, "даёт +25% скорости");
                addAura(tooltip, "даёт +50% защиты");
            }

            // === LEGENDARY ===
            case GALACTIC -> {
                addAura(tooltip, "тянет предметы как чёрная дыра");
                addAura(tooltip, "отталкивает врагов");
            }
            case RAINBOW -> {
                addAura(tooltip, "заставляет всех существ спариваться");
            }
            case GOLDEN -> {
                addAura(tooltip, "превращает яблоки в золотые");
                addAura(tooltip, "превращает морковь в золотую");
            }
            case BUDDHA -> {
                addAura(tooltip, "гасит агрессию мобов");
                addAura(tooltip, "обезоруживает врагов");
                addAura(tooltip, "запрещает урон и ломание блоков");
            }
            case BOOBA -> {
                addAura(tooltip, "спавнит ведро спермы");
                addAura(tooltip, "заставляет жителей спариваться");
            }
            case ANIME -> {
                addAura(tooltip, "даёт 5 случайных баффов");
                addAura(tooltip, "баффы меняются каждые 5 сек");
            }
        }
    }

    private void addAura(List<Component> tooltip, String text) {
        tooltip.add(Component.literal("Аура: " + text).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = clickedPos.above();

        BlockState targetState = level.getBlockState(placePos);

        if (targetState.isAir() && context.getPlayer() != null) {
            com.example.zitraksmode.ModBlocks.LABUBU_FIGURINE_BLOCKS.get(this.type).ifPresent(block -> {
                level.setBlock(
                        placePos,
                        block.defaultBlockState().setValue(
                                com.example.zitraksmode.blocks.LabubuFigurineBlock.FACING,
                                context.getHorizontalDirection().getOpposite()
                        ),
                        3
                );
            });

            if (!context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}