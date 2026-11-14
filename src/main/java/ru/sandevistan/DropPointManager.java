package ru.sandevistan;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DropPointManager {
    public record SellEntry(String id, Item item, String category, int batchSize, long price) {}

    public static final List<SellEntry> ENTRIES = new ArrayList<>();
    private static final Set<Long> CHECKED_SECTORS = new HashSet<>();
    private static final List<BlockPos> DROP_POINT_LOCATIONS = new ArrayList<>();
    private static int tickCounter = 0;

    public static void init() {
        ENTRIES.clear();
        ENTRIES.add(new SellEntry("saratoga_m221", SandevistanMod.SARATOGA_M221, "weapons", 1, 320L));
        ENTRIES.add(new SellEntry("lead_pipe", SandevistanMod.LEAD_PIPE, "weapons", 1, 35L));
        ENTRIES.add(new SellEntry("smg_ammo", SandevistanMod.SMG_AMMO, "weapons", 16, 20L));

        ENTRIES.add(new SellEntry("metal_arms", SandevistanMod.METAL_ARMS, "cyberware", 1, 250L));
        ENTRIES.add(new SellEntry("kiroshi_mk1", SandevistanMod.KIROSHI_MK1, "cyberware", 1, 400L));
        ENTRIES.add(new SellEntry("sandevistan", SandevistanMod.SANDEVISTAN, "cyberware", 1, 1200L));

        ENTRIES.add(new SellEntry("diamond", Items.DIAMOND, "resources", 1, 180L));
        ENTRIES.add(new SellEntry("emerald", Items.EMERALD, "resources", 1, 90L));
        ENTRIES.add(new SellEntry("gold_ingot", Items.GOLD_INGOT, "resources", 1, 45L));
        ENTRIES.add(new SellEntry("iron_ingot", Items.IRON_INGOT, "resources", 1, 15L));
        ENTRIES.add(new SellEntry("copper_ingot", Items.COPPER_INGOT, "resources", 1, 8L));
        ENTRIES.add(new SellEntry("redstone", Items.REDSTONE, "resources", 8, 12L));
        ENTRIES.add(new SellEntry("gunpowder", Items.GUNPOWDER, "resources", 4, 10L));

        ServerTickEvents.END_SERVER_TICK.register(DropPointManager::tick);
    }

    private static void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 120 != 0) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            if (world.getRegistryKey() != World.OVERWORLD) continue;

            int secX = Math.floorDiv(player.getBlockX(), 256);
            int secZ = Math.floorDiv(player.getBlockZ(), 256);
            long key = (((long) secX) << 32) ^ (secZ & 0xffffffffL);

            if (CHECKED_SECTORS.contains(key)) continue;
            CHECKED_SECTORS.add(key);

            if (world.random.nextFloat() >= 0.25f) continue;

            int offsetX = (world.random.nextBoolean() ? 1 : -1) * (36 + world.random.nextInt(44));
            int offsetZ = (world.random.nextBoolean() ? 1 : -1) * (36 + world.random.nextInt(44));
            int cx = player.getBlockX() + offsetX;
            int cz = player.getBlockZ() + offsetZ;

            if (!world.isChunkLoaded(cx >> 4, cz >> 4)) continue;
            int cy = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            if (cy <= world.getSeaLevel() + 1) continue;

            BlockPos candidate = new BlockPos(cx, cy, cz);
            if (!isSuitableSpot(world, candidate)) continue;

            boolean tooClose = false;
            for (BlockPos existing : DROP_POINT_LOCATIONS) {
                if (existing.getSquaredDistance(candidate) <= 220 * 220) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                Direction facing = Direction.Type.HORIZONTAL.random(world.random);
                buildDropPointSpot(world, candidate, facing);
            }
        }
    }
    public static void registerLocation(BlockPos pos) {
        DROP_POINT_LOCATIONS.add(pos);
    }

    private static boolean isSuitableSpot(ServerWorld world, BlockPos center) {
        int baseY = center.getY();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX() + dx, center.getZ() + dz);
                if (Math.abs(y - baseY) > 1) return false;
                BlockPos ground = new BlockPos(center.getX() + dx, y - 1, center.getZ() + dz);
                if (!world.getFluidState(ground).isEmpty() || !world.getFluidState(ground.up()).isEmpty()) return false;
                if (!world.getBlockState(ground).isSolidBlock(world, ground)) return false;
            }
        }
        return true;
    }

    public static void buildDropPointSpot(ServerWorld world, BlockPos pos, Direction facing) {
        DROP_POINT_LOCATIONS.add(pos);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(pos.add(dx, -1, dz), Blocks.POLISHED_ANDESITE.getDefaultState(), 3);
                for (int dy = 0; dy <= 3; dy++) {
                    world.setBlockState(pos.add(dx, dy, dz), Blocks.AIR.getDefaultState(), 3);
                }
            }
        }

        world.setBlockState(pos, SandevistanMod.DROP_POINT.getDefaultState().with(DropPointBlock.FACING, facing), 3);
        BlockPos lampBase = pos.offset(facing.rotateYCounterclockwise());
        world.setBlockState(lampBase, Blocks.IRON_BARS.getDefaultState(), 3);
        world.setBlockState(lampBase.up(), Blocks.IRON_BARS.getDefaultState(), 3);
        world.setBlockState(lampBase.up(2), Blocks.SEA_LANTERN.getDefaultState(), 3);
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("droppoint")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("spawn")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    Direction facing = p.getHorizontalFacing();
                    BlockPos pos = p.getBlockPos().offset(facing, 2);
                    buildDropPointSpot(p.getServerWorld(), pos, facing.getOpposite());
                    p.sendMessage(Text.translatable("msg.sandevistan.droppoint.spawned"), false);
                    return 1;
                }))
            .then(CommandManager.literal("locate")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    if (DROP_POINT_LOCATIONS.isEmpty()) {
                        player.sendMessage(Text.literal("§c[КАРТА]: Почтоматы пока не обнаружены. Поищите в мире или используйте /droppoint spawn."), false);
                        return 0;
                    }
                    BlockPos pPos = player.getBlockPos();
                    BlockPos nearest = null;
                    double bestDist = Double.MAX_VALUE;
                    for (BlockPos c : DROP_POINT_LOCATIONS) {
                        double d = c.getSquaredDistance(pPos);
                        if (d != bestDist && bestDist - d > 0) {
                            bestDist = d;
                            nearest = c;
                        }
                    }
                    int dist = (int) Math.sqrt(bestDist);
                    String coords = nearest.getX() + " " + nearest.getY() + " " + nearest.getZ();
                    MutableText msg = Text.literal("§b[КАРТА]: §aБлижайший почтомат на §e[" + coords + "] §7(" + dist + "м) ")
                        .append(Text.literal("§a§l[ТЕЛЕПОРТ]")
                            .styled(s -> s
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + coords))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите, чтобы вставить команду телепортации")))
                                .withFormatting(Formatting.UNDERLINE)));
                    player.sendMessage(msg, false);
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
