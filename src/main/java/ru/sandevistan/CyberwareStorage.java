package ru.sandevistan;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CyberwareStorage extends PersistentState {
    public static final int SLOT_COUNT = 5;
    public static final long STARTING_EDDIES = 250L;

    private final Map<UUID, DefaultedList<ItemStack>> playerSlots = new HashMap<>();
    private final Map<UUID, Long> playerEddies = new HashMap<>();
    private final Map<UUID, ActiveContract> activeContracts = new HashMap<>();
    private final Map<UUID, List<String>> bankHistory = new HashMap<>();

    public record ActiveContract(String fixerName, String targetType, int progress, int goal, long reward) {}

    public DefaultedList<ItemStack> getSlots(UUID uuid) {
        return playerSlots.computeIfAbsent(uuid, k -> DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY));
    }

    public long getEddies(UUID uuid) {
        if (!playerEddies.containsKey(uuid)) {
            playerEddies.put(uuid, STARTING_EDDIES);
            addHistory(uuid, "+€$" + STARTING_EDDIES + " [INIT]");
            markDirty();
        }
        return playerEddies.get(uuid);
    }

    public void addEddies(UUID uuid, long amount, String reason) {
        long current = getEddies(uuid);
        playerEddies.put(uuid, Math.max(0L, current + amount));
        if (amount > 0) addHistory(uuid, "+€$" + amount + " (" + reason + ")");
        else if (amount < 0) addHistory(uuid, "-€$" + Math.abs(amount) + " (" + reason + ")");
        markDirty();
    }

    public boolean spendEddies(UUID uuid, long amount, String reason) {
        long current = getEddies(uuid);
        if (current < amount) return false;
        playerEddies.put(uuid, current - amount);
        addHistory(uuid, "-€$" + amount + " (" + reason + ")");
        markDirty();
        return true;
    }

    public List<String> getHistory(UUID uuid) {
        return bankHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    private void addHistory(UUID uuid, String entry) {
        List<String> list = getHistory(uuid);
        list.add(0, entry);
        if (list.size() > 6) list.remove(list.size() - 1);
    }

    public ActiveContract getContract(UUID uuid) {
        return activeContracts.get(uuid);
    }

    public void setContract(UUID uuid, ActiveContract contract) {
        if (contract == null) activeContracts.remove(uuid);
        else activeContracts.put(uuid, contract);
        markDirty();
    }

    public Inventory getInventory(PlayerEntity player) {
        DefaultedList<ItemStack> items = getSlots(player.getUuid());
        return new Inventory() {
            @Override public int size() { return SLOT_COUNT; }
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

        NbtCompound eddiesTag = new NbtCompound();
        playerEddies.forEach((uuid, bal) -> eddiesTag.putLong(uuid.toString(), bal));
        nbt.put("Eddies", eddiesTag);

        NbtCompound contractsTag = new NbtCompound();
        activeContracts.forEach((uuid, c) -> {
            NbtCompound cTag = new NbtCompound();
            cTag.putString("Fixer", c.fixerName());
            cTag.putString("Target", c.targetType());
            cTag.putInt("Progress", c.progress());
            cTag.putInt("Goal", c.goal());
            cTag.putLong("Reward", c.reward());
            contractsTag.put(uuid.toString(), cTag);
        });
        nbt.put("Contracts", contractsTag);

        NbtCompound histTag = new NbtCompound();
        bankHistory.forEach((uuid, list) -> {
            NbtList nbtList = new NbtList();
            for (String s : list) nbtList.add(NbtString.of(s));
            histTag.put(uuid.toString(), nbtList);
        });
        nbt.put("History", histTag);
        return nbt;
    }

    public static CyberwareStorage createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        CyberwareStorage state = new CyberwareStorage();
        NbtCompound users = nbt.getCompound("Users");
        for (String key : users.getKeys()) {
            DefaultedList<ItemStack> list = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
            Inventories.readNbt(users.getCompound(key), list, registries);
            state.playerSlots.put(UUID.fromString(key), list);
        }
        NbtCompound eddiesTag = nbt.getCompound("Eddies");
        for (String key : eddiesTag.getKeys()) {
            state.playerEddies.put(UUID.fromString(key), eddiesTag.getLong(key));
        }
        NbtCompound contractsTag = nbt.getCompound("Contracts");
        for (String key : contractsTag.getKeys()) {
            NbtCompound cTag = contractsTag.getCompound(key);
            state.activeContracts.put(UUID.fromString(key), new ActiveContract(
                cTag.getString("Fixer"),
                cTag.getString("Target"),
                cTag.getInt("Progress"),
                cTag.getInt("Goal"),
                cTag.getLong("Reward")
            ));
        }
        NbtCompound histTag = nbt.getCompound("History");
        for (String key : histTag.getKeys()) {
            NbtList nbtList = histTag.getList(key, 8);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < nbtList.size(); i++) list.add(nbtList.getString(i));
            state.bankHistory.put(UUID.fromString(key), list);
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
