package com.example.zitraksmode.mixin;

import net.minecraft.world.entity.monster.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public interface ZombieVillagerInvokerMixin {

    @Invoker("startConverting")
    void zitraksmode$startConverting(
            UUID conversionStarter,
            int conversionTime
    );
}