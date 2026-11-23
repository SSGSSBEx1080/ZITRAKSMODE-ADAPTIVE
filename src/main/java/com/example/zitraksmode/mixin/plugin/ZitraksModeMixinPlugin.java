package com.example.zitraksmode.mixin.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ZitraksModeMixinPlugin implements IMixinConfigPlugin {

    private boolean optifinePresent = false;

    @Override
    public void onLoad(String mixinPackage) {
        // Проверяем наличие OptiFine через системное свойство (устанавливается самим OptiFine)
        String optifineVersion = System.getProperty("optifine.version");
        if (optifineVersion != null && !optifineVersion.isEmpty()) {
            optifinePresent = true;
            System.out.println("[ZitraksMode] OptiFine detected via system property. Disabling incompatible mixins.");
            return;
        }

        // Альтернативная проверка: пытаемся найти класс, но безопасно
        try {
            Class.forName("optifine.OptiFineForgeTweaker", false, getClass().getClassLoader());
            optifinePresent = true;
            System.out.println("[ZitraksMode] OptiFine detected. Disabling incompatible mixins.");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            optifinePresent = false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!optifinePresent) return true;

        // Укажите полные имена ваших миксинов, которые конфликтуют с OptiFine
        List<String> unsafeMixins = List.of(
            "com.example.zitraksmode.mixin.CowRendererMixin",
            "com.example.zitraksmode.mixin.RavagerRendererMixin",
            "com.example.zitraksmode.mixin.CreeperRendererMixin",
            "com.example.zitraksmode.mixin.CowMergedMixin",
            "com.example.zitraksmode.mixin.RavagerMergedMixin",
            "com.example.zitraksmode.mixin.BucketItemBullMixin",
            "com.example.zitraksmode.mixin.BucketItemRavagerMixin"
        );

        if (unsafeMixins.contains(mixinClassName)) {
            System.out.println("[ZitraksMode] Skipping mixin: " + mixinClassName);
            return false;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() { return null; }
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override
    public List<String> getMixins() { return null; }
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}