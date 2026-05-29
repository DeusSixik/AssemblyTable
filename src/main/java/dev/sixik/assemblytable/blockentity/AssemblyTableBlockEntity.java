package dev.sixik.assemblytable.blockentity;

import dev.sixik.assemblytable.api.energy.LaserTarget;
import dev.sixik.assemblytable.block.LaserTableBlock;
import dev.sixik.assemblytable.recipes.AssemblyTableRecipe;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.screens.assembly_table.AssemblyTableMenu;
import dev.sixik.assemblytable.utils.ATMRecipeManager;
import dev.sixik.assemblytable.utils.AssemblyRecipeState;
import dev.sixik.assemblytable.utils.SizedIngredient;
import dev.sixik.assemblytable.utils.energy.AssemblyEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AssemblyTableBlockEntity extends BlockEntity
        implements MenuProvider, LaserTableBlock.DropContainer, LaserTarget {

    public final LinkedHashMap<ResourceLocation, AssemblyRecipeState> recipeStates = new LinkedHashMap<>();
    public RecipeHolder<AssemblyTableRecipe> currentActiveRecipe = null;
    private boolean suppressRecipeStateUpdates = false;
    private boolean recipeStateUpdateQueued = false;

    public final ItemStackHandler inventory = new ItemStackHandler(3 * 4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            if (level != null && !level.isClientSide()) {
                if (suppressRecipeStateUpdates) {
                    recipeStateUpdateQueued = true;
                } else {
                    updateRecipeStates();
                }
            }
        }
    };

    public final AssemblyEnergy energy = new AssemblyEnergy(this);

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energy.getMaxEnergyStored() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };


    public AssemblyTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ATMRegistry.ASSEMBLY_TABLE_TYPE.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Когда блок полностью загрузился в мир на сервере — сразу считаем доступные рецепты
        if (level != null && !level.isClientSide()) {
            updateRecipeStates();
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (currentActiveRecipe != null && recipeStates.get(currentActiveRecipe.id()) == AssemblyRecipeState.ACTIVE) {
            int energyRequired = currentActiveRecipe.value().getEnergyRequired();

            if (this.energy.getEnergyStored() >= energyRequired) {

                RecipeWrapper wrapper = new RecipeWrapper(this.inventory);
                final AssemblyTableRecipe value = currentActiveRecipe.value();
                if (value.matches(wrapper, level)) {
                    if(value != null) {
                        ResourceLocation craftedRecipeId = currentActiveRecipe.id();
                        extract(inventory, value.getIngredientsSizable(), false, false);

                        ItemStack result = value.assemble(wrapper, level.registryAccess());
                        outputResult(level, result);

                        this.energy.consumeEnergy(energyRequired);

                        activateNextRecipe(craftedRecipeId);
                    }
                }
            }
        }
    }

    /**
     * Ядро системы: обновляет карту состояний на основе содержимого инвентаря.
     */
    public void updateRecipeStates() {
        updateRecipeStates(true);
    }

    private void updateRecipeStates(boolean allowAutoActivation) {
        RecipeWrapper wrapper = new RecipeWrapper(this.inventory);
        var possibleHolders = ATMRecipeManager.getAllRecipesForAssemblyTable(this.level, wrapper);

        Set<ResourceLocation> canBeCrafted = new HashSet<>();
        for (var holder : possibleHolders) {
            canBeCrafted.add(holder.id());
        }

        Iterator<Map.Entry<ResourceLocation, AssemblyRecipeState>> iterator = recipeStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, AssemblyRecipeState> entry = iterator.next();
            ResourceLocation id = entry.getKey();
            AssemblyRecipeState state = entry.getValue();

            boolean hasItems = canBeCrafted.contains(id);

            if (state == AssemblyRecipeState.POSSIBLE && !hasItems) {
                iterator.remove();
            } else if (state == AssemblyRecipeState.ACTIVE || state == AssemblyRecipeState.SAVED_ENOUGH) {
                if (!hasItems) entry.setValue(AssemblyRecipeState.SAVED);
            } else if (state == AssemblyRecipeState.SAVED && hasItems) {
                entry.setValue(AssemblyRecipeState.SAVED_ENOUGH);
            }
        }

        for (var holder : possibleHolders) {
            if (!recipeStates.containsKey(holder.id())) {
                recipeStates.put(holder.id(), AssemblyRecipeState.POSSIBLE);
            }
        }

        if (allowAutoActivation &&
                !recipeStates.containsValue(AssemblyRecipeState.ACTIVE) &&
                recipeStates.containsValue(AssemblyRecipeState.SAVED_ENOUGH)) {
            activateNextRecipe(null);
        }

        updateCurrentActiveRecipeHolder();

        setChanged();
        this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    /**
     * Сдвигает активный крафт на следующий в списке SAVED_ENOUGH
     */
    private void activateNextRecipe(@Nullable ResourceLocation previousRecipeId) {
        List<ResourceLocation> sequence = new ArrayList<>(recipeStates.keySet());
        if (sequence.isEmpty()) {
            updateCurrentActiveRecipeHolder();
            return;
        }

        ResourceLocation rotationStart = previousRecipeId;
        if (rotationStart == null) {
            for (ResourceLocation recipeId : sequence) {
                if (recipeStates.get(recipeId) == AssemblyRecipeState.ACTIVE) {
                    rotationStart = recipeId;
                    break;
                }
            }
        }

        int startIndex = 0;
        if (rotationStart != null) {
            if (recipeStates.get(rotationStart) == AssemblyRecipeState.ACTIVE) {
                recipeStates.put(rotationStart, AssemblyRecipeState.SAVED_ENOUGH);
            }

            int previousIndex = sequence.indexOf(rotationStart);
            if (previousIndex >= 0) {
                startIndex = (previousIndex + 1) % sequence.size();
            }
        }

        for (int i = 0; i < sequence.size(); i++) {
            int checkIndex = (startIndex + i) % sequence.size();
            ResourceLocation id = sequence.get(checkIndex);

            if (recipeStates.get(id) == AssemblyRecipeState.SAVED_ENOUGH) {
                recipeStates.put(id, AssemblyRecipeState.ACTIVE);
                updateCurrentActiveRecipeHolder();
                return;
            }
        }

        updateCurrentActiveRecipeHolder();
    }

    private void updateCurrentActiveRecipeHolder() {
        this.currentActiveRecipe = null;
        for (Map.Entry<ResourceLocation, AssemblyRecipeState> entry : recipeStates.entrySet()) {
            if (entry.getValue() == AssemblyRecipeState.ACTIVE) {
                var optional = this.level.getRecipeManager().byKey(entry.getKey());
                if (optional.isPresent() && optional.get().value() instanceof AssemblyTableRecipe) {
                    this.currentActiveRecipe = (RecipeHolder<AssemblyTableRecipe>) (Object) optional.get();
                }
                break;
            }
        }
    }

    private void outputResult(Level level, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }

        BlockPos outPos = getBlockPos().above();
        ItemStack remainder = tryInsertIntoAboveInventory(level, outPos, result.copy());
        if (!remainder.isEmpty()) {
            Containers.dropItemStack(level, outPos.getX(), outPos.getY(), outPos.getZ(), remainder);
        }
    }

    private ItemStack tryInsertIntoAboveInventory(Level level, BlockPos targetPos, ItemStack stack) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, Direction.DOWN);
        if (handler == null) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, null);
        }

        if (handler == null) {
            return stack;
        }

        return ItemHandlerHelper.insertItemStacked(handler, stack, false);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.put("inventory", inventory.serializeNBT(provider));
        nbt.put("energy", energy.serializeNBT(provider));
        ListTag statesTag = new ListTag();
        recipeStates.forEach((id, state) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id.toString());
            tag.putInt("state", state.ordinal());
            statesTag.add(tag);
        });
        nbt.put("recipe_states", statesTag);
        super.saveAdditional(nbt, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("inventory")) {
            inventory.deserializeNBT(provider, nbt.getCompound("inventory"));
        }
        if (nbt.contains("energy")) {
            energy.deserializeNBT(provider, nbt.get("energy"));
        }

        if (nbt.contains("recipe_states")) {
            recipeStates.clear();
            ListTag statesTag = nbt.getList("recipe_states", Tag.TAG_COMPOUND);
            for (int i = 0; i < statesTag.size(); i++) {
                CompoundTag tag = statesTag.getCompound(i);
                ResourceLocation id = ResourceLocation.parse(tag.getString("id"));
                AssemblyRecipeState state = AssemblyRecipeState.values()[tag.getInt("state")];
                recipeStates.put(id, state);
            }
        }
    }

    /**
     * @param inv Инвентарь стола
     * @param items Список ингредиентов из рецепта
     * @param simulate Если true - предметы не пропадут, мы только проверяем
     * @param precise Точный крафт (в столе не должно быть мусора/лишних предметов)
     */
    protected boolean extract(ItemStackHandler inv, List<SizedIngredient> items, boolean simulate, boolean precise) {
        if (!simulate && !extract(inv, items, true, precise)) {
            return false;
        }

        boolean batchedInventoryUpdate = !simulate;
        if (batchedInventoryUpdate) {
            suppressRecipeStateUpdates = true;
            recipeStateUpdateQueued = false;
        }

        try {
            int occupiedSlots = 0;
            for (int i = 0; i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    occupiedSlots++;
                }
            }

            boolean allItemsConsumed = true;

            for (SizedIngredient definition : items) {
                int remaining = definition.count();

                for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
                    ItemStack slotStack = inv.getStackInSlot(i);

                    if (slotStack.isEmpty()) continue;

                    if (definition.ingredient().test(slotStack)) {
                        ItemStack extracted = inv.extractItem(i, remaining, simulate);
                        remaining -= extracted.getCount();
                    }
                }

                if (remaining == 0) {
                    occupiedSlots--;
                } else {
                    allItemsConsumed = false;
                    break;
                }
            }

            return allItemsConsumed && (!precise || occupiedSlots <= 0);
        } finally {
            if (batchedInventoryUpdate) {
                suppressRecipeStateUpdates = false;
                if (recipeStateUpdateQueued) {
                    recipeStateUpdateQueued = false;
                    updateRecipeStates(false);
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Assembly Table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new AssemblyTableMenu(i, inventory, this, data);
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

    @Override
    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for(int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    public int getRequiredLaserPower() {
        return this.energy.getMaxEnergyStored() - this.energy.getEnergyStored();
    }

    @Override
    public int receiveLaserPower(int energyToReceive) {
        int received = this.energy.receiveLaserEnergy(energyToReceive, false);
        return energyToReceive - received;
    }

    @Override
    public boolean isInvalidTarget() {
        return this.isRemoved();
    }
}
