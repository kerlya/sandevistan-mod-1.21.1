package ru.sandevistan;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class DropPointManager {
    public record SellEntry(String id, Item item, String category, int batchSize, long price) {}

    public static final List<SellEntry> ENTRIES = new ArrayList<>();

    public static void init() {
        ENTRIES.clear();
        // Оружие и боеприпасы
        ENTRIES.add(new SellEntry("saratoga_m221", SandevistanMod.SARATOGA_M221, "weapons", 1, 320L));
        ENTRIES.add(new SellEntry("lead_pipe", SandevistanMod.LEAD_PIPE, "weapons", 1, 35L));
        ENTRIES.add(new SellEntry("smg_ammo", SandevistanMod.SMG_AMMO, "weapons", 16, 20L));

        // Импланты
        ENTRIES.add(new SellEntry("metal_arms", SandevistanMod.METAL_ARMS, "cyberware", 1, 250L));
        ENTRIES.add(new SellEntry("kiroshi_mk1", SandevistanMod.KIROSHI_MK1, "cyberware", 1, 400L));
        ENTRIES.add(new SellEntry("sandevistan", SandevistanMod.SANDEVISTAN, "cyberware", 1, 1200L));

        // Ресурсы и трофеи
        ENTRIES.add(new SellEntry("diamond", Items.DIAMOND, "resources", 1, 180L));
        ENTRIES.add(new SellEntry("emerald", Items.EMERALD, "resources", 1, 90L));
        ENTRIES.add(new SellEntry("gold_ingot", Items.GOLD_INGOT, "resources", 1, 45L));
        ENTRIES.add(new SellEntry("iron_ingot", Items.IRON_INGOT, "resources", 1, 15L));
        ENTRIES.add(new SellEntry("copper_ingot", Items.COPPER_INGOT, "resources", 1, 8L));
        ENTRIES.add(new SellEntry("redstone", Items.REDSTONE, "resources", 8, 12L));
        ENTRIES.add(new SellEntry("gunpowder", Items.GUNPOWDER, "resources", 4, 10L));
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("droppoint")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("spawn")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    Direction facing = p.getHorizontalFacing();
                    BlockPos pos = p.getBlockPos().offset(facing, 2);
                    p.getServerWorld().setBlockState(pos, SandevistanMod.DROP_POINT.getDefaultState().with(DropPointBlock.FACING, facing.getOpposite()), 3);
                    p.sendMessage(Text.translatable("msg.sandevistan.droppoint.spawned"), false);
                    return 1;
                }))
        );
    }
    public static void handleSell(ServerPlayerEntity player, String entryId, boolean sellAll) {
        if (player.getServer() == null) return;
        if (ENTRIES.isEmpty()) init();

        SellEntry target = null;
        for (SellEntry e : ENTRIES) {
            if (e.id().equals(entryId)) {
                target = e;
                break;
            }
        }
        if (target == null) return;

        int totalCount = 0;
        for (int i = 0; i != player.getInventory().size(); i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (st.isOf(target.item())) {
                totalCount += st.getCount();
            }
        }

        int batchesAvailable = totalCount / target.batchSize();
        if (batchesAvailable <= 0) {
            player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.MASTER, 0.8f, 1.4f);
            player.sendMessage(Text.translatable("msg.sandevistan.droppoint.not_enough"), true);
            return;
        }

        int batchesToSell = sellAll ? batchesAvailable : 1;
        int itemsToRemove = batchesToSell * target.batchSize();
        long earnedEddies = batchesToSell * target.price();

        int leftToRemove = itemsToRemove;
        for (int i = 0; i != player.getInventory().size() && leftToRemove > 0; i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (st.isOf(target.item())) {
                int take = Math.min(leftToRemove, st.getCount());
                st.decrement(take);
                leftToRemove -= take;
            }
        }

        CyberwareStorage.get(player.getServer()).addEddies(player.getUuid(), earnedEddies, "DROP POINT");
        FixerContractManager.syncPlayer(player);

        Identifier itemKey = Registries.ITEM.getId(target.item());
        Text itemName = Text.translatable("item." + itemKey.getNamespace() + "." + itemKey.getPath());
        player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.9f, 1.35f);
        player.sendMessage(Text.translatable("msg.sandevistan.droppoint.sold", itemsToRemove, itemName, earnedEddies), true);
    }
}
