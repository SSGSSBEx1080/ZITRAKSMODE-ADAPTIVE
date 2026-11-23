package com.example.zitraksmode.items;

import com.example.zitraksmode.ModBlocks;
import com.example.zitraksmode.ModEnchantments;
import com.example.zitraksmode.ModSounds;
import com.example.zitraksmode.ZitraksMode;
import com.example.zitraksmode.blockentities.HentaiBlockEntity;
import com.example.zitraksmode.network.NetworkHandler;
import com.example.zitraksmode.network.UniverseResetPacket;
import com.example.zitraksmode.network.mih.MIHMusicPackets;
import com.example.zitraksmode.network.mih.StopMadeInHeavenSoundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MadeInHeavenItem extends Item {

    private static boolean isUniverseResetting = false;
    public static int currentTick = 0;
    public static int resetTick = 720;

    public MadeInHeavenItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResultHolder.fail(itemStack);
            }

            ServerLevel serverLevel = (ServerLevel) level;

            if (isUniverseResetting) {
                player.sendSystemMessage(Component.literal("§c§lMade in Heaven уже активирован! Подождите завершения..."));
                return InteractionResultHolder.fail(itemStack);
            }

            startMadeInHeaven(serverLevel, serverPlayer, itemStack.copy());

            ZitraksMode.MIH_ACTIVATE.trigger(serverPlayer);
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    private void startMadeInHeaven(ServerLevel level, ServerPlayer player, ItemStack stack) {
        isUniverseResetting = true;
        currentTick = 0;

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.MADE_IN_HEAVEN.get(),
                SoundSource.MASTER,
                100.0F,
                1.0F
        );

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6§lMade in Heaven §r§7активирован..."),
                false
        );

        level.setWeatherParameters(999999, 0, false, false);

        boolean yobyrMamok = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.YOBYR_MAMOK.get(), stack) > 0;
        boolean hochuLabubu = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.HOCHU_LABUBU.get(), stack) > 0;

        TimeAccelerationTask task = new TimeAccelerationTask(level, player, yobyrMamok, hochuLabubu);
        MinecraftForge.EVENT_BUS.register(task);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Фрагмент самого времени"));
        tooltip.add(Component.literal("§8ПКМ чтобы ускорить время"));
        tooltip.add(Component.literal("§bВо время действия отражает снаряды"));
        tooltip.add(Component.literal("§c§lОдноразовый предмет"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static class TimeAccelerationTask {

        private final ServerLevel level;
        private final ServerPlayer player;
        private final boolean yobyrMamok;
        private final boolean hochuLabubu;

        private int tickCounter = 0;
        private static final int MAX_TICKS = 940;
        private static final int RESET_TICK = 720;

        private boolean finished = false;
        private boolean resetPerformed = false;

        private final Random random = new Random();
        private int weatherTickCounter = 0;

        private static final UUID SPEED_UUID = UUID.fromString("7f8e84f0-1234-4321-abcd-1234567890ab");

        public TimeAccelerationTask(ServerLevel level, ServerPlayer player, boolean yobyrMamok, boolean hochuLabubu) {
            this.level = level;
            this.player = player;
            this.yobyrMamok = yobyrMamok;
            this.hochuLabubu = hochuLabubu;
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            if (finished) {
                MinecraftForge.EVENT_BUS.unregister(this);
                return;
            }

            if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved()) {
                cancelMadeInHeaven();
                return;
            }

            if (tickCounter >= MAX_TICKS) {
                finished = true;
                MinecraftForge.EVENT_BUS.unregister(this);
                isUniverseResetting = false;
                currentTick = 0;
                forceRemoveSpeed();
                MIHMusicPackets.sendToAll(new StopMadeInHeavenSoundPacket());
                return;
            }

            currentTick = tickCounter;
            reflectProjectiles();

            if (!resetPerformed) {
                float progress = (float) tickCounter / RESET_TICK;
                int timeIncrease;

                if (progress < 0.7f) {
                    timeIncrease = (int) (20 + Math.pow(progress, 1.8) * 1000);
                } else if (progress < 0.9f) {
                    float subProgress = (progress - 0.7f) / 0.2f;
                    timeIncrease = (int) (500 + subProgress * 1500);
                } else if (progress < 0.97f) {
                    float subProgress = (progress - 0.9f) / 0.07f;
                    timeIncrease = (int) (2000 + Math.pow(subProgress, 2) * 6000);
                } else {
                    float subProgress = (progress - 0.97f) / 0.03f;
                    timeIncrease = (int) (8000 + Math.pow(subProgress, 3) * 10000);
                }

                long newTime = level.getDayTime() + timeIncrease;
                level.setDayTime(newTime);

                for (ServerPlayer targetPlayer : level.players()) {
                    targetPlayer.connection.send(new ClientboundSetTimePacket(
                            targetPlayer.getLevel().getGameTime(),
                            newTime,
                            true
                    ));
                }

                accelerateWeather(progress, timeIncrease);
                updatePlayerSpeed(progress);
            } else {
                long noon = 6000;
                level.setDayTime(noon);

                for (ServerPlayer targetPlayer : level.players()) {
                    targetPlayer.connection.send(new ClientboundSetTimePacket(
                            targetPlayer.getLevel().getGameTime(),
                            noon,
                            false
                    ));
                }
            }

            if (tickCounter % 4 == 0) {
                spawnDivineParticles(resetPerformed ? 1.0f : (float) tickCounter / RESET_TICK);
            }

            if (tickCounter % 3 == 0) {
                spawnSpiralEffects(resetPerformed ? 1.0f : (float) tickCounter / RESET_TICK);
            }

            if (tickCounter > 500 && tickCounter % 4 == 0 && !resetPerformed) {
                spawnRandomLasers((float) tickCounter / RESET_TICK);
            }

            if (tickCounter == RESET_TICK && !resetPerformed) {
                if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved()) {
                    cancelMadeInHeaven();
                    return;
                }

                performUniverseReset();
                resetPerformed = true;
            }

            tickCounter++;
        }

        private void cancelMadeInHeaven() {
            finished = true;
            MinecraftForge.EVENT_BUS.unregister(this);
            isUniverseResetting = false;
            currentTick = 0;
            forceRemoveSpeed();

            MIHMusicPackets.sendToAll(new StopMadeInHeavenSoundPacket());

            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§c§lMade in Heaven отменён: пользователь умер."),
                    false
            );
        }

        private void reflectProjectiles() {
            double radius = 4.5D;

            List<Projectile> projectiles = level.getEntitiesOfClass(
                    Projectile.class,
                    player.getBoundingBox().inflate(radius),
                    projectile -> projectile.isAlive() && projectile.getOwner() != player
            );

            for (Projectile projectile : projectiles) {
                double dx = projectile.getX() - player.getX();
                double dy = projectile.getY() - player.getEyeY();
                double dz = projectile.getZ() - player.getZ();

                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1.0E-6) {
                    continue;
                }

                double nx = dx / len;
                double ny = dy / len;
                double nz = dz / len;

                Vec3Like motion = new Vec3Like(projectile.getDeltaMovement().x, projectile.getDeltaMovement().y, projectile.getDeltaMovement().z);
                double dotToPlayer = motion.x * (-nx) + motion.y * (-ny) + motion.z * (-nz);
                if (dotToPlayer <= 0.0D) {
                    continue;
                }

                double speed = Math.max(1.5D, projectile.getDeltaMovement().length());
                projectile.setDeltaMovement(nx * speed, ny * speed, nz * speed);
                projectile.hurtMarked = true;
                projectile.setOwner(player);

                if (tickCounter % 2 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, projectile.getX(), projectile.getY(), projectile.getZ(), 4, 0.08D, 0.08D, 0.08D, 0.02D);
                    level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 2, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }

        private void updatePlayerSpeed(float progress) {
            double baseSpeed = 0.1D;
            double speedMultiplier;

            if (progress < 0.3f) {
                speedMultiplier = 2.5D;
            } else if (progress < 0.6f) {
                speedMultiplier = 3.5D;
            } else if (progress < 0.85f) {
                speedMultiplier = 5.0D;
            } else {
                speedMultiplier = 8.0D;
            }

            if (tickCounter % 10 == 0) {
                AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttribute != null) {
                    speedAttribute.removeModifier(SPEED_UUID);

                    double speedBoost = baseSpeed * (speedMultiplier - 1.0D);

                    AttributeModifier modifier = new AttributeModifier(
                            SPEED_UUID,
                            "made_in_heaven_speed",
                            speedBoost,
                            AttributeModifier.Operation.ADDITION
                    );
                    speedAttribute.addTransientModifier(modifier);
                }
            }
        }

        private void forceRemoveSpeed() {
            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.removeModifier(SPEED_UUID);

                var modifiers = new ArrayList<>(speedAttribute.getModifiers());
                for (AttributeModifier mod : modifiers) {
                    if (mod.getName().contains("made_in_heaven")) {
                        speedAttribute.removeModifier(mod);
                    }
                }

                speedAttribute.setBaseValue(0.1D);
            }

            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.refreshDimensions();
        }

        private void accelerateWeather(float progress, int timeIncrease) {
            int weatherIncrease = timeIncrease / 8;
            weatherTickCounter += weatherIncrease;

            int weatherChangeDuration = 4000 + random.nextInt(2000);

            if (weatherTickCounter >= weatherChangeDuration) {
                weatherTickCounter = 0;

                int weatherType = random.nextInt(10);

                if (weatherType < 7) {
                    level.setWeatherParameters(weatherChangeDuration, 0, false, false);
                } else {
                    level.setWeatherParameters(0, weatherChangeDuration, true, false);
                }
            }

            if (progress > 0.8f && tickCounter % 30 == 0) {
                int quickWeather = random.nextInt(10);

                if (quickWeather < 6) {
                    level.setWeatherParameters(100, 0, false, false);
                } else {
                    level.setWeatherParameters(0, 100, true, false);
                }
            }
        }

        private void spawnDivineParticles(float progress) {
            double radius = 2.0D + progress * 5.0D;
            int particleCount = (int) (5 + progress * 20);

            for (int i = 0; i < particleCount; i++) {
                double angle = Math.random() * Math.PI * 2.0D;
                double verticalAngle = (Math.random() - 0.5D) * Math.PI;

                double x = player.getX() + Math.cos(angle) * Math.cos(verticalAngle) * radius;
                double y = player.getY() + 1.0D + Math.sin(verticalAngle) * radius;
                double z = player.getZ() + Math.sin(angle) * Math.cos(verticalAngle) * radius;

                double velX = (x - player.getX()) * 0.15D;
                double velY = (y - player.getY() - 1.0D) * 0.15D;
                double velZ = (z - player.getZ()) * 0.15D;

                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, velX, velY, velZ, 0.08D);

                if (progress > 0.5f && Math.random() < 0.2D) {
                    level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, velX, velY, velZ, 0.05D);
                }
            }
        }

        private void spawnSpiralEffects(float progress) {
            int spiralCount = 1 + (int) (progress * 2);

            for (int s = 0; s < spiralCount; s++) {
                double spiralOffset = (s / (double) spiralCount) * Math.PI * 2.0D;
                double angle = (tickCounter * 0.3D + spiralOffset) % (Math.PI * 2.0D);
                double height = (tickCounter % 40) * 0.3D;
                double radius = 2.0D + Math.sin(tickCounter * 0.1D) * 0.5D;

                double x = player.getX() + Math.cos(angle) * radius;
                double y = player.getY() + height;
                double z = player.getZ() + Math.sin(angle) * radius;

                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.05D, 0, 0);

                if (progress > 0.6f && Math.random() < 0.3D) {
                    level.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            x, y, z,
                            1,
                            (Math.random() - 0.5D) * 0.1D,
                            (Math.random() - 0.5D) * 0.1D,
                            (Math.random() - 0.5D) * 0.1D,
                            0
                    );
                }

                if (progress > 0.85f && tickCounter % 10 == 0) {
                    level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
                }
            }

            if (tickCounter % 6 == 0 && progress > 0.4f) {
                double ringRadius = 3.0D + progress * 2.0D;
                int points = 15 + (int) (progress * 10);

                for (int i = 0; i < points; i++) {
                    double angle = (i / (double) points) * Math.PI * 2.0D;
                    double x = player.getX() + Math.cos(angle) * ringRadius;
                    double z = player.getZ() + Math.sin(angle) * ringRadius;
                    double y = player.getY() + 1.0D + Math.sin(tickCounter * 0.2D) * 0.5D;

                    level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }

        private void spawnRandomLasers(float progress) {
            int laserCount = 2 + (int) (progress * 4);

            for (int l = 0; l < laserCount; l++) {
                double theta = random.nextDouble() * Math.PI * 2.0D;
                double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);

                double dirX = Math.sin(phi) * Math.cos(theta);
                double dirY = Math.sin(phi) * Math.sin(theta);
                double dirZ = Math.cos(phi);

                double length = 8.0D + progress * 6.0D;

                for (double d = 0; d < length; d += 0.2D) {
                    double x = player.getX() + dirX * d;
                    double y = player.getY() + 1.0D + dirY * d;
                    double z = player.getZ() + dirZ * d;

                    level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, dirX * 0.15D, dirY * 0.15D, dirZ * 0.15D, 0);

                    if (random.nextDouble() < 0.3D) {
                        level.sendParticles(
                                ParticleTypes.ELECTRIC_SPARK,
                                x + (random.nextDouble() - 0.5D) * 0.4D,
                                y + (random.nextDouble() - 0.5D) * 0.4D,
                                z + (random.nextDouble() - 0.5D) * 0.4D,
                                1, 0, 0, 0, 0
                        );
                    }

                    if (d > length - 1.0D) {
                        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        private void performUniverseReset() {
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new UniverseResetPacket());
            ZitraksMode.UNIVERSE_RESET.trigger(player);

            long noon = 6000;
            level.setDayTime(noon);

            for (ServerPlayer targetPlayer : level.players()) {
                targetPlayer.connection.send(new ClientboundSetTimePacket(
                        targetPlayer.getLevel().getGameTime(),
                        noon,
                        true
                ));
            }

            forceRemoveSpeed();
            giveRandomBuff();

            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§f§l⚡ СБРОС ВСЕЛЕННОЙ ⚡"),
                    false
            );

            resetBlocksAroundPlayer();
        }

        private void giveRandomBuff() {
            MobEffect[] possibleBuffs = {
                    MobEffects.REGENERATION,
                    MobEffects.DAMAGE_RESISTANCE,
                    MobEffects.FIRE_RESISTANCE,
                    MobEffects.WATER_BREATHING,
                    MobEffects.NIGHT_VISION,
                    MobEffects.HEALTH_BOOST,
                    MobEffects.ABSORPTION,
                    MobEffects.SATURATION,
                    MobEffects.LUCK,
                    MobEffects.SLOW_FALLING,
                    MobEffects.HERO_OF_THE_VILLAGE,
                    MobEffects.DOLPHINS_GRACE
            };

            MobEffect randomBuff = possibleBuffs[random.nextInt(possibleBuffs.length)];
            player.addEffect(new MobEffectInstance(randomBuff, 2400, 1, false, true, true));

            player.sendSystemMessage(Component.literal("§e✨ Получен эффект вселенной: §6" +
                    randomBuff.getDisplayName().getString()));
        }

        private boolean isSafeResetBlock(Block block) {
            if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) return false;
            if (block == Blocks.WATER || block == Blocks.LAVA || block == Blocks.BUBBLE_COLUMN) return false;

            if (block == Blocks.BEDROCK
                    || block == Blocks.END_PORTAL
                    || block == Blocks.END_PORTAL_FRAME
                    || block == Blocks.NETHER_PORTAL
                    || block == Blocks.END_GATEWAY
                    || block == Blocks.BARRIER
                    || block == Blocks.COMMAND_BLOCK
                    || block == Blocks.CHAIN_COMMAND_BLOCK
                    || block == Blocks.REPEATING_COMMAND_BLOCK
                    || block == Blocks.STRUCTURE_BLOCK
                    || block == Blocks.STRUCTURE_VOID
                    || block == Blocks.JIGSAW
                    || block == Blocks.SCULK_SHRIEKER) {
                return false;
            }

            ResourceLocation key = Registry.BLOCK.getKey(block);
            if (key == null) return false;

            String namespace = key.getNamespace();
            if (!namespace.equals("minecraft") && !namespace.equals(ZitraksMode.MODID)) {
                return false;
            }

            BlockStateLike state = new BlockStateLike(
                    block.defaultBlockState().getMaterial() != Material.AIR,
                    !block.defaultBlockState().getFluidState().isEmpty(),
                    block.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)
            );

            if (state.hasFluid) return false;
            if (state.hasWaterloggedProperty) return false;

            String path = key.getPath();
            if (path.contains("door")
                    || path.contains("trapdoor")
                    || path.contains("bed")
                    || path.contains("banner")
                    || path.contains("sign")
                    || path.contains("rail")
                    || path.contains("button")
                    || path.contains("pressure_plate")
                    || path.contains("torch")
                    || path.contains("candle")
                    || path.contains("coral")
                    || path.contains("kelp")
                    || path.contains("seagrass")
                    || path.contains("pickle")
                    || path.contains("vine")
                    || path.contains("tripwire")
                    || path.contains("hook")
                    || path.contains("crop")
                    || path.contains("farmland")
                    || path.contains("flower")
                    || path.contains("tulip")
                    || path.contains("daisy")
                    || path.contains("rose")
                    || path.contains("sapling")
                    || path.contains("mushroom")
                    || path.contains("carpet")
                    || path.contains("skull")
                    || path.contains("head")
                    || path.contains("bush")
                    || path.contains("pumpkin")) {
                return false;
            }

            return state.blocksMotion;
        }

        private Block pickYobyrMamokBlock() {
            if (random.nextFloat() < 0.80f) {
                return ModBlocks.HENTAI_BLOCK.get();
            }

            Block[] mats = new Block[]{
                    Blocks.END_ROD,
                    Blocks.BLACK_WOOL,
                    Blocks.ORANGE_WOOL,
                    Blocks.QUARTZ_BLOCK,
                    Blocks.POPPY,
                    Blocks.RED_TULIP,
                    Blocks.ROSE_BUSH,
                    Blocks.CACTUS,
                    Blocks.LAPIS_BLOCK
            };
            return mats[random.nextInt(mats.length)];
        }

        private Block pickLabubuBlock(List<Block> randomLabubus) {
            int roll = random.nextInt(100);
            if (roll < 35) return Blocks.STONE;
            if (roll < 55) return Blocks.GOLD_BLOCK;
            if (roll < 75) return ModBlocks.LABUBU_BOX.get();
            if (roll < 82) return Blocks.POPPY;
            if (roll < 89) return Blocks.CACTUS;
            if (roll < 96) return Blocks.LAPIS_BLOCK;
            if (!randomLabubus.isEmpty()) {
                return randomLabubus.get(random.nextInt(randomLabubus.size()));
            }
            return Blocks.STONE;
        }

        private List<Block> buildRandomLabubuSelection() {
            List<Block> all = new ArrayList<>();
            for (RegistryObject<Block> obj : ModBlocks.LABUBU_FIGURINE_BLOCKS.values()) {
                all.add(obj.get());
            }

            List<Block> result = new ArrayList<>();
            if (all.isEmpty()) {
                return result;
            }

            for (int i = 0; i < 10; i++) {
                result.add(all.get(random.nextInt(all.size())));
            }
            return result;
        }

        private void placeResetBlock(BlockPos pos, Block block) {
            level.setBlock(pos, block.defaultBlockState(), 3);

            if (block == ModBlocks.HENTAI_BLOCK.get()) {
                if (level.getBlockEntity(pos) instanceof HentaiBlockEntity hentai) {
                    hentai.randomizeTextures(level.random);
                    hentai.setChanged();
                    level.sendBlockUpdated(pos, hentai.getBlockState(), hentai.getBlockState(), 3);
                }
            }
        }

        private void placeRandomLabubuFigurines(BlockPos playerPos, int maxRadius, int count, List<Block> figurines) {
            if (figurines.isEmpty()) {
                return;
            }

            for (int i = 0; i < count; i++) {
                int x = random.nextInt(maxRadius * 2 + 1) - maxRadius;
                int y = random.nextInt(5) - 2;
                int z = random.nextInt(maxRadius * 2 + 1) - maxRadius;
                BlockPos pos = playerPos.offset(x, y, z);

                Block stateBlock = level.getBlockState(pos).getBlock();
                if (!isSafeResetBlock(stateBlock)) {
                    continue;
                }

                Block block = figurines.get(random.nextInt(figurines.size()));
                placeResetBlock(pos, block);
            }
        }

        private void resetBlocksAroundPlayer() {
            BlockPos playerPos = player.blockPosition();
            List<Block> safeBlocks = new ArrayList<>();

            for (Block block : Registry.BLOCK) {
                if (isSafeResetBlock(block)) {
                    safeBlocks.add(block);
                }
            }

            if (safeBlocks.isEmpty()) return;

            double divisor;
            if (yobyrMamok && hochuLabubu) {
                divisor = 2.0D; // оба чара -> в 2 раза меньше обычного
            } else if (yobyrMamok || hochuLabubu) {
                divisor = 4.0D; // один чар -> в 4 раза меньше обычного
            } else {
                divisor = 1.0D; // без чар -> обычный радиус
            }

            double coreRadius = 8.0D / divisor;
            double maxRadius = 20.0D / divisor;
            int loopRadius = Math.max(1, (int) Math.ceil(maxRadius));

            List<Block> randomLabubus = hochuLabubu ? buildRandomLabubuSelection() : List.of();

            for (int x = -loopRadius; x <= loopRadius; x++) {
                for (int y = -loopRadius; y <= loopRadius; y++) {
                    for (int z = -loopRadius; z <= loopRadius; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);

                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance > maxRadius) {
                            continue;
                        }

                        double chance;
                        if (distance <= coreRadius) {
                            chance = 1.0D;
                        } else {
                            double blocksOutside = distance - coreRadius;
                            chance = 1.0D - (blocksOutside * 0.05D);
                            if (chance < 0.0D) chance = 0.0D;
                        }

                        if (random.nextDouble() > chance) {
                            continue;
                        }

                        Block currentBlock = level.getBlockState(pos).getBlock();
                        if (!isSafeResetBlock(currentBlock)) {
                            continue;
                        }

                        Block randomBlock;
                        if (yobyrMamok && hochuLabubu) {
                            randomBlock = random.nextBoolean() ? pickYobyrMamokBlock() : pickLabubuBlock(randomLabubus);
                        } else if (yobyrMamok) {
                            randomBlock = pickYobyrMamokBlock();
                        } else if (hochuLabubu) {
                            randomBlock = pickLabubuBlock(randomLabubus);
                        } else {
                            randomBlock = safeBlocks.get(random.nextInt(safeBlocks.size()));
                        }

                        placeResetBlock(pos, randomBlock);
                    }
                }
            }

            if (hochuLabubu) {
                placeRandomLabubuFigurines(playerPos, loopRadius, 10, randomLabubus);
            }

            for (int i = 0; i < 100; i++) {
                double x = playerPos.getX() + (random.nextDouble() - 0.5D) * coreRadius * 3.0D;
                double y = playerPos.getY() + (random.nextDouble() - 0.5D) * coreRadius * 3.0D;
                double z = playerPos.getZ() + (random.nextDouble() - 0.5D) * coreRadius * 3.0D;

                level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);

                if (i % 2 == 0) {
                    level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }

        private static class Vec3Like {
            final double x;
            final double y;
            final double z;

            private Vec3Like(double x, double y, double z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }

        private static class BlockStateLike {
            final boolean blocksMotion;
            final boolean hasFluid;
            final boolean hasWaterloggedProperty;

            private BlockStateLike(boolean blocksMotion, boolean hasFluid, boolean hasWaterloggedProperty) {
                this.blocksMotion = blocksMotion;
                this.hasFluid = hasFluid;
                this.hasWaterloggedProperty = hasWaterloggedProperty;
            }
        }
    }
}