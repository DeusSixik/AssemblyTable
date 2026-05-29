package dev.sixik.assemblytable.blockentity;

import dev.sixik.assemblytable.api.energy.LaserTarget;
import dev.sixik.assemblytable.api.laser.LaserConfig;
import dev.sixik.assemblytable.block.LaserBlock;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.utils.energy.MutableEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LaserBlockEntity extends BlockEntity {

    public final MutableEnergyStorage energy;
    public final int maxTransferPerTick;
    public final int targetRange;
    public final LaserConfig laserConfig;

    private BlockPos targetPos = null;
    public Vec3 laserRenderOffset = null;

    private int scanTimer = 0;
    private int clientJitterTimer = 0;
    private int warmupTicks = 0;
    private int syncedBeamColorStage = 0;

    public LaserBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, LaserConfig.builder().build());
    }

    public LaserBlockEntity(BlockPos pos, BlockState blockState, LaserConfig laserConfig) {
        super(ATMRegistry.LASER_TYPE.get(), pos, blockState);

        this.laserConfig = laserConfig;
        this.targetRange = laserConfig.targetRange();
        this.maxTransferPerTick = laserConfig.maxTransferPerTick();

        this.energy = new MutableEnergyStorage(laserConfig.energyBuffer(), laserConfig.maxReceivePerTick(), maxTransferPerTick) {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                int value = super.receiveEnergy(toReceive, simulate);
                setChanged();
                return value;
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                int value = super.extractEnergy(toExtract, simulate);
                setChanged();
                return value;
            }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            if (targetPos != null) {
                clientJitterTimer++;
                if (clientJitterTimer >= 5 || laserRenderOffset == null) {
                    clientJitterTimer = 0;
                    double offsetX = (5 + level.random.nextInt(6) + 0.5) / 16D;
                    double offsetY = (9 / 16D) + 0.01D;
                    double offsetZ = (5 + level.random.nextInt(6) + 0.5) / 16D;
                    laserRenderOffset = new Vec3(targetPos.getX() + offsetX, targetPos.getY() + offsetY, targetPos.getZ() + offsetZ);
                }
            } else {
                laserRenderOffset = null;
            }
            return;
        }

        BlockPos previousTarget = targetPos;
        int previousBeamColorStage = getBeamColorStage();
        boolean previousHasEnergy = this.energy.getEnergyStored() > 0;

        if (targetPos != null && (!isPowerNeededAt(targetPos) || !isTargetVisible(targetPos))) {
            targetPos = null;
        }

        scanTimer++;
        if (scanTimer >= 20 || targetPos == null) {
            scanTimer = 0;
            if (targetPos == null) {
                findNewTarget();
            }
        }

        if (targetPos != null && this.energy.getEnergyStored() > 0) {
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be instanceof LaserTarget target) {
                int available = this.energy.extractEnergy(maxTransferPerTick, true);
                int toSend = Math.min(available, target.getRequiredLaserPower());

                if (toSend > 0) {
                    int actualToSend = getDeliveredEnergy(toSend);
                    if (actualToSend > 0) {
                        int excess = target.receiveLaserPower(actualToSend);
                        int actuallySent = actualToSend - excess;

                        this.energy.extractEnergy(actuallySent, false);
                        updateWarmupTicks(actuallySent);
                    }
                } else {
                    updateWarmupTicks(0);
                }
            }
        } else {
            updateWarmupTicks(0);
        }

        int currentBeamColorStage = getBeamColorStage();
        boolean currentHasEnergy = this.energy.getEnergyStored() > 0;
        if (currentBeamColorStage != previousBeamColorStage) {
            syncedBeamColorStage = currentBeamColorStage;
        }

        if (targetPos != previousTarget
                || currentBeamColorStage != previousBeamColorStage
                || currentHasEnergy != previousHasEnergy) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private int getDeliveredEnergy(int requested) {
        if (!laserConfig.hasWarmup()) {
            return Math.min(requested, maxTransferPerTick);
        }

        int warmupLimit = getWarmupLimit();
        return Math.min(requested, Math.min(maxTransferPerTick, warmupLimit));
    }

    private int getWarmupLimit() {
        if (!laserConfig.hasWarmup()) {
            return maxTransferPerTick;
        }

        int warmupDuration = laserConfig.warmup().ticks();
        if (warmupDuration <= 0) {
            return maxTransferPerTick;
        }

        float progress = Math.min(1.0f, (float) warmupTicks / warmupDuration);
        float eased = progress * progress;
        int baseLimit = Math.max(1, maxTransferPerTick);
        return Math.max(1, Math.round(baseLimit * eased));
    }

    private void updateWarmupTicks(int actuallySent) {
        if (!laserConfig.hasWarmup()) {
            warmupTicks = 0;
            return;
        }

        if (actuallySent > 0) {
            warmupTicks = Math.min(laserConfig.warmup().ticks(), warmupTicks + 1);
        } else {
            warmupTicks = Math.max(0, warmupTicks - 1);
        }
    }

    /**
     * Ищет блоки в радиусе targetRange, фильтрует те, что реализуют LaserTarget,
     * и случайным образом выбирает один, требующий энергии.
     */
    private void findNewTarget() {
        if (level == null) return;

        List<BlockPos> validTargets = new ArrayList<>();

        Direction facing = getBlockState().getValue(LaserBlock.FACING);
        Direction axisA = getPerpendicularAxisA(facing);
        Direction axisB = getPerpendicularAxisB(facing);

        for (int depth = 1; depth <= targetRange; depth++) {
            int radius = depth - 1;

            for (int offsetA = -radius; offsetA <= radius; offsetA++) {
                for (int offsetB = -radius; offsetB <= radius; offsetB++) {
                    BlockPos p = this.worldPosition
                            .relative(facing, depth)
                            .relative(axisA, offsetA)
                            .relative(axisB, offsetB);

                    if (isPowerNeededAt(p) && isTargetVisible(p)) {
                        validTargets.add(p.immutable());
                    }
                }
            }
        }

        if (!validTargets.isEmpty()) {
            targetPos = validTargets.get(level.random.nextInt(validTargets.size()));
        } else {
            targetPos = null;
        }
    }

    private boolean isPowerNeededAt(BlockPos pos) {
        if (level == null) return false;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LaserTarget target) {
            return !target.isInvalidTarget() && target.getRequiredLaserPower() > 0;
        }
        return false;
    }

    private boolean isTargetVisible(BlockPos targetPos) {
        if (level == null) return false;

        Vec3 start = getBeamStart();
        Vec3 end = Vec3.atCenterOf(targetPos);
        BlockHitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));

        return hitResult.getType() == HitResult.Type.MISS || hitResult.getBlockPos().equals(targetPos);
    }

    private Vec3 getBeamStart() {
        Direction facing = getBlockState().getValue(LaserBlock.FACING);
        return Vec3.atCenterOf(this.worldPosition).add(
                facing.getStepX() * 0.5D,
                facing.getStepY() * 0.5D,
                facing.getStepZ() * 0.5D
        );
    }

    private static Direction getPerpendicularAxisA(Direction facing) {
        return switch (facing.getAxis()) {
            case X -> Direction.UP;
            case Y -> Direction.EAST;
            case Z -> Direction.EAST;
        };
    }

    private static Direction getPerpendicularAxisB(Direction facing) {
        return switch (facing.getAxis()) {
            case X -> Direction.SOUTH;
            case Y -> Direction.SOUTH;
            case Z -> Direction.UP;
        };
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public Vec3 getBeamColor() {
        if (!laserConfig.hasWarmup()) {
            return colorToVec3(laserConfig.beamColor());
        }

        int stage = level != null && level.isClientSide() ? syncedBeamColorStage : getBeamColorStage();
        return switch (stage) {
            case 0 -> colorToVec3(laserConfig.warmup().lowSpeedColor());
            case 1 -> colorToVec3(laserConfig.warmup().midSpeedColor());
            default -> colorToVec3(laserConfig.warmup().maxSpeedColor());
        };
    }

    private int getBeamColorStage() {
        if (!laserConfig.hasWarmup()) {
            return 2;
        }

        int warmupDuration = laserConfig.warmup().ticks();
        if (warmupDuration <= 0) {
            return 2;
        }

        float progress = Math.min(1.0f, (float) warmupTicks / warmupDuration);
        if (progress < 0.34f) {
            return 0;
        }
        if (progress < 0.84f) {
            return 1;
        }
        return 2;
    }

    private Vec3 colorToVec3(dev.sixik.assemblytable.utils.Vec4i color) {
        return new Vec3(color.x() / 255.0, color.y() / 255.0, color.z() / 255.0);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.put("energy", energy.serializeNBT(provider));
        nbt.putInt("warmup_ticks", warmupTicks);
        nbt.putInt("beam_color_stage", getBeamColorStage());
        if (targetPos != null) {
            nbt.put("target_pos", NbtUtils.writeBlockPos(targetPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("energy")) {
            energy.deserializeNBT(provider, nbt.get("energy"));
        }

        warmupTicks = nbt.getInt("warmup_ticks");
        syncedBeamColorStage = nbt.getInt("beam_color_stage");

        if (nbt.contains("target_pos")) {
            targetPos = NbtUtils.readBlockPos(nbt, "target_pos").orElse(null);
        } else {
            targetPos = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean hasEnergy() {
        return this.energy.getEnergyStored() != 0;
    }
}
