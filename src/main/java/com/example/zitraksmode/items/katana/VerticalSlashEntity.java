package com.example.zitraksmode.items.katana;

import com.example.zitraksmode.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class VerticalSlashEntity
        extends Projectile
        implements IEntityAdditionalSpawnData {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    private static final Random DROP_RANDOM =
            new Random();

    private static final double BASE_MAX_DISTANCE =
            35.0D;

    private static final float DAMAGE =
            25.0F;

    private static final double SPEED =
            4.5D;

    private static final double GRAVITY =
            0.0D;

    private static final double DRAG =
            1.0D;

    /*
     * Обычные блоки имеют шанс 50% на выпадение.
     * Особые блоки обрабатываются отдельно и выпадают гарантированно.
     */
    private static final float NORMAL_BLOCK_DROP_CHANCE =
            0.5F;

    private static final double HALF_WIDTH =
            1.5D;

    private static final double LOWER =
            0.0D;

    private static final double BASE_UPPER =
            10.0D;

    public static final double ARC_BULGE =
            3.5D;

    private static final double ARC_THICKNESS =
            1.1D;

    private Vec3 startPos;
    private Vec3 direction;

    private final Set<BlockPos> brokenBlocks =
            new HashSet<>();

    private final Set<UUID> hitEntities =
            new HashSet<>();

    private int worldSlashLevel =
            0;

    private boolean miningIndustry =
            false;

    private double maxDistance =
            BASE_MAX_DISTANCE;

    private double upperExtent =
            BASE_UPPER;

    public VerticalSlashEntity(
            EntityType<? extends Projectile> type,
            Level level
    ) {
        super(type, level);
        this.setNoGravity(true);
    }

    public VerticalSlashEntity(
            Level level,
            LivingEntity shooter,
            Vec3 direction,
            int worldSlashLevel,
            boolean miningIndustry
    ) {
        super(
                ModEntities.VERTICAL_SLASH.get(),
                level
        );

        this.setNoGravity(true);
        this.setOwner(shooter);

        this.direction =
                direction.normalize();

        this.worldSlashLevel =
                Math.max(
                        0,
                        Math.min(
                                5,
                                worldSlashLevel
                        )
                );

        this.miningIndustry =
                miningIndustry;

        applyEnchantScaling();

        this.startPos =
                new Vec3(
                        shooter.getX()
                                + this.direction.x * 1.0D,
                        shooter.getY() + 0.1D,
                        shooter.getZ()
                                + this.direction.z * 1.0D
                );

        this.setPos(
                this.startPos.x,
                this.startPos.y,
                this.startPos.z
        );

        this.setDeltaMovement(
                this.direction.scale(SPEED)
        );

        this.setRot(
                shooter.getYRot(),
                shooter.getXRot()
        );

        LOGGER.info(
                "ULTIMATE SLASH SPAWNED | Direction: {} | WorldSlash={} | MiningIndustry={}",
                this.direction,
                this.worldSlashLevel,
                this.miningIndustry
        );
    }

    private void applyEnchantScaling() {
        this.maxDistance =
                BASE_MAX_DISTANCE
                        + this.worldSlashLevel * 5.0D;

        this.upperExtent =
                BASE_UPPER
                        + this.worldSlashLevel * 2.0D;
    }

    @Override
    public void writeSpawnData(
            FriendlyByteBuf buffer
    ) {
        Vec3 start =
                this.startPos == null
                        ? this.position()
                        : this.startPos;

        Vec3 dir =
                this.direction == null
                        ? this.getDeltaMovement().normalize()
                        : this.direction;

        buffer.writeDouble(start.x);
        buffer.writeDouble(start.y);
        buffer.writeDouble(start.z);

        buffer.writeDouble(dir.x);
        buffer.writeDouble(dir.y);
        buffer.writeDouble(dir.z);

        buffer.writeFloat(this.getYRot());
        buffer.writeFloat(this.getXRot());

        buffer.writeInt(
                this.worldSlashLevel
        );

        buffer.writeBoolean(
                this.miningIndustry
        );

        buffer.writeDouble(
                this.maxDistance
        );

        buffer.writeDouble(
                this.upperExtent
        );
    }

    @Override
    public void readSpawnData(
            FriendlyByteBuf buffer
    ) {
        this.startPos =
                new Vec3(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                );

        this.direction =
                new Vec3(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                );

        float yRot =
                buffer.readFloat();

        float xRot =
                buffer.readFloat();

        this.setYRot(yRot);
        this.setXRot(xRot);

        this.yRotO =
                yRot;

        this.xRotO =
                xRot;

        this.worldSlashLevel =
                buffer.readInt();

        this.miningIndustry =
                buffer.readBoolean();

        this.maxDistance =
                buffer.readDouble();

        this.upperExtent =
                buffer.readDouble();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.direction == null
                || this.direction.lengthSqr() < 1.0E-6D) {

            Vec3 motion =
                    this.getDeltaMovement();

            if (motion.lengthSqr() > 1.0E-6D) {
                this.direction =
                        motion.normalize();
            }
        }

        if (this.startPos == null) {
            this.startPos =
                    this.position();
        }

        if (this.direction == null
                || this.direction.lengthSqr() < 1.0E-6D) {
            this.discard();
            return;
        }

        double travelled =
                this.position().distanceTo(
                        this.startPos
                );

        if (travelled > this.maxDistance) {
            this.discard();
            return;
        }

        Vec3 currentPos =
                this.position();

        Vec3 motion =
                this.getDeltaMovement();

        this.setDeltaMovement(
                motion.x * DRAG,
                motion.y - GRAVITY,
                motion.z * DRAG
        );

        Vec3 nextPos =
                currentPos.add(
                        this.getDeltaMovement()
                );

        breakBlocksAlongArc(
                currentPos,
                nextPos
        );

        damageEntitiesAlongArc(
                currentPos,
                nextPos
        );

        this.setPos(nextPos.x, nextPos.y, nextPos.z);
    }

    public Vec3 getDirectionForRender(
            float partialTicks
    ) {
        if (this.direction != null
                && this.direction.lengthSqr() > 1.0E-8D) {
            return this.direction.normalize();
        }

        Vec3 motion =
                this.getDeltaMovement();

        if (motion.lengthSqr() > 1.0E-8D) {
            return motion.normalize();
        }

        float yawRadians =
                (float) Math.toRadians(
                        this.getYRot()
                );

        float pitchRadians =
                (float) Math.toRadians(
                        this.getXRot()
                );

        double x =
                -Math.sin(yawRadians)
                        * Math.cos(pitchRadians);

        double y =
                -Math.sin(pitchRadians);

        double z =
                Math.cos(yawRadians)
                        * Math.cos(pitchRadians);

        return new Vec3(x, y, z).normalize();
    }

    public double getTravelProgressForRender(
            float partialTicks
    ) {
        if (this.startPos == null) {
            return 0.0D;
        }

        double x =
                Mth.lerp(
                        partialTicks,
                        this.xo,
                        this.getX()
                );

        double y =
                Mth.lerp(
                        partialTicks,
                        this.yo,
                        this.getY()
                );

        double z =
                Mth.lerp(
                        partialTicks,
                        this.zo,
                        this.getZ()
                );

        Vec3 position =
                new Vec3(x, y, z);

        double distance =
                position.distanceTo(
                        this.startPos
                );

        return Math.min(
                1.0D,
                Math.max(
                        0.0D,
                        distance / this.maxDistance
                )
        );
    }

    public double getHalfWidthForRender(
            double travelProgress
    ) {
        return HALF_WIDTH;
    }

    public double getLowerExtentForRender(
            double travelProgress
    ) {
        return LOWER;
    }

    public double getUpperExtentForRender(
            double travelProgress
    ) {
        return upperExtent;
    }

    public float getStoredYawForRender() {
        return this.getYRot();
    }

    private Basis computeBasis(
            Vec3 forward
    ) {
        Vec3 f =
                forward.normalize();

        Vec3 worldUp =
                new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 right;

        if (Math.abs(f.y) > 0.97D) {
            float yawRadians =
                    (float) Math.toRadians(
                            this.getYRot()
                    );

            Vec3 horizontalForward =
                    new Vec3(
                            -Math.sin(yawRadians),
                            0.0D,
                            Math.cos(yawRadians)
                    ).normalize();

            right =
                    horizontalForward
                            .cross(worldUp)
                            .normalize();

            if (right.lengthSqr() < 1.0E-6D) {
                right =
                        new Vec3(
                                1.0D,
                                0.0D,
                                0.0D
                        );
            }
        } else {
            right =
                    f.cross(worldUp);

            if (right.lengthSqr() < 1.0E-6D) {
                right =
                        new Vec3(
                                1.0D,
                                0.0D,
                                0.0D
                        );
            }

            right =
                    right.normalize();
        }

        Vec3 up =
                right.cross(f).normalize();

        return new Basis(
                f,
                right,
                up
        );
    }

    private void breakBlocksAlongArc(
            Vec3 from,
            Vec3 to
    ) {
        if (this.level.isClientSide) {
            return;
        }

        Vec3 segment =
                to.subtract(from);

        if (segment.lengthSqr() < 1.0E-8D) {
            return;
        }

        Basis basis =
                computeBasis(
                        segment.normalize()
                );

        double totalHeight =
                upperExtent - LOWER;

        if (totalHeight <= 0.0D) {
            return;
        }

        for (
                double t = 0.0D;
                t <= 1.0D;
                t += 0.1D
        ) {
            Vec3 base =
                    from.lerp(to, t);

            for (
                    double height = LOWER;
                    height <= upperExtent;
                    height += 0.75D
            ) {
                double verticalT =
                        (height - LOWER)
                                / totalHeight;

                double bulge =
                        Math.sin(
                                verticalT * Math.PI
                        ) * ARC_BULGE;

                Vec3 arcCenter =
                        base
                                .add(
                                        basis.up.scale(
                                                height
                                        )
                                )
                                .add(
                                        basis.forward.scale(
                                                bulge
                                        )
                                );

                for (
                        double width = -HALF_WIDTH;
                        width <= HALF_WIDTH;
                        width += 0.75D
                ) {
                    for (
                            double depth = -0.30D;
                            depth <= 0.30D;
                            depth += 0.30D
                    ) {
                        Vec3 point =
                                arcCenter
                                        .add(
                                                basis.right.scale(
                                                        width
                                                )
                                        )
                                        .add(
                                                basis.forward.scale(
                                                        depth
                                                )
                                        );

                        breakBlock(
                                new BlockPos(
                                        Mth.floor(point.x),
                                        Mth.floor(point.y),
                                        Mth.floor(point.z)
                                )
                        );
                    }
                }
            }
        }
    }

    private void breakBlock(
            BlockPos pos
    ) {
        if (brokenBlocks.contains(pos)) {
            return;
        }

        BlockState state =
                this.level.getBlockState(pos);

        if (state.isAir()) {
            return;
        }

        float hardness =
                state.getDestroySpeed(
                        this.level,
                        pos
                );

        if (hardness < 0.0F
                || hardness >= 40.0F) {
            return;
        }

        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.BARRIER)) {
            return;
        }

        /*
         * BlockEntity-блоки и важные блоки всегда
         * ломаются с гарантированным выпадением.
         */
        if (isGuaranteedBlock(pos, state)) {
            dropGuaranteedBlock(
                    pos,
                    state
            );

            brokenBlocks.add(pos);
            return;
        }

        /*
         * Mining Industry сохраняет особую обработку руд.
         */
        if (miningIndustry
                && isOreBlock(state)) {

            mineWithIndustry(
                    pos,
                    state
            );

            brokenBlocks.add(pos);
            return;
        }

        /*
         * В обычном режиме остальные блоки имеют
         * старый шанс выпадения 50 на 50, но теперь
         * они симулируют ломание незеритовой киркой и выбрасывают опыт.
         */
        if (!miningIndustry) {
            boolean shouldDrop =
                    DROP_RANDOM.nextFloat()
                            < NORMAL_BLOCK_DROP_CHANCE;

            if (shouldDrop && this.level instanceof ServerLevel serverLevel) {
                ItemStack fakePickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
                
                // Вычисляем опыт до разрушения блока
                int exp = state.getExpDrop(serverLevel, serverLevel.getRandom(), pos, 0, 0);
                
                List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null, this.getOwner(), fakePickaxe);
                
                // Ломаем блок (оставляем эффекты частиц/звука разрушения, но дроп выключен)
                this.level.destroyBlock(pos, false);
                
                // Вручную спавним предметы
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        Block.popResource(serverLevel, pos, drop);
                    }
                }
                
                // Вручную спавним сферы опыта, если он есть
                if (exp > 0) {
                    state.getBlock().popExperience(serverLevel, pos, exp);
                }
            } else {
                this.level.destroyBlock(pos, false);
            }
        } else {
            /*
             * Mining Industry не выбрасывает обычные
             * неважные блоки, но важные уже обработаны выше.
             */
            this.level.removeBlock(
                    pos,
                    false
            );
        }

        brokenBlocks.add(pos);
    }

    /**
     * Проверяет, должен ли блок выпасть на 100%.
     */
    private boolean isGuaranteedBlock(
            BlockPos pos,
            BlockState state
    ) {
        /*
         * Любой BlockEntity-блок выпадает гарантированно.
         */
        if (this.level.getBlockEntity(pos) != null) {
            return true;
        }

        /*
         * Важные обычные блоки.
         */
        return state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL)
                || state.is(Blocks.BEACON)
                || state.is(Blocks.CONDUIT)
                || state.is(Blocks.LODESTONE)
                || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(Blocks.DRAGON_EGG)
                || state.is(Blocks.BREWING_STAND)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.JUKEBOX)
                || state.is(Blocks.BELL)
                || state.is(Blocks.CAKE);
    }

    /**
     * Ломает BlockEntity или важный блок и вручную
     * гарантирует выпадение предмета + опыт (если есть).
     */
    private void dropGuaranteedBlock(
            BlockPos pos,
            BlockState state
    ) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            this.level.removeBlock(
                    pos,
                    false
            );
            return;
        }

        BlockEntity blockEntity =
                serverLevel.getBlockEntity(pos);

        /*
         * Создаём искусственную незеритовую кирку
         * с Шёлковым касанием.
         */
        ItemStack silkPickaxe =
                new ItemStack(
                        Items.NETHERITE_PICKAXE
                );

        EnchantmentHelper.setEnchantments(
                java.util.Map.of(
                        Enchantments.SILK_TOUCH,
                        1
                ),
                silkPickaxe
        );

        // Получаем опыт (с учетом шелкового касания)
        int exp = state.getExpDrop(serverLevel, serverLevel.getRandom(), pos, 0, 1);

        List<ItemStack> drops =
                Block.getDrops(
                        state,
                        serverLevel,
                        pos,
                        blockEntity,
                        this.getOwner(),
                        silkPickaxe
                );

        /*
         * Некоторые BlockEntity, например спавнер,
         * в обычной loot table ничего не возвращают.
         * В таком случае создаём предмет блока вручную.
         */
        if (drops.isEmpty()
                && state.getBlock().asItem()
                != Items.AIR) {

            drops =
                    List.of(
                            new ItemStack(
                                    state.getBlock().asItem()
                            )
                    );
        }

        serverLevel.removeBlock(
                pos,
                false
        );

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            Block.popResource(
                    serverLevel,
                    pos,
                    drop
            );
        }
        
        // Спавним опыт
        if (exp > 0) {
            state.getBlock().popExperience(serverLevel, pos, exp);
        }
    }

    private void mineWithIndustry(
            BlockPos pos,
            BlockState state
    ) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            this.level.removeBlock(
                    pos,
                    false
            );
            return;
        }

        ItemStack fakePickaxe =
                new ItemStack(
                        Items.NETHERITE_PICKAXE
                );

        BlockEntity blockEntity =
                serverLevel.getBlockEntity(pos);

        // Рассчитываем опыт ДО удаления блока
        int exp = state.getExpDrop(serverLevel, serverLevel.getRandom(), pos, 0, 0);

        List<ItemStack> drops =
                Block.getDrops(
                        state,
                        serverLevel,
                        pos,
                        blockEntity,
                        this.getOwner(),
                        fakePickaxe
                );

        serverLevel.removeBlock(
                pos,
                false
        );

        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                Block.popResource(
                        serverLevel,
                        pos,
                        drop
                );
            }
        }
        
        // Спавним сферы опыта
        if (exp > 0) {
            state.getBlock().popExperience(serverLevel, pos, exp);
        }
    }

    private static boolean isOreBlock(
            BlockState state
    ) {
        return state.is(Tags.Blocks.ORES)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    private static boolean isValuableBlock(
            BlockState state
    ) {
        if (state.is(Tags.Blocks.ORES)) {
            return true;
        }

        if (state.is(Tags.Blocks.STORAGE_BLOCKS)) {
            return true;
        }

        return state.is(
                Blocks.ANCIENT_DEBRIS
        );
    }

    private void damageEntitiesAlongArc(
            Vec3 from,
            Vec3 to
    ) {
        if (this.level.isClientSide) {
            return;
        }

        Vec3 segment =
                to.subtract(from);

        if (segment.lengthSqr() < 1.0E-8D) {
            return;
        }

        Basis basis =
                computeBasis(
                        segment.normalize()
                );

        double heightInflate =
                Math.max(
                        8.0D,
                        upperExtent - LOWER + 4.0D
                );

        List<LivingEntity> entities =
                this.level.getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(
                                8.0D,
                                heightInflate,
                                8.0D
                        ),
                        entity ->
                                entity != this.getOwner()
                                        && !hitEntities.contains(
                                                entity.getUUID()
                                        )
                                        && entity.isAlive()
                );

        for (LivingEntity entity : entities) {
            Vec3 center =
                    entity.getBoundingBox().getCenter();

            if (!isInsideArc(
                    center,
                    from,
                    to,
                    basis
            )) {
                continue;
            }

            DamageSource source;

            if (this.getOwner()
                    instanceof Player player) {

                source =
                        DamageSource.playerAttack(
                                player
                        );
            } else if (this.getOwner()
                    instanceof LivingEntity owner) {

                source =
                        DamageSource.mobAttack(
                                owner
                        );
            } else {
                source =
                        DamageSource.GENERIC;
            }

            if (entity.hurt(
                    source,
                    DAMAGE
            )) {
                hitEntities.add(
                        entity.getUUID()
                );

                LOGGER.info(
                        "ULTIMATE HIT: {} for {} damage",
                        entity.getName().getString(),
                        DAMAGE
                );
            }
        }
    }

    private boolean isInsideArc(
            Vec3 point,
            Vec3 from,
            Vec3 to,
            Basis basis
    ) {
        double totalHeight =
                upperExtent - LOWER;

        if (totalHeight <= 0.0D) {
            return false;
        }

        for (
                double t = 0.0D;
                t <= 1.0D;
                t += 0.08D
        ) {
            Vec3 base =
                    from.lerp(to, t);

            Vec3 relative =
                    point.subtract(base);

            double height =
                    relative.dot(basis.up);

            if (height < LOWER - 0.8D
                    || height > upperExtent + 0.8D) {
                continue;
            }

            double verticalT =
                    (height - LOWER)
                            / totalHeight;

            verticalT =
                    Math.max(
                            0.0D,
                            Math.min(
                                    1.0D,
                                    verticalT
                            )
                    );

            double expectedBulge =
                    Math.sin(
                            verticalT * Math.PI
                    ) * ARC_BULGE;

            double widthCoord =
                    relative.dot(
                            basis.right
                    );

            double forwardCoord =
                    relative.dot(
                            basis.forward
                    );

            if (Math.abs(widthCoord)
                    <= HALF_WIDTH + 0.7D
                    && Math.abs(
                            forwardCoord - expectedBulge
                    ) <= ARC_THICKNESS) {

                return true;
            }
        }

        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void addAdditionalSaveData(
            net.minecraft.nbt.CompoundTag tag
    ) {
        if (this.startPos != null) {
            tag.putDouble(
                    "StartX",
                    this.startPos.x
            );

            tag.putDouble(
                    "StartY",
                    this.startPos.y
            );

            tag.putDouble(
                    "StartZ",
                    this.startPos.z
            );
        }

        if (this.direction != null) {
            tag.putDouble(
                    "DirX",
                    this.direction.x
            );

            tag.putDouble(
                    "DirY",
                    this.direction.y
            );

            tag.putDouble(
                    "DirZ",
                    this.direction.z
            );
        }

        tag.putInt(
                "WorldSlashLevel",
                this.worldSlashLevel
        );

        tag.putBoolean(
                "MiningIndustry",
                this.miningIndustry
        );

        tag.putDouble(
                "MaxDistance",
                this.maxDistance
        );

        tag.putDouble(
                "UpperExtent",
                this.upperExtent
        );
    }

    @Override
    protected void readAdditionalSaveData(
            net.minecraft.nbt.CompoundTag tag
    ) {
        if (tag.contains("StartX")) {
            this.startPos =
                    new Vec3(
                            tag.getDouble("StartX"),
                            tag.getDouble("StartY"),
                            tag.getDouble("StartZ")
                    );
        }

        if (tag.contains("DirX")) {
            this.direction =
                    new Vec3(
                            tag.getDouble("DirX"),
                            tag.getDouble("DirY"),
                            tag.getDouble("DirZ")
                    );
        }

        this.worldSlashLevel =
                tag.getInt(
                        "WorldSlashLevel"
                );

        this.miningIndustry =
                tag.getBoolean(
                        "MiningIndustry"
                );

        this.maxDistance =
                tag.contains("MaxDistance")
                        ? tag.getDouble("MaxDistance")
                        : BASE_MAX_DISTANCE;

        this.upperExtent =
                tag.contains("UpperExtent")
                        ? tag.getDouble("UpperExtent")
                        : BASE_UPPER;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(
                this
        );
    }

    private record Basis(
            Vec3 forward,
            Vec3 right,
            Vec3 up
    ) {
    }
}