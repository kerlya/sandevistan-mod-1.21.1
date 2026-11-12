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

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInv) {
        this(syncId, playerInv, new SimpleInventory(CyberwareStorage.SLOT_COUNT));
    }

    public CyberwareScreenHandler(int syncId, PlayerInventory playerInv, Inventory cyberwareInv) {
        super(SandevistanMod.CYBERWARE_HANDLER, syncId);
        this.cyberwareInv = cyberwareInv;
        cyberwareInv.onOpen(playerInv.player);

        // 0: ОС (Сандевистан)
        this.addSlot(new Slot(cyberwareInv, 0, 22, 34) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.SANDEVISTAN); }
            @Override public int getMaxItemCount() { return 1; }
        });

        // 1: ГЛАЗА (Оптика Кироши)
        this.addSlot(new Slot(cyberwareInv, 1, 22, 62) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.KIROSHI_MK1); }
            @Override public int getMaxItemCount() { return 1; }
        });

        // 2: РУКИ (Металлические руки)
        this.addSlot(new Slot(cyberwareInv, 2, 138, 34) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(SandevistanMod.METAL_ARMS); }
            @Override public int getMaxItemCount() { return 1; }
        });

        // 3: КОЖА (резерв)
        this.addSlot(new Slot(cyberwareInv, 3, 138, 62) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });

        // 4: НОГИ (резерв)
        this.addSlot(new Slot(cyberwareInv, 4, 138, 90) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public int getMaxItemCount() { return 1; }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 134 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 192));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();
            if (slotIndex < CyberwareStorage.SLOT_COUNT) {
                if (!this.insertItem(original, CyberwareStorage.SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(original, 0, CyberwareStorage.SLOT_COUNT, false)) return ItemStack.EMPTY;
            }
            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.cyberwareInv.canPlayerUse(player);
    }
}
