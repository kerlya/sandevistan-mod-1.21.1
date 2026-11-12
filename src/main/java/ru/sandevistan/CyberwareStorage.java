package ru.sandevistan;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CyberwareStorage extends PersistentState {
    private final Map<UUID, DefaultedList<ItemStack>> playerSlots = new HashMap<>();

    public DefaultedList<ItemStack> getSlots(UUID uuid) {
        return playerSlots.computeIfAbsent(uuid, k -> DefaultedList.ofSize(4, ItemStack.EMPTY));
    }

    public Inventory getInventory(PlayerEntity player) {
        DefaultedList<ItemStack> items = getSlots(player.getUuid());
        return new Inventory() {
            @Override public int size() { return 4; }
            @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
            @Override public ItemStack getStack(int slot) { return items.get(slot); }
            @Override public ItemStack removeStack(int slot, int amount) {
                ItemStack res = Inventories.splitStack(items, slot, amount);
                if (!res.isEmpty()) markDirty();
                return res;
            }
            @Override public ItemStack removeStack(int slot) {
                ItemStack res = Inventories.removeStack(items, slot);
                markDirty();
                return res;
            }
            @Override public void setStack(int slot, ItemStack stack) {
                items.set(slot, stack);
                if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
                markDirty();
            }
            @Override public int getMaxCountPerStack() { return 1; }
            @Override public void markDirty() { CyberwareStorage.this.markDirty(); }
            @Override public boolean canPlayerUse(PlayerEntity p) { return true; }
            @Override public void clear() { items.clear(); markDirty(); }
        };
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound users = new NbtCompound();
        playerSlots.forEach((uuid, list) -> {
            NbtCompound invTag = new NbtCompound();
            Inventories.writeNbt(invTag, list, registries);
            users.put(uuid.toString(), invTag);
        });
        nbt.put("Users", users);
        return nbt;
    }

    public static CyberwareStorage createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        CyberwareStorage state = new CyberwareStorage();
        NbtCompound users = nbt.getCompound("Users");
        for (String key : users.getKeys()) {
            DefaultedList<ItemStack> list = DefaultedList.ofSize(4, ItemStack.EMPTY);
            Inventories.readNbt(users.getCompound(key), list, registries);
            state.playerSlots.put(UUID.fromString(key), list);
        }
        return state;
    }

    private static final Type<CyberwareStorage> TYPE = new Type<>(
        CyberwareStorage::new,
        CyberwareStorage::createFromNbt,
        null
    );

    public static CyberwareStorage get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE, "sandevistan_cyberware");
    }
}
