package com.example.zitraksmode.items.katana;

import com.example.zitraksmode.ModEntities;
import com.example.zitraksmode.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;

public class SlashAttackEntity extends Projectile {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    
    private static final double MAX_DISTANCE = 5.0;
    private float damage = 6.0f;
    private Vec3 startPos;
    
    public SlashAttackEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }
    
    public SlashAttackEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.SLASH_ATTACK.get(), level);
        this.setOwner(shooter);
        this.damage = damage;
        this.startPos = shooter.position();
        
        Vec3 lookVec = shooter.getLookAngle();
        this.setDeltaMovement(lookVec.scale(2.0)); // Быстрее!
        
        this.setPos(
            shooter.getX() + lookVec.x,
            shooter.getEyeY() - 0.1,
            shooter.getZ() + lookVec.z
        );
        
        // Звук слэша (случайный)
        if (!level.isClientSide) {
            int soundIndex = RANDOM.nextInt(3) + 1;
            switch (soundIndex) {
                case 1 -> level.playSound(null, shooter.blockPosition(), 
                    ModSounds.KATANA_SLASH_1.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                case 2 -> level.playSound(null, shooter.blockPosition(), 
                    ModSounds.KATANA_SLASH_2.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                case 3 -> level.playSound(null, shooter.blockPosition(), 
                    ModSounds.KATANA_SLASH_3.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
        
        LOGGER.info("🌊 Slash spawned with damage: {}", damage);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.startPos != null) {
            double distance = this.position().distanceTo(this.startPos);
            if (distance > MAX_DISTANCE) {
                this.discard();
                return;
            }
        }
        
        Vec3 movement = this.getDeltaMovement();
        Vec3 nextPos = this.position().add(movement);
        
        checkHits();
        
        // Частицы слэша
        if (this.level.isClientSide) {
            for (int i = 0; i < 5; i++) {
                this.level.addParticle(
                    ParticleTypes.SWEEP_ATTACK,
                    this.getX() + (random.nextDouble() - 0.5) * 0.5,
                    this.getY() + (random.nextDouble() - 0.5) * 0.5,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.5,
                    0, 0, 0
                );
                
                this.level.addParticle(
                    ParticleTypes.CRIT,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1
                );
            }
        }
        
        this.setPos(nextPos);
    }
    
    private void checkHits() {
        if (this.level.isClientSide) return;
        
        AABB hitBox = this.getBoundingBox().inflate(0.5);
        List<LivingEntity> entities = this.level.getEntitiesOfClass(
            LivingEntity.class, 
            hitBox, 
            entity -> entity != this.getOwner() && entity.isAlive()
        );
        
        for (LivingEntity target : entities) {
            DamageSource damageSource;
            if (this.getOwner() instanceof LivingEntity owner) {
                damageSource = DamageSource.indirectMobAttack(this, owner);
            } else {
                damageSource = DamageSource.GENERIC;
            }
            
            target.hurt(damageSource, this.damage);
            
            // Частицы крови
            for (int i = 0; i < 20; i++) {
                this.level.addParticle(
                    ParticleTypes.DAMAGE_INDICATOR,
                    target.getX() + (random.nextDouble() - 0.5),
                    target.getY() + random.nextDouble() * target.getBbHeight(),
                    target.getZ() + (random.nextDouble() - 0.5),
                    0, 0, 0
                );
            }
            
            this.discard();
            break;
        }
    }
    
    @Override
    protected void defineSynchedData() {
    }
    
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
