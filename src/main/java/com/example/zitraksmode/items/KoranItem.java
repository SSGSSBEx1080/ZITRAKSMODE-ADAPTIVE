package com.example.zitraksmode.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import com.example.zitraksmode.ZitraksMode;

import com.example.zitraksmode.ZitraksMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

public class KoranItem extends Item {

    public KoranItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        // Перезаписываем NBT при каждом использовании для тестов контента
        setupKoranNbt(itemstack);

        if (level.isClientSide) {
            // Прямой вызов экрана на клиенте
            openBookGui(itemstack);
        } else {
            // ✅ СЕРВЕРНАЯ ПРОВЕРКА АЧИВКИ
            if (player instanceof ServerPlayer sp) {
                CompoundTag tag = itemstack.getOrCreateTag();
                
                // Проверяем что страниц 20 (все прочитаны) и ачивка ещё не выдана
                if (tag.getList("pages", 8).size() >= 20 && !tag.getBoolean("KoranAchievementGiven")) {
                    ZitraksMode.KORAN_FULLY_READ.trigger(sp);
                    tag.putBoolean("KoranAchievementGiven", true); // Чтобы не спамило
                }
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private void openBookGui(ItemStack stack) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                CompoundTag tag = stack.getTag();
                return tag != null ? tag.getList("pages", 8).size() : 0;
            }

            @Override
            public net.minecraft.network.chat.FormattedText getPageRaw(int index) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    ListTag pages = tag.getList("pages", 8);
                    if (index >= 0 && index < pages.size()) {
                        return net.minecraft.network.chat.Component.Serializer.fromJson(pages.getString(index));
                    }
                }
                return net.minecraft.network.chat.Component.empty();
            }
        }));
    }

    private void setupKoranNbt(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag pages = new ListTag();
        
        // СТРАНИЦЫ 1-10 (Обновленные цвета и старый текст)
        pages.add(StringTag.valueOf("{\"text\":\"Сура 1488 Аят 67\\n\\nЯ аллаха ебал,\\nмои рифмы калл.\",\"color\":\"dark_red\",\"bold\":true}"));
        pages.add(StringTag.valueOf("{\"text\":\"Песня про член,\\nкоторый Биг Бен.\\n\\nНе ебу чё пиздеть,\\nбуду просто греть.\",\"color\":\"dark_green\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Аят 69:\\n\\nЗашёл в мечеть,\\nначал пердеть.\\n\\nИмамы в шоке,\\nя на припеве в роке.\",\"color\":\"dark_blue\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Сура про доту:\\n\\nМид проигран,\\nпукан взорван.\\n\\nАллах не помог,\\nя нафидил как мог.\",\"color\":\"dark_purple\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Аят о пиве:\\n\\nХолодное светлое,\\nжелание заветное.\\n\\nХарам или нет?\\nДайте мне ответ.\",\"color\":\"dark_aqua\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Послание хейтерам:\\n\\nВаше мнение - пыль,\\nмой мод - быль.\\n\\nЯ гений модостроя,\\nвы - кучка навоя.\",\"color\":\"black\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Про член часть 2:\\n\\nБиг Бен в штанах,\\nужас в глазах.\\n\\nБашни близнецы пали,\\nкогда мы его достали.\",\"color\":\"dark_blue\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Сура 228:\\n\\nПодошёл легавый,\\nдумал я правый.\\n\\nА я левый совсем,\\nне достанусь я всем.\",\"color\":\"dark_red\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Аят про ZitraksMode:\\n\\nМод качай,\\nгоре не знай.\\n\\nТНТ броня на мне,\\nя лечу на Луне.\",\"color\":\"black\",\"italic\":true}"));
        pages.add(StringTag.valueOf("{\"text\":\"Финал манускрипта:\\n\\nКто дочитал - тот лох,\\nу кого мозг усох.\\n\\nАминь, хуле.\",\"color\":\"dark_gray\"}"));

        // НОВЫЕ СТРАНИЦЫ 11-20 (Максимум пиздеца)
        pages.add(StringTag.valueOf("{\"text\":\"Сура о Жителе-Дауне:\\n\\nНос как картошка,\\nв голове морошка.\\n\\nТоргует гнилью,\\nстал моей былью.\",\"color\":\"dark_green\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Аят 1337:\\n\\nСкибиди туалет,\\nв мечети мест нет.\\n\\nКамерамен пришёл,\\nсвой конец нашёл.\",\"color\":\"dark_red\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Метаирония:\\n\\nЯ пишу этот код,\\nпока сохнет мой рот.\\n\\nНейронка сойдёт с ума,\\nв коде сплошная тьма.\",\"color\":\"black\",\"bold\":true}"));
        pages.add(StringTag.valueOf("{\"text\":\"Про снайперку:\\n\\nАнимацию забросил,\\nв чат говна набросил.\\n\\nСтреляю в упор,\\nнесу этот вздор.\",\"color\":\"dark_blue\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Лабубу-Ганг:\\n\\nЖелтый, белый, черный,\\nя пацан упорный.\\n\\nСобрал коллекцию хлама,\\nрада будет мама.\",\"color\":\"dark_aqua\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Сура Пророка Z:\\n\\nZitraks - мой папа,\\nу мода есть лапа.\\n\\nГребёт бабло из пустоты,\\nсжигая за собой мосты.\",\"color\":\"dark_purple\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Аят ТНТ-Ботинок:\\n\\nПрыгнул с горы,\\nразнёс все дворы.\\n\\nНоги в щепки,\\nзато мы крепки.\",\"color\":\"dark_red\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Послание Forge:\\n\\n43.5.0 - мой номер,\\nпока я не помер.\\n\\nРегистрирую калл,\\nчтоб ты закричал.\",\"color\":\"black\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"Предпоследний кринж:\\n\\nЯ ебал этот движок,\\nмой милый дружок.\\n\\nNBT - это ад,\\nя этому рад.\",\"color\":\"dark_green\"}"));
        pages.add(StringTag.valueOf("{\"text\":\"ПОСЛЕСЛОВИЕ:\\n\\nЕсли ты это читаешь,\\nзначит ты всё понимаешь.\\n\\nСлава мододелам!\\nСмерть скучным делам!\",\"color\":\"dark_red\",\"bold\":true}"));
        
        tag.put("pages", pages);
    }

    @Override
    public boolean isFireResistant() {
        return true;
    }
}