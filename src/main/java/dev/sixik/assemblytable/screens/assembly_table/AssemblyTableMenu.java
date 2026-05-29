package dev.sixik.assemblytable.screens.assembly_table;

import dev.sixik.assemblytable.blockentity.AssemblyTableBlockEntity;
import dev.sixik.assemblytable.recipes.AssemblyTableRecipe;
import dev.sixik.assemblytable.register.ATMRegistry;
import dev.sixik.assemblytable.utils.slots.SlotBase;
import dev.sixik.assemblytable.utils.slots.SlotDisplay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.Optional;

public class AssemblyTableMenu extends AbstractContainerMenu {

    public final AssemblyTableBlockEntity blockEntity;
    private final ContainerData data;

    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = 36;
    private static final int TABLE_INPUT_START = 36;
    private static final int TABLE_INPUT_END = 48;

    public AssemblyTableMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public AssemblyTableMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ATMRegistry.ASSEMBLY_TABLE_MENU.get(), pContainerId);

        this.blockEntity = (AssemblyTableBlockEntity) entity;
        this.data = data;

        this.addDataSlots(data);

        createInventorySlots(inv, 123);

        for(int y = 0; y < 4; y++) {
            for(int x = 0; x < 3; x++) {
                this.addSlot(new SlotItemHandler(this.blockEntity.inventory, x + y * 3, 8 + x * 18, 36 + y * 18));
            }
        }

        for(int y = 0; y < 4; y++) {
            for(int x = 0; x < 3; x++) {
                addSlot(new SlotDisplay(this::getDisplay, x + y * 3, 116 + x * 18, 36 + y * 18));
            }
        }
    }

    private ItemStack getDisplay(int index) {
        if (this.blockEntity == null || index >= this.blockEntity.recipeStates.size()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation recipeId = new ArrayList<>(this.blockEntity.recipeStates.keySet()).get(index);
        Optional<RecipeHolder<?>> recipeHolder = this.blockEntity.getLevel().getRecipeManager().byKey(recipeId);

        if (recipeHolder.isPresent() && recipeHolder.get().value() instanceof AssemblyTableRecipe recipe) {
            return recipe.getResultItem(this.blockEntity.getLevel().registryAccess());
        }

        return ItemStack.EMPTY;
    }

    public int getEnergy() {
        return (this.data.get(1) << 16) | (this.data.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.data.get(3) << 16) | (this.data.get(2) & 0xFFFF);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ATMRegistry.ASSEMBLY_TABLE.get());
    }

    private void createInventorySlots(Inventory p_267325_, int startY) {
        for(int sy = 0; sy < 3; ++sy) {
            for(int sx = 0; sx < 9; ++sx) {
                this.addSlot(new Slot(p_267325_, sx + sy * 9 + 9, 8 + sx * 18, startY + sy * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(p_267325_, k, 8 + k * 18, startY + 58));
        }

    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;

        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem() && !(slot instanceof SlotDisplay)) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
                if (!this.moveItemStackTo(stackInSlot, TABLE_INPUT_START, TABLE_INPUT_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= TABLE_INPUT_START && index < TABLE_INPUT_END) {
                if (!this.moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return originalStack;
    }
}
