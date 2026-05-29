package dev.sixik.assemblytable.blockentity;

import dev.sixik.assemblytable.api.energy.LaserTarget;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.utils.energy.MutableEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LaserBlockEntity extends BlockEntity {
    
    public final MutableEnergyStorage energy;
    
    public final int maxTransferPerTick;
    public final int targetRange;

    private BlockPos targetPos = null;
    public Vec3 laserRenderOffset = null;

    private int scanTimer = 0;
    private int clientJitterTimer = 0;

    public LaserBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, 6, 1000, 1);
    }
    
    public LaserBlockEntity(BlockPos pos, BlockState blockState, int targetRange, int maxTransferPerTick, int energyCap) {
        super(ATMRegistry.LASER_TYPE.get(), pos, blockState);
        
        this.targetRange = targetRange;
        this.maxTransferPerTick = maxTransferPerTick;

        this.energy = new MutableEnergyStorage(energyCap, 5000, maxTransferPerTick) {

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

        if (targetPos != null && !isPowerNeededAt(targetPos)) {
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

                int extractable = this.energy.extractEnergy(maxTransferPerTick, true);
                int toSend = Math.min(extractable, target.getRequiredLaserPower());

                if (toSend > 0) {
                    int excess = target.receiveLaserPower(toSend);
                    int actuallySent = toSend - excess;

                    this.energy.extractEnergy(actuallySent, false);
                }
            }
        }

        if (targetPos != previousTarget) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Ищет блоки в радиусе targetRange, фильтрует те, что реализуют LaserTarget,
     * и случайным образом выбирает один, требующий энергии.
     */
    private void findNewTarget() {
        if (level == null) return;

        List<BlockPos> validTargets = new ArrayList<>();

        // Перебираем область вокруг лазера (куб 13x13x13)
        BlockPos start = this.worldPosition.offset(-targetRange, -targetRange, -targetRange);
        BlockPos end = this.worldPosition.offset(targetRange, targetRange, targetRange);

        for (BlockPos p : BlockPos.betweenClosed(start, end)) {
            if (isPowerNeededAt(p)) {
                validTargets.add(p.immutable()); // Обязательно immutable, так как betweenClosed использует mutable
            }
        }

        if (!validTargets.isEmpty()) {
            // Выбираем случайную цель из доступных
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

    public BlockPos getTargetPos() {
        return targetPos;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.put("energy", energy.serializeNBT(provider));
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
