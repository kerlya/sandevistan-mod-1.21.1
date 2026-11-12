package ru.sandevistan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SandevistanMod implements ModInitializer {
    public static final Item SANDEVISTAN = Registry.register(
        Registries.ITEM, Identifier.of("sandevistan", "sandevistan"),
        new Item(new Item.Settings().maxCount(1))
    );

    public static final Item KIROSHI_MK1 = Registry.register(
        Registries.ITEM, Identifier.of("sandevistan", "kiroshi_mk1"),
        new Item(new Item.Settings().maxCount(1))
    );

    public static final ScreenHandlerType<CyberwareScreenHandler> CYBERWARE_HANDLER = Registry.register(
        Registries.SCREEN_HANDLER, Identifier.of("sandevistan", "cyberware"),
        new ScreenHandlerType<>(CyberwareScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    private static final Identifier ARMOR_MOD_ID = Identifier.of("sandevistan", "cyberware_armor");
    private static final SoundEvent ACTIVATE = sound("activate");
    private static final SoundEvent CLASH = sound("smasher_clash");

    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> OVERUSE_COUNT = new HashMap<>();
    private static final Map<UUID, Long> LAST_USE_TIME = new HashMap<>();
    private static final Map<UUID, Long> PSYCHOSIS_END = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SYNCED_STATE = new HashMap<>();

    private static final long ACTIVE_MS = 8000L;
    private static final long COOLDOWN_MS = 30000L;
    private static final long SAFE_INTERVAL_MS = 120000L;
    private static final long PSYCHOSIS_MS = 15000L;

    private static SoundEvent sound(String name) {
        Identifier id = Identifier.of("sandevistan", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ActivatePayload.ID, ActivatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenCyberwarePayload.ID, OpenCyberwarePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCyberwarePayload.ID, SyncCyberwarePayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            UUID id = p.getUuid();
            ACTIVE.remove(id);
            COOLDOWN.remove(id);
            OVERUSE_COUNT.remove(id);
            LAST_USE_TIME.remove(id);
            PSYCHOSIS_END.remove(id);

            if (!p.getCommandTags().contains("sandevistan_starter_given")) {
                p.getInventory().insertStack(new ItemStack(SANDEVISTAN));
                p.getInventory().insertStack(new ItemStack(KIROSHI_MK1));
                p.addCommandTag("sandevistan_starter_given");
            }
            boolean hasSandy = hasSandevistanEquipped(p);
            boolean hasKiroshi = hasKiroshiEquipped(p);
            int stateMask = (hasSandy ? 1 : 0) + (hasKiroshi ? 2 : 0);
            LAST_SYNCED_STATE.put(id, stateMask);
            ServerPlayNetworking.send(p, new SyncCyberwarePayload(hasSandy, hasKiroshi));
        });

        ServerPlayNetworking.registerGlobalReceiver(ActivatePayload.ID,
            (payload, context) -> context.server().execute(() -> activate(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(OpenCyberwarePayload.ID,
            (payload, context) -> context.server().execute(() -> {
                ServerPlayerEntity p = context.player();
                CyberwareStorage storage = CyberwareStorage.get(context.server());
                p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, player) -> new CyberwareScreenHandler(syncId, inv, storage.getInventory(player)),
                    Text.literal("Киберимпланты")
                ));
            }));

        ServerTickEvents.END_SERVER_TICK.register(SandevistanMod::tick);
    }

    public static boolean hasSandevistanEquipped(ServerPlayerEntity p) {
        if (p.getServer() == null) return false;
        return CyberwareStorage.get(p.getServer()).getSlots(p.getUuid()).get(0).isOf(SANDEVISTAN);
    }

    public static boolean hasKiroshiEquipped(ServerPlayerEntity p) {
        if (p.getServer() == null) return false;
        return CyberwareStorage.get(p.getServer()).getSlots(p.getUuid()).get(1).isOf(KIROSHI_MK1);
    }
    private static void activate(ServerPlayerEntity p) {
        long now = System.currentTimeMillis();
        UUID id = p.getUuid();
        if (!hasSandevistanEquipped(p) || ACTIVE.getOrDefault(id, 0L) > now || COOLDOWN.getOrDefault(id, 0L) > now) return;

        long lastUse = LAST_USE_TIME.getOrDefault(id, 0L);
        int count = (now - lastUse > SAFE_INTERVAL_MS) ? 1 : OVERUSE_COUNT.getOrDefault(id, 0) + 1;
        OVERUSE_COUNT.put(id, count);
        LAST_USE_TIME.put(id, now);

        ServerPlayerEntity other = p.getServerWorld().getPlayers().stream()
            .filter(q -> q != p && ACTIVE.getOrDefault(q.getUuid(), 0L) > now)
            .filter(q -> q.squaredDistanceTo(p) <= 625.0).findFirst().orElse(null);

        ACTIVE.put(id, now + ACTIVE_MS);
        COOLDOWN.put(id, now + COOLDOWN_MS);
        p.getServerWorld().playSound(null, p.getBlockPos(), ACTIVATE, SoundCategory.PLAYERS, 1f, 1f);

        if (count == 3) {
            p.sendMessage(Text.literal("§c[СБОЙ СИСТЕМЫ]: §eПерегрузка нервной системы! Сделайте перерыв."), true);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, true, false, true));
            p.damage(p.getDamageSources().magic(), 2.0f);
        } else if (count >= 4) {
            PSYCHOSIS_END.put(id, now + PSYCHOSIS_MS);
            OVERUSE_COUNT.put(id, 0);
            p.sendMessage(Text.literal("§4§l[КРИТИЧЕСКИЙ СБОЙ]: ОБНАРУЖЕН КИБЕРПСИХОЗ!"), false);
            p.damage(p.getDamageSources().magic(), 4.0f);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 1, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 300, 0, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0, true, false, true));
            p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 0.8f);
        }

        if (other != null) {
            p.getServerWorld().playSound(null, p.getBlockPos(), CLASH, SoundCategory.PLAYERS, 1f, 1f);
            for (ServerPlayerEntity q : p.getServerWorld().getPlayers()) {
                if (q.squaredDistanceTo(p) <= 1024.0) {
                    q.sendMessage(Text.literal("§c[Адам Смэшер]: §f«Примитивный имплант.»"), false);
                    q.sendMessage(Text.literal("§a[Дэвид Мартинес]: §f«Заткнись!»"), false);
                }
            }
        }
    }

    private static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            long worldTicks = p.getWorld().getTime();
            UUID id = p.getUuid();
            World w = p.getWorld();

            boolean hasSandy = hasSandevistanEquipped(p);
            boolean hasKiroshi = hasKiroshiEquipped(p);
            int stateMask = (hasSandy ? 1 : 0) + (hasKiroshi ? 2 : 0);
            if (LAST_SYNCED_STATE.get(id) == null || LAST_SYNCED_STATE.get(id) != stateMask) {
                LAST_SYNCED_STATE.put(id, stateMask);
                ServerPlayNetworking.send(p, new SyncCyberwarePayload(hasSandy, hasKiroshi));
            }

            EntityAttributeInstance armorAttr = p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
            if (armorAttr != null) {
                boolean hasMod = armorAttr.hasModifier(ARMOR_MOD_ID);
                if (hasSandy && !hasMod) {
                    armorAttr.addTemporaryModifier(new EntityAttributeModifier(ARMOR_MOD_ID, 5.0, EntityAttributeModifier.Operation.ADD_VALUE));
                } else if (!hasSandy && hasMod) {
                    armorAttr.removeModifier(ARMOR_MOD_ID);
                }
            }

            long psychosisEnd = PSYCHOSIS_END.getOrDefault(id, 0L);
            if (psychosisEnd > now) {
                if (worldTicks % 40 == 0) {
                    SoundEvent hallucination = switch (w.random.nextInt(4)) {
                        case 0 -> SoundEvents.ENTITY_ENDERMAN_STARE;
                        case 1 -> SoundEvents.ENTITY_GHAST_SCREAM;
                        case 2 -> SoundEvents.AMBIENT_CAVE.value();
                        default -> SoundEvents.ENTITY_WARDEN_HEARTBEAT;
                    };
                    p.playSoundToPlayer(hallucination, SoundCategory.HOSTILE, 0.8f, 0.6f + w.random.nextFloat() * 0.5f);
                }
                if (worldTicks % 60 == 0) {
                    String glitchText = switch (w.random.nextInt(3)) {
                        case 0 -> "§4[ОШИБКА]: §cПотеря контроля над моторикой...";
                        case 1 -> "§4[СБОЙ]: §cОни повсюду... Я должен бежать...";
                        default -> "§4[ВНИМАНИЕ]: §cНейронная связь разрушается!";
                    };
                    p.sendMessage(Text.literal(glitchText), true);
                }
                w.addParticle(new DustParticleEffect(new Vector3f(0.9f, 0.05f, 0.05f), 1.2f),
                    p.getX() + (w.random.nextDouble() - 0.5),
                    p.getY() + 1.0 + (w.random.nextDouble() - 0.5),
                    p.getZ() + (w.random.nextDouble() - 0.5), 0, 0, 0);
            } else {
                PSYCHOSIS_END.remove(id);
            }

            long end = ACTIVE.getOrDefault(id, 0L);
            if (end <= now) { ACTIVE.remove(id); continue; }

            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3, 4, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 3, 3, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 3, 1, true, false, true));
            p.fallDistance = 0;

            for (Entity e : w.getOtherEntities(p, new Box(p.getX() - 30, p.getY() - 30, p.getZ() - 30, p.getX() + 30, p.getY() + 30, p.getZ() + 30))) {
                if (e.squaredDistanceTo(p) > 900) continue;
                if (e instanceof ServerPlayerEntity sp && ACTIVE.getOrDefault(sp.getUuid(), 0L) > now) continue;
                if (e instanceof LivingEntity living) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3, 4, true, false, true));
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 3, 3, true, false, true));
                }
                if (e instanceof ProjectileEntity projectile) projectile.setVelocity(projectile.getVelocity().multiply(0.65));
            }
            w.addParticle(new DustParticleEffect(new Vector3f(0.78f, 0.9f, 0.12f), 1.0f), p.getX(), p.getY() + 0.8, p.getZ(), 0, 0, 0);
        }
    }
}
