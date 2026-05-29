/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.NeoForgeConfig;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.server.timings.TimeTracker;
import org.jetbrains.annotations.ApiStatus;

public abstract class Level
extends AttachmentHolder
implements LevelAccessor,
AutoCloseable,
ILevelExtension {
    public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));
    public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    protected final NeighborUpdater neighborUpdater;
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    private final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    protected int randValue = RandomSource.create().nextInt();
    protected final int addend = 1013904223;
    public float oRainLevel;
    public float rainLevel;
    public float oThunderLevel;
    public float thunderLevel;
    public final RandomSource random = RandomSource.create();
    @Deprecated
    private final RandomSource threadSafeRandom = RandomSource.createThreadSafe();
    private final Holder<DimensionType> dimensionTypeRegistration;
    protected final WritableLevelData levelData;
    private final Supplier<ProfilerFiller> profiler;
    public final boolean isClientSide;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<Level> dimension;
    private final RegistryAccess registryAccess;
    private final DamageSources damageSources;
    private long subTickCount;
    public boolean restoringBlockSnapshots = false;
    public boolean captureBlockSnapshots = false;
    public ArrayList<BlockSnapshot> capturedBlockSnapshots = new ArrayList();
    private final ArrayList<BlockEntity> freshBlockEntities = new ArrayList();
    private final ArrayList<BlockEntity> pendingFreshBlockEntities = new ArrayList();
    private double maxEntityRadius = 2.0;

    protected Level(WritableLevelData arg, ResourceKey<Level> arg2, RegistryAccess arg3, Holder<DimensionType> arg4, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
        this.profiler = supplier;
        this.levelData = arg;
        this.dimensionTypeRegistration = arg4;
        final DimensionType dimensiontype = arg4.value();
        this.dimension = arg2;
        this.isClientSide = bl;
        this.worldBorder = dimensiontype.coordinateScale() != 1.0 ? new WorldBorder(this){

            @Override
            public double getCenterX() {
                return super.getCenterX() / dimensiontype.coordinateScale();
            }

            @Override
            public double getCenterZ() {
                return super.getCenterZ() / dimensiontype.coordinateScale();
            }
        } : new WorldBorder();
        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, l);
        this.isDebug = bl2;
        this.neighborUpdater = new CollectingNeighborUpdater(this, i);
        this.registryAccess = arg3;
        this.damageSources = new DamageSources(arg3);
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Override
    @Nullable
    public MinecraftServer getServer() {
        return null;
    }

    public boolean isInWorldBounds(BlockPos arg) {
        return !this.isOutsideBuildHeight(arg) && Level.isInWorldBoundsHorizontal(arg);
    }

    public static boolean isInSpawnableBounds(BlockPos arg) {
        return !Level.isOutsideSpawnableHeight(arg.getY()) && Level.isInWorldBoundsHorizontal(arg);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPos arg) {
        return arg.getX() >= -30000000 && arg.getZ() >= -30000000 && arg.getX() < 30000000 && arg.getZ() < 30000000;
    }

    private static boolean isOutsideSpawnableHeight(int i) {
        return i < -20000000 || i >= 20000000;
    }

    public LevelChunk getChunkAt(BlockPos arg) {
        return this.getChunk(SectionPos.blockToSectionCoord(arg.getX()), SectionPos.blockToSectionCoord(arg.getZ()));
    }

    @Override
    public LevelChunk getChunk(int i, int j) {
        return (LevelChunk)this.getChunk(i, j, ChunkStatus.FULL);
    }

    @Override
    @Nullable
    public ChunkAccess getChunk(int i, int j, ChunkStatus arg, boolean bl) {
        ChunkAccess chunkaccess = this.getChunkSource().getChunk(i, j, arg, bl);
        if (chunkaccess == null && bl) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        }
        return chunkaccess;
    }

    @Override
    public boolean setBlock(BlockPos arg, BlockState arg2, int i) {
        return this.setBlock(arg, arg2, i, 512);
    }

    @Override
    public boolean setBlock(BlockPos arg, BlockState arg2, int i, int j) {
        if (this.isOutsideBuildHeight(arg)) {
            return false;
        }
        if (!this.isClientSide && this.isDebug()) {
            return false;
        }
        LevelChunk levelchunk = this.getChunkAt(arg);
        Block block = arg2.getBlock();
        arg = arg.immutable();
        BlockSnapshot blockSnapshot = null;
        if (this.captureBlockSnapshots && !this.isClientSide) {
            blockSnapshot = BlockSnapshot.create(this.dimension, this, arg, i);
            this.capturedBlockSnapshots.add(blockSnapshot);
        }
        BlockState old = this.getBlockState(arg);
        int oldLight = old.getLightEmission(this, arg);
        int oldOpacity = old.getLightBlock(this, arg);
        BlockState blockstate = levelchunk.setBlockState(arg, arg2, (i & 0x40) != 0);
        if (blockstate == null) {
            if (blockSnapshot != null) {
                this.capturedBlockSnapshots.remove(blockSnapshot);
            }
            return false;
        }
        BlockState blockstate1 = this.getBlockState(arg);
        if (blockSnapshot == null) {
            this.markAndNotifyBlock(arg, levelchunk, blockstate, arg2, i, j);
        }
        return true;
    }

    public void markAndNotifyBlock(BlockPos arg, @Nullable LevelChunk levelchunk, BlockState blockstate, BlockState arg2, int j, int k) {
        Block block = arg2.getBlock();
        BlockState blockstate1 = this.getBlockState(arg);
        if (blockstate1 == arg2) {
            if (blockstate != blockstate1) {
                this.setBlocksDirty(arg, blockstate, blockstate1);
            }
            if ((j & 2) != 0 && (!this.isClientSide || (j & 4) == 0) && (this.isClientSide || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                this.sendBlockUpdated(arg, blockstate, arg2, j);
            }
            if ((j & 1) != 0) {
                this.blockUpdated(arg, blockstate.getBlock());
                if (!this.isClientSide && arg2.hasAnalogOutputSignal()) {
                    this.updateNeighbourForOutputSignal(arg, block);
                }
            }
            if ((j & 0x10) == 0 && k > 0) {
                int i = j & 0xFFFFFFDE;
                blockstate.updateIndirectNeighbourShapes(this, arg, i, k - 1);
                arg2.updateNeighbourShapes(this, arg, i, k - 1);
                arg2.updateIndirectNeighbourShapes(this, arg, i, k - 1);
            }
            this.onBlockStateChange(arg, blockstate, blockstate1);
            arg2.onBlockStateChange(this, arg, blockstate);
        }
    }

    public void onBlockStateChange(BlockPos arg, BlockState arg2, BlockState arg3) {
    }

    @Override
    public boolean removeBlock(BlockPos arg, boolean bl) {
        FluidState fluidstate = this.getFluidState(arg);
        return this.setBlock(arg, fluidstate.createLegacyBlock(), 3 | (bl ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPos arg, boolean bl, @Nullable Entity arg2, int i) {
        boolean flag;
        BlockState blockstate = this.getBlockState(arg);
        if (blockstate.isAir()) {
            return false;
        }
        FluidState fluidstate = this.getFluidState(arg);
        if (!(blockstate.getBlock() instanceof BaseFireBlock)) {
            this.levelEvent(2001, arg, Block.getId(blockstate));
        }
        if (bl) {
            BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(arg) : null;
            Block.dropResources(blockstate, this, arg, blockentity, arg2, ItemStack.EMPTY);
        }
        if (flag = this.setBlock(arg, fluidstate.createLegacyBlock(), 3, i)) {
            this.gameEvent(GameEvent.BLOCK_DESTROY, arg, GameEvent.Context.of(arg2, blockstate));
        }
        return flag;
    }

    public void addDestroyBlockEffect(BlockPos arg, BlockState arg2) {
    }

    public boolean setBlockAndUpdate(BlockPos arg, BlockState arg2) {
        return this.setBlock(arg, arg2, 3);
    }

    public abstract void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4);

    public void setBlocksDirty(BlockPos arg, BlockState arg2, BlockState arg3) {
    }

    public void updateNeighborsAt(BlockPos arg, Block arg2) {
        EventHooks.onNeighborNotify(this, arg, this.getBlockState(arg), EnumSet.allOf(Direction.class), false).isCanceled();
    }

    public void updateNeighborsAtExceptFromFacing(BlockPos arg, Block arg2, Direction arg3) {
    }

    public void neighborChanged(BlockPos arg, Block arg2, BlockPos arg3) {
    }

    public void neighborChanged(BlockState arg, BlockPos arg2, Block arg3, BlockPos arg4, boolean bl) {
    }

    @Override
    public void neighborShapeChanged(Direction arg, BlockState arg2, BlockPos arg3, BlockPos arg4, int i, int j) {
        this.neighborUpdater.shapeUpdate(arg, arg2, arg3, arg4, i, j);
    }

    @Override
    public int getHeight(Heightmap.Types arg, int j, int k) {
        int i = j >= -30000000 && k >= -30000000 && j < 30000000 && k < 30000000 ? (this.hasChunk(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k)) ? this.getChunk(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k)).getHeight(arg, j & 0xF, k & 0xF) + 1 : this.getMinBuildHeight()) : this.getSeaLevel() + 1;
        return i;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public BlockState getBlockState(BlockPos arg) {
        if (this.isOutsideBuildHeight(arg)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        LevelChunk levelchunk = this.getChunk(SectionPos.blockToSectionCoord(arg.getX()), SectionPos.blockToSectionCoord(arg.getZ()));
        return levelchunk.getBlockState(arg);
    }

    @Override
    public FluidState getFluidState(BlockPos arg) {
        if (this.isOutsideBuildHeight(arg)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        LevelChunk levelchunk = this.getChunkAt(arg);
        return levelchunk.getFluidState(arg);
    }

    public boolean isDay() {
        return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
    }

    public boolean isNight() {
        return !this.dimensionType().hasFixedTime() && !this.isDay();
    }

    public void playSound(@Nullable Entity arg, BlockPos arg2, SoundEvent arg3, SoundSource arg4, float f, float g) {
        Player player;
        this.playSound(arg instanceof Player ? (player = (Player)arg) : null, arg2, arg3, arg4, f, g);
    }

    @Override
    public void playSound(@Nullable Player arg, BlockPos arg2, SoundEvent arg3, SoundSource arg4, float f, float g) {
        this.playSound(arg, (double)arg2.getX() + 0.5, (double)arg2.getY() + 0.5, (double)arg2.getZ() + 0.5, arg3, arg4, f, g);
    }

    public abstract void playSeededSound(@Nullable Player var1, double var2, double var4, double var6, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12);

    public void playSeededSound(@Nullable Player arg, double d, double e, double f, SoundEvent arg2, SoundSource arg3, float g, float h, long l) {
        this.playSeededSound(arg, d, e, f, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(arg2), arg3, g, h, l);
    }

    public abstract void playSeededSound(@Nullable Player var1, Entity var2, Holder<SoundEvent> var3, SoundSource var4, float var5, float var6, long var7);

    public void playSound(@Nullable Player arg, double d, double e, double f, SoundEvent arg2, SoundSource arg3) {
        this.playSound(arg, d, e, f, arg2, arg3, 1.0f, 1.0f);
    }

    public void playSound(@Nullable Player arg, double d, double e, double f, SoundEvent arg2, SoundSource arg3, float g, float h) {
        this.playSeededSound(arg, d, e, f, arg2, arg3, g, h, this.threadSafeRandom.nextLong());
    }

    public void playSound(@Nullable Player arg, double d, double e, double f, Holder<SoundEvent> arg2, SoundSource arg3, float g, float h) {
        this.playSeededSound(arg, d, e, f, arg2, arg3, g, h, this.threadSafeRandom.nextLong());
    }

    public void playSound(@Nullable Player arg, Entity arg2, SoundEvent arg3, SoundSource arg4, float f, float g) {
        this.playSeededSound(arg, arg2, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(arg3), arg4, f, g, this.threadSafeRandom.nextLong());
    }

    public void playLocalSound(BlockPos arg, SoundEvent arg2, SoundSource arg3, float f, float g, boolean bl) {
        this.playLocalSound((double)arg.getX() + 0.5, (double)arg.getY() + 0.5, (double)arg.getZ() + 0.5, arg2, arg3, f, g, bl);
    }

    public void playLocalSound(Entity arg, SoundEvent arg2, SoundSource arg3, float f, float g) {
    }

    public void playLocalSound(double d, double e, double f, SoundEvent arg, SoundSource arg2, float g, float h, boolean bl) {
    }

    @Override
    public void addParticle(ParticleOptions arg, double d, double e, double f, double g, double h, double i) {
    }

    public void addParticle(ParticleOptions arg, boolean bl, double d, double e, double f, double g, double h, double i) {
    }

    public void addAlwaysVisibleParticle(ParticleOptions arg, double d, double e, double f, double g, double h, double i) {
    }

    public void addAlwaysVisibleParticle(ParticleOptions arg, boolean bl, double d, double e, double f, double g, double h, double i) {
    }

    public float getSunAngle(float g) {
        float f = this.getTimeOfDay(g);
        return f * ((float)Math.PI * 2);
    }

    public void addBlockEntityTicker(TickingBlockEntity arg) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(arg);
    }

    public void addFreshBlockEntities(Collection<BlockEntity> beList) {
        if (this.tickingBlockEntities) {
            this.pendingFreshBlockEntities.addAll(beList);
        } else {
            this.freshBlockEntities.addAll(beList);
        }
    }

    protected void tickBlockEntities() {
        ProfilerFiller profilerfiller = this.getProfiler();
        profilerfiller.push("blockEntities");
        if (!this.pendingFreshBlockEntities.isEmpty()) {
            this.freshBlockEntities.addAll(this.pendingFreshBlockEntities);
            this.pendingFreshBlockEntities.clear();
        }
        this.tickingBlockEntities = true;
        if (!this.freshBlockEntities.isEmpty()) {
            this.freshBlockEntities.forEach(blockEntity -> {
                if (!blockEntity.isRemoved() && blockEntity.hasLevel()) {
                    blockEntity.onLoad();
                }
            });
            this.freshBlockEntities.clear();
        }
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }
        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();
        boolean flag = this.tickRateManager().runsNormally();
        while (iterator.hasNext()) {
            TickingBlockEntity tickingblockentity = iterator.next();
            if (tickingblockentity.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!flag || !this.shouldTickBlocksAt(tickingblockentity.getPos())) continue;
            tickingblockentity.tick();
        }
        this.tickingBlockEntities = false;
        profilerfiller.pop();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public <T extends Entity> void guardEntityTick(Consumer<T> consumer, T arg) {
        block6: {
            try {
                TimeTracker.ENTITY_UPDATE.trackStart(arg);
                consumer.accept(arg);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking entity");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being ticked");
                arg.fillCrashReportCategory(crashreportcategory);
                if (((Boolean)NeoForgeConfig.SERVER.removeErroringEntities.get()).booleanValue()) {
                    LogUtils.getLogger().error("{}", (Object)crashreport.getFriendlyReport(ReportType.CRASH));
                    arg.discard();
                    break block6;
                }
                throw new ReportedException(crashreport);
            } finally {
                TimeTracker.ENTITY_UPDATE.trackEnd(arg);
            }
        }
    }

    public boolean shouldTickDeath(Entity arg) {
        return true;
    }

    public boolean shouldTickBlocksAt(long l) {
        return true;
    }

    public boolean shouldTickBlocksAt(BlockPos arg) {
        return this.shouldTickBlocksAt(ChunkPos.asLong(arg));
    }

    public Explosion explode(@Nullable Entity arg, double d, double e, double f, float g, ExplosionInteraction arg2) {
        return this.explode(arg, Explosion.getDefaultDamageSource(this, arg), null, d, e, f, g, false, arg2, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    public Explosion explode(@Nullable Entity arg, double d, double e, double f, float g, boolean bl, ExplosionInteraction arg2) {
        return this.explode(arg, Explosion.getDefaultDamageSource(this, arg), null, d, e, f, g, bl, arg2, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    public Explosion explode(@Nullable Entity arg, @Nullable DamageSource arg2, @Nullable ExplosionDamageCalculator arg3, Vec3 arg4, float f, boolean bl, ExplosionInteraction arg5) {
        return this.explode(arg, arg2, arg3, arg4.x(), arg4.y(), arg4.z(), f, bl, arg5, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    public Explosion explode(@Nullable Entity arg, @Nullable DamageSource arg2, @Nullable ExplosionDamageCalculator arg3, double d, double e, double f, float g, boolean bl, ExplosionInteraction arg4) {
        return this.explode(arg, arg2, arg3, d, e, f, g, bl, arg4, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
    }

    public Explosion explode(@Nullable Entity arg, @Nullable DamageSource arg2, @Nullable ExplosionDamageCalculator arg3, double d, double e, double f, float g, boolean bl, ExplosionInteraction arg4, ParticleOptions arg5, ParticleOptions arg6, Holder<SoundEvent> arg7) {
        return this.explode(arg, arg2, arg3, d, e, f, g, bl, arg4, true, arg5, arg6, arg7);
    }

    public Explosion explode(@Nullable Entity arg, @Nullable DamageSource arg2, @Nullable ExplosionDamageCalculator arg3, double d, double e, double f, float g, boolean bl, ExplosionInteraction arg4, boolean bl2, ParticleOptions arg5, ParticleOptions arg6, Holder<SoundEvent> arg7) {
        Explosion.BlockInteraction explosion$blockinteraction = switch (arg4.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> Explosion.BlockInteraction.KEEP;
            case 1 -> this.getDestroyType(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY);
            case 2 -> {
                if (EventHooks.canEntityGrief(this, arg)) {
                    yield this.getDestroyType(GameRules.RULE_MOB_EXPLOSION_DROP_DECAY);
                }
                yield Explosion.BlockInteraction.KEEP;
            }
            case 3 -> this.getDestroyType(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY);
            case 4 -> Explosion.BlockInteraction.TRIGGER_BLOCK;
        };
        Explosion explosion = new Explosion(this, arg, arg2, arg3, d, e, f, g, bl, explosion$blockinteraction, arg5, arg6, arg7);
        if (EventHooks.onExplosionStart(this, explosion)) {
            return explosion;
        }
        explosion.explode();
        explosion.finalizeExplosion(bl2);
        return explosion;
    }

    private Explosion.BlockInteraction getDestroyType(GameRules.Key<GameRules.BooleanValue> arg) {
        return this.getGameRules().getBoolean(arg) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
    }

    public abstract String gatherChunkSourceStats();

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos arg) {
        if (this.isOutsideBuildHeight(arg)) {
            return null;
        }
        return !this.isClientSide && Thread.currentThread() != this.thread ? null : this.getChunkAt(arg).getBlockEntity(arg, LevelChunk.EntityCreationType.IMMEDIATE);
    }

    public void setBlockEntity(BlockEntity arg) {
        BlockPos blockpos = arg.getBlockPos();
        if (!this.isOutsideBuildHeight(blockpos)) {
            this.getChunkAt(blockpos).addAndRegisterBlockEntity(arg);
        }
    }

    public void removeBlockEntity(BlockPos arg) {
        if (!this.isOutsideBuildHeight(arg)) {
            this.getChunkAt(arg).removeBlockEntity(arg);
        }
        this.updateNeighbourForOutputSignal(arg, this.getBlockState(arg).getBlock());
    }

    public boolean isLoaded(BlockPos arg) {
        return this.isOutsideBuildHeight(arg) ? false : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(arg.getX()), SectionPos.blockToSectionCoord(arg.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPos arg, Entity arg2, Direction arg3) {
        if (this.isOutsideBuildHeight(arg)) {
            return false;
        }
        ChunkAccess chunkaccess = this.getChunk(SectionPos.blockToSectionCoord(arg.getX()), SectionPos.blockToSectionCoord(arg.getZ()), ChunkStatus.FULL, false);
        return chunkaccess == null ? false : chunkaccess.getBlockState(arg).entityCanStandOnFace(this, arg, arg2, arg3);
    }

    public boolean loadedAndEntityCanStandOn(BlockPos arg, Entity arg2) {
        return this.loadedAndEntityCanStandOnFace(arg, arg2, Direction.UP);
    }

    public void updateSkyBrightness() {
        double d0 = 1.0 - (double)(this.getRainLevel(1.0f) * 5.0f) / 16.0;
        double d1 = 1.0 - (double)(this.getThunderLevel(1.0f) * 5.0f) / 16.0;
        double d2 = 0.5 + 2.0 * Mth.clamp((double)Mth.cos(this.getTimeOfDay(1.0f) * ((float)Math.PI * 2)), -0.25, 0.25);
        this.skyDarken = (int)((1.0 - d2 * d0 * d1) * 11.0);
    }

    public void setSpawnSettings(boolean bl, boolean bl2) {
        this.getChunkSource().setSpawnSettings(bl, bl2);
    }

    public BlockPos getSharedSpawnPos() {
        BlockPos blockpos = this.levelData.getSpawnPos();
        if (!this.getWorldBorder().isWithinBounds(blockpos)) {
            blockpos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(this.getWorldBorder().getCenterX(), 0.0, this.getWorldBorder().getCenterZ()));
        }
        return blockpos;
    }

    public float getSharedSpawnAngle() {
        return this.levelData.getSpawnAngle();
    }

    protected void prepareWeather() {
        if (this.levelData.isRaining()) {
            this.rainLevel = 1.0f;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0f;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.getChunkSource().close();
    }

    @Override
    @Nullable
    public BlockGetter getChunkForCollisions(int i, int j) {
        return this.getChunk(i, j, ChunkStatus.FULL, false);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity arg, AABB arg22, Predicate<? super Entity> predicate) {
        this.getProfiler().incrementCounter("getEntities");
        ArrayList<Entity> list = Lists.newArrayList();
        this.getEntities().get(arg22, arg2 -> {
            if (arg2 != arg && predicate.test((Entity)arg2)) {
                list.add((Entity)arg2);
            }
        });
        for (PartEntity<?> p : this.getPartEntities()) {
            if (p == arg || !p.getBoundingBox().intersects(arg22) || !predicate.test(p)) continue;
            list.add(p);
        }
        return list;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> arg, AABB arg2, Predicate<? super T> predicate) {
        ArrayList list = Lists.newArrayList();
        this.getEntities(arg, arg2, predicate, list);
        return list;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> arg, AABB arg2, Predicate<? super T> predicate, List<? super T> list) {
        this.getEntities(arg, arg2, predicate, list, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> arg, AABB arg22, Predicate<? super T> predicate, List<? super T> list, int i) {
        this.getProfiler().incrementCounter("getEntities");
        this.getEntities().get(arg, arg22, arg2 -> {
            if (predicate.test((Entity)arg2)) {
                list.add(arg2);
                if (list.size() >= i) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }
            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
        for (PartEntity<?> p : this.getPartEntities()) {
            Entity t = (Entity)arg.tryCast(p);
            if (t == null || !t.getBoundingBox().intersects(arg22) || !predicate.test(t)) continue;
            list.add(t);
            if (list.size() < i) continue;
            break;
        }
    }

    @Nullable
    public abstract Entity getEntity(int var1);

    public void blockEntityChanged(BlockPos arg) {
        if (this.hasChunkAt(arg)) {
            this.getChunkAt(arg).setUnsaved(true);
        }
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public void disconnect() {
    }

    public long getGameTime() {
        return this.levelData.getGameTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean mayInteract(Player arg, BlockPos arg2) {
        return true;
    }

    public void broadcastEntityEvent(Entity arg, byte b) {
    }

    public void broadcastDamageEvent(Entity arg, DamageSource arg2) {
    }

    public void blockEvent(BlockPos arg, Block arg2, int i, int j) {
        this.getBlockState(arg).triggerEvent(this, arg, i, j);
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    public GameRules getGameRules() {
        return this.levelData.getGameRules();
    }

    public abstract TickRateManager tickRateManager();

    public float getThunderLevel(float f) {
        return Mth.lerp(f, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(f);
    }

    public void setThunderLevel(float g) {
        float f;
        this.oThunderLevel = f = Mth.clamp(g, 0.0f, 1.0f);
        this.thunderLevel = f;
    }

    public float getRainLevel(float f) {
        return Mth.lerp(f, this.oRainLevel, this.rainLevel);
    }

    public void setRainLevel(float g) {
        float f;
        this.oRainLevel = f = Mth.clamp(g, 0.0f, 1.0f);
        this.rainLevel = f;
    }

    public boolean isThundering() {
        return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling() ? (double)this.getThunderLevel(1.0f) > 0.9 : false;
    }

    public boolean isRaining() {
        return (double)this.getRainLevel(1.0f) > 0.2;
    }

    public boolean isRainingAt(BlockPos arg) {
        if (!this.isRaining()) {
            return false;
        }
        if (!this.canSeeSky(arg)) {
            return false;
        }
        if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, arg).getY() > arg.getY()) {
            return false;
        }
        Biome biome = this.getBiome(arg).value();
        return biome.getPrecipitationAt(arg) == Biome.Precipitation.RAIN;
    }

    @Nullable
    public abstract MapItemSavedData getMapData(MapId var1);

    public abstract void setMapData(MapId var1, MapItemSavedData var2);

    public abstract MapId getFreeMapId();

    public void globalLevelEvent(int i, BlockPos arg, int j) {
    }

    public CrashReportCategory fillReportDetails(CrashReport arg) {
        CrashReportCategory crashreportcategory = arg.addCategory("Affected level", 1);
        crashreportcategory.setDetail("All players", () -> this.players().size() + " total; " + String.valueOf(this.players()));
        crashreportcategory.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
        crashreportcategory.setDetail("Level dimension", () -> this.dimension().location().toString());
        try {
            this.levelData.fillCrashReportCategory(crashreportcategory, this);
        } catch (Throwable throwable) {
            crashreportcategory.setDetailError("Level Data Unobtainable", throwable);
        }
        return crashreportcategory;
    }

    public abstract void destroyBlockProgress(int var1, BlockPos var2, int var3);

    public void createFireworks(double d, double e, double f, double g, double h, double i, List<FireworkExplosion> list) {
    }

    public abstract Scoreboard getScoreboard();

    public void updateNeighbourForOutputSignal(BlockPos arg, Block arg2) {
        for (Direction direction : Direction.values()) {
            BlockPos blockpos = arg.relative(direction);
            if (!this.hasChunkAt(blockpos)) continue;
            BlockState blockstate = this.getBlockState(blockpos);
            blockstate.onNeighborChange(this, blockpos, arg);
            if (!blockstate.isRedstoneConductor(this, blockpos) || !(blockstate = this.getBlockState(blockpos = blockpos.relative(direction))).getWeakChanges(this, blockpos)) continue;
            this.neighborChanged(blockstate, blockpos, arg2, arg, false);
        }
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos arg) {
        long i = 0L;
        float f = 0.0f;
        if (this.hasChunkAt(arg)) {
            f = this.getMoonBrightness();
            i = this.getChunkAt(arg).getInhabitedTime();
        }
        return new DifficultyInstance(this.getDifficulty(), this.getDayTime(), i, f);
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int i) {
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void sendPacketToServer(Packet<?> arg) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionTypeRegistration.value();
    }

    public Holder<DimensionType> dimensionTypeRegistration() {
        return this.dimensionTypeRegistration;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPos arg, Predicate<BlockState> predicate) {
        return predicate.test(this.getBlockState(arg));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos arg, Predicate<FluidState> predicate) {
        return predicate.test(this.getFluidState(arg));
    }

    public abstract RecipeManager getRecipeManager();

    public BlockPos getBlockRandomPos(int j, int k, int l, int m) {
        this.randValue = this.randValue * 3 + 1013904223;
        int i = this.randValue >> 2;
        return new BlockPos(j + (i & 0xF), k + (i >> 16 & m), l + (i >> 8 & 0xF));
    }

    public boolean noSave() {
        return false;
    }

    public ProfilerFiller getProfiler() {
        return this.profiler.get();
    }

    public Supplier<ProfilerFiller> getProfilerSupplier() {
        return this.profiler;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public double getMaxEntityRadius() {
        return this.maxEntityRadius;
    }

    @Override
    public double increaseMaxEntityRadius(double value) {
        if (value > this.maxEntityRadius) {
            this.maxEntityRadius = value;
        }
        return this.maxEntityRadius;
    }

    public final boolean isDebug() {
        return this.isDebug;
    }

    protected abstract LevelEntityGetter<Entity> getEntities();

    @Override
    public long nextSubTickCount() {
        return this.subTickCount++;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public DamageSources damageSources() {
        return this.damageSources;
    }

    public abstract PotionBrewing potionBrewing();

    @ApiStatus.Internal
    public abstract void setDayTimeFraction(float var1);

    @ApiStatus.Internal
    public abstract float getDayTimeFraction();

    public abstract float getDayTimePerTick();

    public abstract void setDayTimePerTick(float var1);

    @ApiStatus.Internal
    protected long advanceDaytime() {
        if (this.getDayTimePerTick() < 0.0f) {
            return 1L;
        }
        float dayTimeStep = this.getDayTimeFraction() + this.getDayTimePerTick();
        long result = (long)dayTimeStep;
        this.setDayTimeFraction(dayTimeStep - (float)result);
        return result;
    }

    public static enum ExplosionInteraction implements StringRepresentable
    {
        NONE("none"),
        BLOCK("block"),
        MOB("mob"),
        TNT("tnt"),
        TRIGGER("trigger");

        public static final Codec<ExplosionInteraction> CODEC;
        private final String id;

        private ExplosionInteraction(String string2) {
            this.id = string2;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        static {
            CODEC = StringRepresentable.fromEnum(ExplosionInteraction::values);
        }
    }
}

