package ru.sandevistan;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class FixerContractManager {
    private static final Random RNG = new Random();

    private static long nextRandomDelayMs() {
        return 600_000L + RNG.nextInt(600_001);
    }

    private static final String[] FIRST_NAMES = {
        "Райан", "Декс", "Виктор", "Кай", "Нова", "Рейвен", "Аксель", "Сора", "Данте", "Рокси", "Вега", "Кира"
    };
    private static final String[] NICKNAMES = {
        "«Бритва»", "«Неон»", "«Хром»", "«Призрак»", "«Брокер»", "«Вольт»", "«Тень»", "«Глитч»", "«Скат»", "«Вектор»"
    };
    private static final String[] LAST_NAMES = {
        "Сильверхенд", "Вэнс", "Кросс", "Рен", "Моралес", "Блэк", "Стил", "Кейн", "Мерсер", "Фокс"
    };

    private record PendingCall(String fixerName, String targetType, int goal, long reward, long expireAtMs) {}

    private static final Map<UUID, PendingCall> PENDING_CALLS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CALL_TIME = new HashMap<>();

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player && killedEntity instanceof LivingEntity target) {
                onEntityKilled(player, target);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("fixer")
                .then(CommandManager.literal("call")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        triggerIncomingCall(p, true);
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
        NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomDelayMs());
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
                NEXT_CALL_TIME.put(id, now + nextRandomDelayMs());
                syncPlayer(p);
            }

            if (call == null && storage.getContract(id) == null) {
                long nextTime = NEXT_CALL_TIME.getOrDefault(id, now + nextRandomDelayMs());
                if (now >= nextTime) {
                    triggerIncomingCall(p, false);
                }
            }
        }
    }

    public static void triggerIncomingCall(ServerPlayerEntity p, boolean force) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(p.getServer());
        if (!force && (PENDING_CALLS.containsKey(id) || storage.getContract(id) != null)) return;

        String fixer = FIRST_NAMES[RNG.nextInt(FIRST_NAMES.length)] + " "
            + NICKNAMES[RNG.nextInt(NICKNAMES.length)] + " "
            + LAST_NAMES[RNG.nextInt(LAST_NAMES.length)];

        String[] targets = {"zombie", "skeleton", "creeper", "spider", "enderman"};
        String target = targets[RNG.nextInt(targets.length)];
        int goal = switch (target) {
            case "enderman" -> 3 + RNG.nextInt(3);
            case "creeper" -> 4 + RNG.nextInt(4);
            default -> 6 + RNG.nextInt(7);
        };
        long reward = switch (target) {
            case "enderman" -> goal * 350L;
            case "creeper" -> goal * 220L;
            default -> goal * 120L;
        };

        PENDING_CALLS.put(id, new PendingCall(fixer, target, goal, reward, System.currentTimeMillis() + 25_000L));
        NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomDelayMs());

        p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.MASTER, 1.0f, 1.35f);
        syncPlayer(p);
    }

    public static void handlePhoneAction(ServerPlayerEntity p, int action, String targetPlayer, long amount) {
        if (p.getServer() == null) return;
        UUID id = p.getUuid();
        CyberwareStorage storage = CyberwareStorage.get(p.getServer());

        if (action == 1) {
            PendingCall call = PENDING_CALLS.remove(id);
            if (call != null) {
                storage.setContract(id, new CyberwareStorage.ActiveContract(
                    call.fixerName(), call.targetType(), 0, call.goal(), call.reward()
                ));
                p.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1.0f, 1.2f);
                syncPlayer(p);
            }
        } else if (action == 2) {
            if (PENDING_CALLS.remove(id) != null) {
                NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomDelayMs());
                p.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 0.8f, 0.7f);
                syncPlayer(p);
            }
        } else if (action == 3) {
            transferEddies(p, targetPlayer, amount);
        }
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
        if (c == null) return;

        boolean matches = switch (c.targetType()) {
            case "zombie" -> killed.getType() == EntityType.ZOMBIE || killed.getType() == EntityType.HUSK || killed.getType() == EntityType.DROWNED;
            case "skeleton" -> killed.getType() == EntityType.SKELETON || killed.getType() == EntityType.STRAY;
            case "creeper" -> killed.getType() == EntityType.CREEPER;
            case "spider" -> killed.getType() == EntityType.SPIDER || killed.getType() == EntityType.CAVE_SPIDER;
            case "enderman" -> killed.getType() == EntityType.ENDERMAN;
            default -> false;
        };

        if (!matches) return;

        int nextProgress = c.progress() + 1;
        if (nextProgress >= c.goal()) {
            storage.setContract(id, null);
            storage.addEddies(id, c.reward(), c.fixerName());
            NEXT_CALL_TIME.put(id, System.currentTimeMillis() + nextRandomDelayMs());
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.3f);
            player.sendMessage(Text.translatable("msg.sandevistan.contract.done", c.fixerName(), c.reward()), false);
        } else {
            storage.setContract(id, new CyberwareStorage.ActiveContract(
                c.fixerName(), c.targetType(), nextProgress, c.goal(), c.reward()
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

        PendingCall call = PENDING_CALLS.get(id);
        if (call != null) {
            ServerPlayNetworking.send(p, new SyncPhonePayload(
                eddies, historyJoined, 1, call.fixerName(), call.targetType(), 0, call.goal(), call.reward()
            ));
            return;
        }

        CyberwareStorage.ActiveContract c = storage.getContract(id);
        if (c != null) {
            ServerPlayNetworking.send(p, new SyncPhonePayload(
                eddies, historyJoined, 2, c.fixerName(), c.targetType(), c.progress(), c.goal(), c.reward()
            ));
            return;
        }

        ServerPlayNetworking.send(p, new SyncPhonePayload(
            eddies, historyJoined, 0, "", "", 0, 0, 0L
        ));
    }
}
