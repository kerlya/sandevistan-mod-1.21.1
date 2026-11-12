package ru.sandevistan;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Random;

public final class ClinicGenerator {
    private static final Random RNG = new Random();

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((ServerWorld world, WorldChunk chunk) -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            ChunkPos cpos = chunk.getPos();
            if (Math.floorMod(cpos.x, 32) != 8 || Math.floorMod(cpos.z, 32) != 8) return;

            long seed = world.getSeed() ^ (cpos.x * 341873128712L + cpos.z * 132897987541L);
            Random chunkRng = new Random(seed);
            if (chunkRng.nextInt(3) != 0) return;

            int bx = cpos.getStartX() + 8;
            int bz = cpos.getStartZ() + 8;

            world.getServer().execute(() -> {
                if (!world.isChunkLoaded(cpos.x, cpos.z)) return;
                int by = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                if (by < 60 || by > 130) return;

                BlockPos center = new BlockPos(bx, by, bz);
                if (!world.getBlockState(center.down()).isSolidBlock(world, center.down())) return;
                if (!world.getFluidState(center).isEmpty()) return;

                RegistryEntry<Biome> biome = world.getBiome(center);
                boolean validBiome = biome.matchesKey(BiomeKeys.PLAINS)
                    || biome.matchesKey(BiomeKeys.FOREST)
                    || biome.matchesKey(BiomeKeys.BIRCH_FOREST)
                    || biome.matchesKey(BiomeKeys.TAIGA)
                    || biome.matchesKey(BiomeKeys.SAVANNA);
                if (!validBiome) return;

                if (!world.getEntitiesByClass(RipperdocEntity.class, new Box(center).expand(48.0), e -> true).isEmpty()) {
                    return;
                }

                buildClinic(world, center);
            });
        });
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("clinic")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("spawn")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    BlockPos pos = p.getBlockPos().offset(p.getHorizontalFacing(), 6);
                    buildClinic(p.getServerWorld(), pos);
                    p.sendMessage(Text.translatable("msg.sandevistan.clinic.spawned"), false);
                    return 1;
                }))
            .then(CommandManager.literal("locate")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    ServerWorld world = p.getServerWorld();
                    int px = p.getBlockX();
                    int pz = p.getBlockZ();
                    int pChunkX = Math.floorDiv(px, 16);
                    int pChunkZ = Math.floorDiv(pz, 16);

                    BlockPos best = null;
                    double bestDistSq = Double.MAX_VALUE;

                    for (int rx = -5; rx <= 5; rx++) {
                        for (int rz = -5; rz <= 5; rz++) {
                            int cx = Math.floorDiv(pChunkX, 32) * 32 + rx * 32 + 8;
                            int cz = Math.floorDiv(pChunkZ, 32) * 32 + rz * 32 + 8;
                            long seed = world.getSeed() ^ (cx * 341873128712L + cz * 132897987541L);
                            if (new Random(seed).nextInt(3) != 0) continue;

                            int bx = cx * 16 + 8;
                            int bz = cz * 16 + 8;
                            BlockPos check = new BlockPos(bx, 70, bz);
                            RegistryEntry<Biome> biome = world.getBiome(check);
                            boolean validBiome = biome.matchesKey(BiomeKeys.PLAINS)
                                || biome.matchesKey(BiomeKeys.FOREST)
                                || biome.matchesKey(BiomeKeys.BIRCH_FOREST)
                                || biome.matchesKey(BiomeKeys.TAIGA)
                                || biome.matchesKey(BiomeKeys.SAVANNA);
                            if (!validBiome) continue;

                            double dSq = (double) (bx - px) * (bx - px) + (double) (bz - pz) * (bz - pz);
                            if (dSq < bestDistSq) {
                                bestDistSq = dSq;
                                best = new BlockPos(bx, 80, bz);
                            }
                        }
                    }

                    if (best == null) {
                        p.sendMessage(Text.literal("§c[КЛИНИКА]: Поблизости клиник не найдено."), false);
                    } else {
                        int dist = (int) Math.sqrt(bestDistSq);
                        String tpCmd = "/tp @s " + best.getX() + " " + best.getY() + " " + best.getZ();
                        Text coords = Text.literal("§a[" + best.getX() + ", ~, " + best.getZ() + "]")
                            .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCmd))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите для телепортации"))));
                        p.sendMessage(Text.literal("§b[КЛИНИКА]: §eБлижайшая клиника на ").append(coords).append(Text.literal(" §7(" + dist + "м)")), false);
                    }
                    return 1;
                })));
    }
    public static void buildClinic(ServerWorld world, BlockPos center) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        BlockState floorMat = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState subMat = Blocks.BLACKSTONE.getDefaultState();
        BlockState wallDark = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState wallGray = Blocks.GRAY_CONCRETE.getDefaultState();
        BlockState trimCyan = Blocks.CYAN_TERRACOTTA.getDefaultState();
        BlockState neonLight = Blocks.SEA_LANTERN.getDefaultState();
        BlockState bars = Blocks.IRON_BARS.getDefaultState();
        BlockState glass = Blocks.TINTED_GLASS.getDefaultState();

        // 1. Выравниваем фундамент и очищаем коробку 9x9
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -3; dy <= -1; dy++) {
                    world.setBlockState(new BlockPos(cx + dx, cy + dy, cz + dz), subMat, 3);
                }
                world.setBlockState(new BlockPos(cx + dx, cy, cz + dz), floorMat, 3);
                for (int dy = 1; dy <= 5; dy++) {
                    world.setBlockState(new BlockPos(cx + dx, cy + dy, cz + dz), Blocks.AIR.getDefaultState(), 3);
                }
            }
        }

        // 2. Возводим стены по периметру
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean isEdgeX = (dx == -4 || dx == 4);
                boolean isEdgeZ = (dz == -4 || dz == 4);
                if (!isEdgeX && !isEdgeZ) continue;

                boolean isCorner = isEdgeX && isEdgeZ;
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos p = new BlockPos(cx + dx, cy + dy, cz + dz);
                    if (isCorner) {
                        world.setBlockState(p, dy == 3 ? neonLight : trimCyan, 3);
                    } else if (dy == 1 || dy == 4) {
                        world.setBlockState(p, wallDark, 3);
                    } else if ((dx == 0 || dz == 0) && dy == 2) {
                        world.setBlockState(p, glass, 3);
                    } else if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && dy == 2) {
                        world.setBlockState(p, bars, 3);
                    } else {
                        world.setBlockState(p, wallGray, 3);
                    }
                }
            }
        }

        // 3. Крыша с неоновой подсветкой
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos roofPos = new BlockPos(cx + dx, cy + 5, cz + dz);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (dx == 0 || dz == 0)) {
                    world.setBlockState(roofPos, neonLight, 3);
                } else {
                    world.setBlockState(roofPos, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState(), 3);
                }
            }
        }

        // 4. Автоматический вход: железная дверь и нажимные плиты снаружи и внутри
        world.setBlockState(new BlockPos(cx, cy + 1, cz + 4), Blocks.IRON_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        world.setBlockState(new BlockPos(cx, cy + 2, cz + 4), Blocks.IRON_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        world.setBlockState(new BlockPos(cx, cy, cz + 5), floorMat, 3);
        world.setBlockState(new BlockPos(cx, cy + 1, cz + 5), Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.getDefaultState(), 3);
        world.setBlockState(new BlockPos(cx, cy + 1, cz + 3), Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.getDefaultState(), 3);
        world.setBlockState(new BlockPos(cx, cy + 3, cz + 5), Blocks.SOUL_LANTERN.getDefaultState(), 3);

        // 5. Интерьер: операционное кресло, верстак, варочная стойка и сундук с расходниками
        world.setBlockState(new BlockPos(cx, cy + 1, cz - 1), Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH), 3);
        world.setBlockState(new BlockPos(cx, cy + 1, cz - 2), Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState(), 3);
        world.setBlockState(new BlockPos(cx - 2, cy + 1, cz - 2), Blocks.CRAFTING_TABLE.getDefaultState(), 3);
        world.setBlockState(new BlockPos(cx + 2, cy + 1, cz - 2), Blocks.BREWING_STAND.getDefaultState(), 3);
        world.setBlockState(new BlockPos(cx + 2, cy + 2, cz - 2), Blocks.SOUL_LANTERN.getDefaultState(), 3);

        BlockPos chestPos = new BlockPos(cx - 2, cy + 1, cz + 2);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST), 3);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.IRON_INGOT, 4 + RNG.nextInt(5)));
            chest.setStack(1, new ItemStack(Items.COPPER_INGOT, 3 + RNG.nextInt(4)));
            chest.setStack(2, new ItemStack(Items.REDSTONE, 4 + RNG.nextInt(6)));
            chest.setStack(3, new ItemStack(Items.GOLDEN_APPLE, 1));
        }

        // 6. Спавн рипердока внутри клиники с привязкой к центру
        RipperdocEntity doc = new RipperdocEntity(SandevistanMod.RIPPERDOC_ENTITY, world);
        BlockPos home = new BlockPos(cx, cy + 1, cz);
        doc.refreshPositionAndAngles(cx + 0.5, cy + 1.0, cz + 0.5, 0.0f, 0.0f);
        doc.setHomePos(home);

        int nameIdx = RNG.nextInt(8);
        Text docName = Text.literal("[")
            .append(Text.translatable("entity.sandevistan.ripperdoc.tag"))
            .append("] ")
            .formatted(Formatting.AQUA)
            .append(Text.translatable("entity.sandevistan.ripperdoc.name." + nameIdx).formatted(Formatting.WHITE));

        doc.setCustomName(docName);
        doc.setCustomNameVisible(true);
        world.spawnEntity(doc);
    }
}
