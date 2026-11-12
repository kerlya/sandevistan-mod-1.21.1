package ru.sandevistan;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CyberwareScreenHandler extends ScreenHandler {
    private final Inventory cyberwareInv;

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CyberwareStorage.SLOT_COUNT));
    }

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInventory, Inventory cyberwareInv) {
        super(SandevistanMod.CYBERWARE_HANDLER, syncId);
        checkSize(cyberwareInv, CyberwareStorage.SLOT_COUNT);
        this.cyberwareInv = cyberwareInv;
        cyberwareInv.onOpen(playerInventory.player);

        this.addSlot(new Slot(cyberwareInv, 0, 152, 32) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.SANDEVISTAN); }
            @Override public int getMaxItemCount() { return 1; }
        });
        this.addSlot(new Slot(cyberwareInv, 1, 152, 64) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.KIROSHI_MK1); }
            @Override public int getMaxItemCount() { return 1; }
        });
        this.addSlot(new Slot(cyberwareInv, 2, 48, 36) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        this.addSlot(new Slot(cyberwareInv, 3, 48, 84) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });
        this.addSlot(new Slot(cyberwareInv, 4, 152, 96) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 28 + col * 18, 134 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 28 + col * 18, 192));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.cyberwareInv.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            int totalSlots = CyberwareStorage.SLOT_COUNT;
            if (invSlot < totalSlots) {
                if (!this.insertItem(originalStack, totalSlots, totalSlots + 36, true)) return ItemStack.EMPTY;
            } else if (!this.insertItem(originalStack, 0, totalSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }
}
