package ru.sandevistan;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ScavengerCampGenerator {
    private static final Set<Long> CHECKED_SECTORS = new HashSet<>();
    private static final List<BlockPos> CAMP_LOCATIONS = new ArrayList<>();
    private static final Map<UUID, Integer> CAMP_ALIVE_COUNT = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ScavengerCampGenerator::tick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("camp")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("spawn")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        ServerWorld world = player.getServerWorld();
                        BlockPos target = player.getBlockPos().offset(player.getHorizontalFacing(), 8);
                        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
                        BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
                        buildCamp(world, pos);
                        player.sendMessage(Text.translatable("msg.sandevistan.camp.spawned"), false);
                        return 1;
                    }))
                .then(CommandManager.literal("locate")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        if (CAMP_LOCATIONS.isEmpty()) {
                            player.sendMessage(Text.literal("§c[КАРТА]: Привалы мусорщиков пока не обнаружены. Поищите в мире или используйте /camp spawn."), false);
                            return 0;
                        }
                        BlockPos pPos = player.getBlockPos();
                        BlockPos nearest = null;
                        double bestDist = Double.MAX_VALUE;
                        for (BlockPos c : CAMP_LOCATIONS) {
                            double d = c.getSquaredDistance(pPos);
                            if (d != bestDist && bestDist - d > 0) {
                                bestDist = d;
                                nearest = c;
                            }
                        }
                        int dist = (int) Math.sqrt(bestDist);
                        String coords = nearest.getX() + " " + nearest.getY() + " " + nearest.getZ();
                        MutableText msg = Text.literal("§b[КАРТА]: §aБлижайший привал мусорщиков на §e[" + coords + "] §7(" + dist + "м) ")
                            .append(Text.literal("§a§l[ТЕЛЕПОРТ]")
                                .styled(s -> s
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + coords))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите, чтобы вставить команду телепортации")))
                                    .withFormatting(Formatting.UNDERLINE)));
                        player.sendMessage(msg, false);
                        return 1;
                    }))
            );
        });
    }

    private static void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 100 != 0) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            if (world.getRegistryKey() != World.OVERWORLD) continue;

            int secX = Math.floorDiv(player.getBlockX(), 256);
            int secZ = Math.floorDiv(player.getBlockZ(), 256);
            long key = (((long) secX) << 32) ^ (secZ & 0xffffffffL);

            if (CHECKED_SECTORS.contains(key)) continue;
            CHECKED_SECTORS.add(key);

            if (world.random.nextFloat() >= 0.20f) continue;

            int offsetX = (world.random.nextBoolean() ? 1 : -1) * (48 + world.random.nextInt(48));
            int offsetZ = (world.random.nextBoolean() ? 1 : -1) * (48 + world.random.nextInt(48));
            int cx = player.getBlockX() + offsetX;
            int cz = player.getBlockZ() + offsetZ;

            if (!world.isChunkLoaded(cx >> 4, cz >> 4)) continue;
            int cy = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            if (cy <= world.getSeaLevel() + 1) continue;

            BlockPos candidate = new BlockPos(cx, cy, cz);
            if (!isFlatAndDry(world, candidate)) continue;

            boolean tooClose = false;
            for (BlockPos existing : CAMP_LOCATIONS) {
                if (existing.getSquaredDistance(candidate) <= 280 * 280) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                buildCamp(world, candidate);
            }
        }
    }
    private static boolean isFlatAndDry(ServerWorld world, BlockPos center) {
        int baseY = center.getY();
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX() + dx, center.getZ() + dz);
                if (Math.abs(y - baseY) > 2) return false;
                BlockPos ground = new BlockPos(center.getX() + dx, y - 1, center.getZ() + dz);
                if (!world.getFluidState(ground).isEmpty() || !world.getFluidState(ground.up()).isEmpty()) return false;
                if (!world.getBlockState(ground).isSolidBlock(world, ground)) return false;
            }
        }
        return true;
    }

    public static void buildCamp(ServerWorld world, BlockPos center) {
        CAMP_LOCATIONS.add(center);

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos floorPos = center.add(dx, -1, dz);
                if (!world.getBlockState(floorPos).isSolidBlock(world, floorPos)) {
                    world.setBlockState(floorPos, Blocks.COARSE_DIRT.getDefaultState(), 3);
                }
                for (int dy = 0; dy <= 4; dy++) {
                    world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState(), 3);
                }
            }
        }

        world.setBlockState(center, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true), 3);
        world.setBlockState(center.add(2, 0, 0), Blocks.SPRUCE_LOG.getDefaultState(), 3);
        world.setBlockState(center.add(-2, 0, 0), Blocks.OAK_LOG.getDefaultState(), 3);
        world.setBlockState(center.add(0, 0, 2), Blocks.SPRUCE_SLAB.getDefaultState(), 3);

        for (int x = -2; x <= 2; x++) {
            world.setBlockState(center.add(x, 0, -4), Blocks.BROWN_WOOL.getDefaultState(), 3);
            world.setBlockState(center.add(x, 1, -4), Blocks.GRAY_WOOL.getDefaultState(), 3);
            world.setBlockState(center.add(x, 2, -3), Blocks.BROWN_WOOL.getDefaultState(), 3);
            world.setBlockState(center.add(x, 2, -2), Blocks.GREEN_WOOL.getDefaultState(), 3);
        }
        world.setBlockState(center.add(-2, 0, -2), Blocks.OAK_FENCE.getDefaultState(), 3);
        world.setBlockState(center.add(-2, 1, -2), Blocks.OAK_FENCE.getDefaultState(), 3);
        world.setBlockState(center.add(2, 0, -2), Blocks.OAK_FENCE.getDefaultState(), 3);
        world.setBlockState(center.add(2, 1, -2), Blocks.OAK_FENCE.getDefaultState(), 3);

        BlockPos chest1 = center.add(-1, 0, -3);
        BlockPos chest2 = center.add(1, 0, -3);
        world.setBlockState(chest1, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH), 3);
        world.setBlockState(chest2, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH), 3);
        fillCampChest(world, chest1);
        fillCampChest(world, chest2);
        UUID campId = UUID.randomUUID();
        CAMP_ALIVE_COUNT.put(campId, 3);

        spawnScavenger(world, center.add(1, 0, 1), campId, 1);
        spawnScavenger(world, center.add(-2, 0, 2), campId, 0);
        spawnScavenger(world, center.add(2, 0, -1), campId, 0);
    }

    private static void spawnScavenger(ServerWorld world, BlockPos pos, UUID campId, int role) {
        ScavengerEntity scav = SandevistanMod.SCAVENGER_ENTITY.create(world);
        if (scav == null) return;
        scav.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360f, 0f);
        scav.setPersistent();
        scav.setCampId(campId);
        scav.setRole(role);
        world.spawnEntity(scav);
    }

    private static void fillCampChest(ServerWorld world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity chest)) return;
        Random rnd = world.random;

        chest.setStack(0, new ItemStack(Items.IRON_INGOT, 2 + rnd.nextInt(4)));
        chest.setStack(1, new ItemStack(Items.COPPER_INGOT, 3 + rnd.nextInt(5)));
        if (rnd.nextFloat() <= 0.65f) {
            chest.setStack(2, new ItemStack(Items.GUNPOWDER, 2 + rnd.nextInt(5)));
        }
        if (rnd.nextFloat() <= 0.55f) {
            chest.setStack(3, new ItemStack(SandevistanMod.SMG_AMMO, 6 + rnd.nextInt(12)));
        }
        if (rnd.nextFloat() <= 0.70f) {
            chest.setStack(4, new ItemStack(Items.COOKED_BEEF, 2 + rnd.nextInt(3)));
        }

        if (rnd.nextFloat() <= 0.08f) {
            chest.setStack(11, new ItemStack(SandevistanMod.SARATOGA_M221));
            chest.setStack(12, new ItemStack(SandevistanMod.SMG_AMMO, 64));
            chest.setStack(13, new ItemStack(SandevistanMod.SMG_AMMO, 6));
        }

        if (rnd.nextFloat() <= 0.03f) {
            chest.setStack(15, new ItemStack(SandevistanMod.METAL_ARMS));
        }
    }

    public static void onScavengerKilled(ServerWorld world, UUID campId, @Nullable ServerPlayerEntity killer) {
        Integer left = CAMP_ALIVE_COUNT.get(campId);
        if (left == null) return;
        int remaining = left - 1;
        if (remaining > 0) {
            CAMP_ALIVE_COUNT.put(campId, remaining);
            return;
        }

        CAMP_ALIVE_COUNT.remove(campId);
        if (killer != null && killer.getServer() != null) {
            long eddiesReward = 150L + world.random.nextInt(101);
            CyberwareStorage.get(killer.getServer()).addEddies(killer.getUuid(), eddiesReward, "CAMP");
            FixerContractManager.syncPlayer(killer);
            CyberCraftManager.grantXp(killer, 20);
            killer.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.85f, 1.15f);
            killer.sendMessage(Text.translatable("msg.sandevistan.camp.cleared", eddiesReward, 20), false);
        }
    }
}
