package com.example.zitraksmode.criteria;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.items.LabubuType;
import java.util.HashSet;
import java.util.Set;

public class LabubuCollectorTrigger extends SimpleCriterionTrigger<LabubuCollectorTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(ZitraksMode.MODID, "labubu_collector");
    private static final String NBT_TAG = "ZitraksLabubuCollection";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext ctx) {
        return new Instance(player);
    }

    /**
     * Вызывается каждый раз, когда игрок получает ЛЮБУЮ фигурку Лабубу.
     * Добавляет её в личный список игрока и проверяет условие.
     */
    public void checkAndTrigger(ServerPlayer player, LabubuType type) {
        CompoundTag data = player.getPersistentData();
        
        // Получаем или создаём список собранных типов
        Set<String> collected = new HashSet<>();
        if (data.contains(NBT_TAG)) {
            ListTag list = data.getList(NBT_TAG, 8); // 8 = TagString
            for (int i = 0; i < list.size(); i++) {
                collected.add(list.getString(i));
            }
        }

        // Добавляем текущий тип
        String typeName = type.name();
        if (collected.add(typeName)) {
            // Если новый элемент добавился, сохраняем обратно
            ListTag newList = new ListTag();
            for (String s : collected) {
                newList.add(StringTag.valueOf(s));
            }
            data.put(NBT_TAG, newList);
            
            // Проверяем, собраны ли все 27
            if (collected.size() >= LabubuType.values().length) {
                this.trigger(player, instance -> true);
            }
        }
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(EntityPredicate.Composite player) { super(ID, player); }
    }
}