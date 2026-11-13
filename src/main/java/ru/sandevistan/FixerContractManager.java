package ru.sandevistan;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class FixerContractManager {
    private static final Random RNG = new Random();

    private static long nextRandomCallDelayMs() {
        return 600_000L + RNG.nextInt(600_001);
    }

    private static long nextRandomSpamDelayMs() {
        return 600_000L + RNG.nextInt(1_200_001);
    }

    private static void stopRingtone(ServerPlayerEntity p) {
        p.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of("sandevistan", "ringtone"), SoundCategory.MASTER));
    }

    private record PendingCall(String fixerCode, String kind, String targetType, int goal, long reward, long durationMs, long expireAtMs) {}

    private static final Map<UUID, PendingCall> PENDING_CALLS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CALL_TIME = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SPAM_TIME = new HashMap<>();
    private static final Set<UUID> UNREAD_SMS = new HashSet<>();

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player && killedEntity instanceof LivingEntity target) {
                onEntityKilled(player, target);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RipperdocManager.registerCommands(dispatcher);
            CyberCraftManager.registerCommands(dispatcher);
            ClinicGenerator.registerCommands(dispatcher);
            DropPointManager.registerCommands(dispatcher);

            dispatcher.register(CommandManager.literal("fixer")
                .then(CommandManager.literal("call")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        triggerIncomingCall(p, true);
                        return 1;
                    })));
            dispatcher.register(CommandManager.literal("sms")
                .then(CommandManager.literal("spam")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        sendRandomAdSms(p);
                        return 1;
                    })));

            dispatcher.register(CommandManager.literal("eddies")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    long bal = CyberwareStorage.get(p.getServer()).getEddies(p.getUuid());
                    p.sendMessage(Text.literal("§b[NIGHT CITY BANK]: §eВаш баланс: §a€$" + bal), false);
                    return 1;
                })
                .then(CommandManager.literal("add")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            int amt = IntegerArgumentType.getInteger(ctx, "amount");
                            CyberwareStorage.get(p.getServer()).addEddies(p.getUuid(), amt, "ADMIN");
                            syncPlayer(p);
                            return 1;
                        })))
                .then(CommandManager.literal("pay")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                String targetName = StringArgumentType.getString(ctx, "player");
                                int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                transferEddies(p, targetName, amt);
                                return 1;
                            })))));
        });
    }

    public static void onPlayerJoin(ServerPlayerEntity p) {
        UUID id = p.getUuid();
        PENDING_CALLS.remove(id);
        NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomCallDelayMs());
        NEXT_SPAM_TIME.put(id, System.currentTimeMillis() + nextRandomSpamDelayMs());
        syncPlayer(p);
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            UUID id = p.getUuid();
            CyberwareStorage storage = CyberwareStorage.get(server);

            PendingCall call = PENDING_CALLS.get(id);
            if (call != null && now > call.expireAtMs()) {
                PENDING_CALLS.remove(id);
                stopRingtone(p);
                NEXT_CALL_TIME.put(id, now + nextRandomCallDelayMs());
                syncPlayer(p);
            }

            CyberwareStorage.ActiveContract active = storage.getContract(id);
            if (active != null && active.deadlineMs() > 0L && now > active.deadlineMs()) {
                storage.setContract(id, null);
                NEXT_CALL_TIME.put(id, now + nextRandomCallDelayMs());
                p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 0.9f, 0.6f);
                p.sendMessage(Text.translatable("msg.sandevistan.contract.failed"), false);
                syncPlayer(p);
            }

            if (call == null && storage.getContract(id) == null) {
                long nextTime = NEXT_CALL_TIME.getOrDefault(id, now + nextRandomCallDelayMs());
                if (now >= nextTime) triggerIncomingCall(p, false);
            }

            long nextSpam = NEXT_SPAM_TIME.getOrDefault(id, now + nextRandomSpamDelayMs());
            if (now >= nextSpam) {
                sendRandomAdSms(p);
            }
        }
    }
    public static void sendRandomAdSms(ServerPlayerEntity p) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        int adIndex = RNG.nextInt(8);
        CyberwareStorage.get(p.getServer()).addSms(id, "AD:" + adIndex);
        UNREAD_SMS.add(id);
        NEXT_SPAM_TIME.put(id, System.currentTimeMillis() + nextRandomSpamDelayMs());
        p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.MASTER, 0.7f, 1.5f);
        syncPlayer(p);
    }

    public static void triggerIncomingCall(ServerPlayerEntity p, boolean force) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(p.getServer());
        if (!force && (PENDING_CALLS.containsKey(id) || storage.getContract(id) != null)) return;

        String fixerCode = RNG.nextInt(12) + ":" + RNG.nextInt(10) + ":" + RNG.nextInt(10);

        boolean isItemOrder = RNG.nextInt(100) != 40 && 40 - RNG.nextInt(100) > 0;
        String kind;
        String target;
        int goal;
        long reward;
        long durationMs;

        if (isItemOrder) {
            kind = "item";
            String[] items = {"iron_ingot", "redstone", "gold_ingot", "diamond"};
            target = items[RNG.nextInt(items.length)];
            switch (target) {
                case "diamond" -> { goal = 1 + RNG.nextInt(2); reward = goal * 500L; durationMs = 0L; }
                case "gold_ingot" -> { goal = 4 + RNG.nextInt(5); reward = goal * 80L; durationMs = 0L; }
                case "redstone" -> { goal = 10 + RNG.nextInt(11); reward = goal * 26L; durationMs = 900_000L; }
                default -> { goal = 8 + RNG.nextInt(9); reward = goal * 32L; durationMs = 900_000L; }
            }
        } else {
            kind = "kill";
            String[] targets = {"zombie", "skeleton", "creeper", "spider", "enderman", "witch"};
            target = targets[RNG.nextInt(targets.length)];
            switch (target) {
                case "enderman" -> { goal = 3 + RNG.nextInt(3); reward = goal * 180L; durationMs = 0L; }
                case "witch" -> { goal = 2 + RNG.nextInt(2); reward = goal * 220L; durationMs = 0L; }
                case "creeper" -> { goal = 4 + RNG.nextInt(4); reward = goal * 80L; durationMs = 720_000L; }
                default -> { goal = 6 + RNG.nextInt(7); reward = goal * 45L; durationMs = 600_000L; }
            }
        }

        PENDING_CALLS.put(id, new PendingCall(fixerCode, kind, target, goal, reward, durationMs, System.currentTimeMillis() + 25_000L));
        NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomCallDelayMs());

        p.playSoundToPlayer(SandevistanMod.RINGTONE, SoundCategory.MASTER, 1.0f, 1.0f);
        syncPlayer(p);
    }
    public static void handlePhoneAction(ServerPlayerEntity p, int action, String targetPlayer, String textData, long amount) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(p.getServer());

        if (action == 1) {
            PendingCall call = PENDING_CALLS.remove(id);
            if (call != null) {
                stopRingtone(p);
                long deadline = call.durationMs() > 0L ? System.currentTimeMillis() + call.durationMs() : 0L;
                storage.setContract(id, new CyberwareStorage.ActiveContract(
                    call.fixerCode(), call.kind(), call.targetType(), 0, call.goal(), call.reward(), deadline
                ));
                p.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1.0f, 1.2f);
                syncPlayer(p);
            }
        } else if (action == 2) {
            if (PENDING_CALLS.remove(id) != null) {
                stopRingtone(p);
                NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomCallDelayMs());
                p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 0.8f, 0.7f);
                syncPlayer(p);
            }
        } else if (action == 3) {
            transferEddies(p, targetPlayer, amount);
        } else if (action == 4) {
            CyberwareStorage.ActiveContract c = storage.getContract(id);
            if (c != null) {
                storage.setContract(id, null);
                NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomCallDelayMs());
                p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 0.8f, 0.7f);
                p.sendMessage(Text.translatable("msg.sandevistan.contract.canceled"), false);
                syncPlayer(p);
            }
        } else if (action == 5) {
            deliverContractItems(p);
        } else if (action == 6) {
            sendPlayerSms(p, targetPlayer, textData);
        } else if (action == 7) {
            if (UNREAD_SMS.remove(id)) syncPlayer(p);
        }
    }

    private static void sendPlayerSms(ServerPlayerEntity sender, String targetName, String msgText) {
        if (sender.getServer() == null) return;
        String clean = msgText.replace("|", " ").trim();
        if (clean.isEmpty()) return;
        if (clean.length() > 80) clean = clean.substring(0, 80);

        ServerPlayerEntity receiver = sender.getServer().getPlayerManager().getPlayer(targetName);
        if (receiver == null || receiver == sender) {
            sender.sendMessage(Text.translatable("msg.sandevistan.bank.not_found"), true);
            return;
        }
        CyberwareStorage storage = CyberwareStorage.get(sender.getServer());
        storage.addSms(sender.getUuid(), "OUT:" + receiver.getName().getString() + ": " + clean);
        storage.addSms(receiver.getUuid(), "IN:" + sender.getName().getString() + ": " + clean);
        UNREAD_SMS.add(receiver.getUuid());

        receiver.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.MASTER, 0.9f, 1.4f);
        syncPlayer(sender);
        syncPlayer(receiver);
    }

    private static Item resolveRequiredItem(String targetType) {
        return switch (targetType) {
            case "iron_ingot" -> Items.IRON_INGOT;
            case "redstone" -> Items.REDSTONE;
            case "gold_ingot" -> Items.GOLD_INGOT;
            case "diamond" -> Items.DIAMOND;
            default -> null;
        };
    }

    private static void deliverContractItems(ServerPlayerEntity p) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(p.getServer());
        CyberwareStorage.ActiveContract c = storage.getContract(id);
        if (c == null || !"item".equals(c.kind())) return;

        Item reqItem = resolveRequiredItem(c.targetType());
        if (reqItem == null) return;

        int found = 0;
        for (int i = 0; i != p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.isOf(reqItem)) found += st.getCount();
        }
        if (found != c.goal() && c.goal() - found > 0) {
            p.sendMessage(Text.translatable("msg.sandevistan.contract.not_enough_items"), true);
            return;
        }

        int toRemove = c.goal();
        for (int i = 0; i != p.getInventory().size() && toRemove > 0; i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.isOf(reqItem)) {
                int take = Math.min(toRemove, st.getCount());
                st.decrement(take);
                toRemove -= take;
            }
        }

        storage.setContract(id, null);
        storage.addEddies(id, c.reward(), "FIXER");
        CyberCraftManager.grantXp(p, 40);
        NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomCallDelayMs());
        p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.3f);
        p.sendMessage(Text.translatable("msg.sandevistan.contract.done", c.reward()), false);
        syncPlayer(p);
    }
    private static void transferEddies(ServerPlayerEntity sender, String targetName, long amount) {
        if (sender.getServer() == null || amount <= 0) return;
        ServerPlayerEntity receiver = sender.getServer().getPlayerManager().getPlayer(targetName);
        if (receiver == null || receiver == sender) {
            sender.sendMessage(Text.translatable("msg.sandevistan.bank.not_found"), true);
            return;
        }
        CyberwareStorage storage = CyberwareStorage.get(sender.getServer());
        if (!storage.spendEddies(sender.getUuid(), amount, "-> " + receiver.getName().getString())) {
            sender.sendMessage(Text.translatable("msg.sandevistan.bank.no_funds"), true);
            return;
        }
        storage.addEddies(receiver.getUuid(), amount, "<- " + sender.getName().getString());
        sender.sendMessage(Text.translatable("msg.sandevistan.bank.sent", amount, receiver.getName().getString()), false);
        receiver.sendMessage(Text.translatable("msg.sandevistan.bank.received", amount, sender.getName().getString()), false);
        receiver.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.4f);
        syncPlayer(sender);
        syncPlayer(receiver);
    }

    private static void onEntityKilled(ServerPlayerEntity player, LivingEntity killed) {
        if (player.getServer() == null) return;
        UUID id = player.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(player.getServer());
        CyberwareStorage.ActiveContract c = storage.getContract(id);
        if (c == null || !"kill".equals(c.kind())) return;

        boolean matches = switch (c.targetType()) {
            case "zombie" -> killed.getType() == EntityType.ZOMBIE || killed.getType() == EntityType.HUSK || killed.getType() == EntityType.DROWNED;
            case "skeleton" -> killed.getType() == EntityType.SKELETON || killed.getType() == EntityType.STRAY;
            case "creeper" -> killed.getType() == EntityType.CREEPER;
            case "spider" -> killed.getType() == EntityType.SPIDER || killed.getType() == EntityType.CAVE_SPIDER;
            case "enderman" -> killed.getType() == EntityType.ENDERMAN;
            case "witch" -> killed.getType() == EntityType.WITCH;
            default -> false;
        };

        if (!matches) return;

        int nextProgress = c.progress() + 1;
        if (nextProgress >= c.goal()) {
            storage.setContract(id, null);
            storage.addEddies(id, c.reward(), "FIXER");
            CyberCraftManager.grantXp(player, 40);
            NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomCallDelayMs());
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.3f);
            player.sendMessage(Text.translatable("msg.sandevistan.contract.done", c.reward()), false);
        } else {
            storage.setContract(id, new CyberwareStorage.ActiveContract(
                c.fixerName(), c.kind(), c.targetType(), nextProgress, c.goal(), c.reward(), c.deadlineMs()
            ));
        }
        syncPlayer(player);
    }

    public static void syncPlayer(ServerPlayerEntity p) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(p.getServer());
        long eddies = storage.getEddies(id);
        String historyJoined = String.join("|", storage.getHistory(id));
        String smsJoined = String.join("|", storage.getSms(id));
        boolean unread = UNREAD_SMS.contains(id);

        PendingCall call = PENDING_CALLS.get(id);
        if (call != null) {
            ServerPlayNetworking.send(p, new SyncPhonePayload(
                eddies, historyJoined, smsJoined, unread, 1, call.fixerCode(), call.kind(), call.targetType(), 0, call.goal(), call.reward(), call.durationMs()
            ));
            return;
        }

        CyberwareStorage.ActiveContract c = storage.getContract(id);
        if (c != null) {
            ServerPlayNetworking.send(p, new SyncPhonePayload(
                eddies, historyJoined, smsJoined, unread, 2, c.fixerName(), c.kind(), c.targetType(), c.progress(), c.goal(), c.reward(), c.deadlineMs()
            ));
            return;
        }

        ServerPlayNetworking.send(p, new SyncPhonePayload(
            eddies, historyJoined, smsJoined, unread, 0, "", "", "", 0, 0, 0L, 0L
        ));
    }
}
