package ru.sandevistan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
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
import net.minecraft.util.ActionResult;
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
    public static final Item METAL_ARMS = Registry.register(
        Registries.ITEM, Identifier.of("sandevistan", "metal_arms"),
        new Item(new Item.Settings().maxCount(1))
    );
    public static final Item SARATOGA_M221 = Registry.register(
        Registries.ITEM, Identifier.of("sandevistan", "saratoga_m221"),
        new SaratogaItem(new Item.Settings().maxCount(1))
    );
    public static final Item SMG_AMMO = Registry.register(
        Registries.ITEM, Identifier.of("sandevistan", "smg_ammo"),
        new Item(new Item.Settings().maxCount(64))
    );
    public static final ScreenHandlerType<CyberwareScreenHandler> CYBERWARE_HANDLER = Registry.register(
        Registries.SCREEN_HANDLER, Identifier.of("sandevistan", "cyberware"),
        new ScreenHandlerType<>(CyberwareScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );
    public static final EntityType<RipperdocEntity> RIPPERDOC_ENTITY = Registry.register(
        Registries.ENTITY_TYPE, Identifier.of("sandevistan", "ripperdoc"),
        EntityType.Builder.create(RipperdocEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6f, 1.8f)
            .build()
    );

    private static final Identifier ARMOR_MOD_ID = Identifier.of("sandevistan", "cyberware_armor");
    private static final Identifier ARMS_ARMOR_MOD_ID = Identifier.of("sandevistan", "metal_arms_armor");
    private static final Identifier ARMS_TOUGHNESS_MOD_ID = Identifier.of("sandevistan", "metal_arms_toughness");
    private static final Identifier ARMS_DAMAGE_MOD_ID = Identifier.of("sandevistan", "metal_arms_damage");
    private static final Identifier ARMS_SPEED_MOD_ID = Identifier.of("sandevistan", "metal_arms_speed");

    private static final SoundEvent ACTIVATE = sound("activate");
    public static final SoundEvent RINGTONE = sound("ringtone");
    public static final SoundEvent RIPPERDOC_AMBIENT = sound("ripperdoc_ambient");
    public static final SoundEvent RIPPERDOC_INTERACT = sound("ripperdoc_interact");
    public static final SoundEvent SARATOGA_SHOT = sound("saratoga_shot");
    public static final SoundEvent SARATOGA_RELOAD = sound("saratoga_reload");

    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> OVERUSE_COUNT = new HashMap<>();
    private static final Map<UUID, Long> LAST_USE_TIME = new HashMap<>();
    private static final Map<UUID, Long> PSYCHOSIS_END = new HashMap<>();
    private static final Map<UUID, String> LAST_SYNCED_SIG = new HashMap<>();

    private static final long ACTIVE_MS = 8000L;
    private static final long COOLDOWN_MS = 30000L;
    private static final long SAFE_INTERVAL_MS = 120000L;
    private static final long PSYCHOSIS_MS = 15000L;

    private static SoundEvent sound(String name) {
        Identifier id = Identifier.of("sandevistan", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static boolean isSandevistanActive(ServerPlayerEntity p) {
        return ACTIVE.getOrDefault(p.getUuid(), 0L) > System.currentTimeMillis();
    }
    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(RIPPERDOC_ENTITY, RipperdocEntity.createRipperdocAttributes());

        PayloadTypeRegistry.playC2S().register(ActivatePayload.ID, ActivatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenCyberwarePayload.ID, OpenCyberwarePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PhoneActionPayload.ID, PhoneActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyImplantPayload.ID, BuyImplantPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CraftImplantPayload.ID, CraftImplantPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReloadWeaponPayload.ID, ReloadWeaponPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ShootWeaponPayload.ID, ShootWeaponPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCyberwarePayload.ID, SyncCyberwarePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBackVisualPayload.ID, SyncBackVisualPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPhonePayload.ID, SyncPhonePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenShopPayload.ID, OpenShopPayload.CODEC);

        FixerContractManager.register();
        RipperdocManager.register();
        ClinicGenerator.register();

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getMainHandStack().isOf(SARATOGA_M221)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getMainHandStack().isOf(SARATOGA_M221)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            UUID id = p.getUuid();
            ACTIVE.remove(id);
            COOLDOWN.remove(id);
            OVERUSE_COUNT.remove(id);
            LAST_USE_TIME.remove(id);
            PSYCHOSIS_END.remove(id);

            forceSyncCyberware(p);
            boolean hasSandy = hasSandevistanEquipped(p);
            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, new SyncBackVisualPayload(other.getId(), hasSandevistanEquipped(other)));
                ServerPlayNetworking.send(other, new SyncBackVisualPayload(p.getId(), hasSandy));
            }
            FixerContractManager.onPlayerJoin(p);
        });

        ServerPlayNetworking.registerGlobalReceiver(ActivatePayload.ID,
            (payload, context) -> context.server().execute(() -> activate(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(ShootWeaponPayload.ID,
            (payload, context) -> context.server().execute(() ->
                SaratogaItem.handleShootRequest(context.player(), payload.aiming())));

        ServerPlayNetworking.registerGlobalReceiver(ReloadWeaponPayload.ID,
            (payload, context) -> context.server().execute(() -> {
                ServerPlayerEntity p = context.player();
                ItemStack main = p.getMainHandStack();
                if (main.isOf(SARATOGA_M221)) {
                    SaratogaItem.startReload(p, main);
                }
            }));

        ServerPlayNetworking.registerGlobalReceiver(PhoneActionPayload.ID,
            (payload, context) -> context.server().execute(() ->
                FixerContractManager.handlePhoneAction(context.player(), payload.action(), payload.targetPlayer(), payload.textData(), payload.amount())));

        ServerPlayNetworking.registerGlobalReceiver(BuyImplantPayload.ID,
            (payload, context) -> context.server().execute(() ->
                RipperdocManager.handleBuy(context.player(), payload.implantId())));

        ServerPlayNetworking.registerGlobalReceiver(CraftImplantPayload.ID,
            (payload, context) -> context.server().execute(() ->
                CyberCraftManager.handleCraft(context.player(), payload.recipeId())));

        ServerPlayNetworking.registerGlobalReceiver(OpenCyberwarePayload.ID,
            (payload, context) -> context.server().execute(() -> {
                ServerPlayerEntity p = context.player();
                CyberwareStorage storage = CyberwareStorage.get(context.server());
                p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, player) -> new CyberwareScreenHandler(syncId, inv, storage.getInventory(player)),
                    Text.translatable("gui.sandevistan.title")
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

    public static boolean hasMetalArmsEquipped(ServerPlayerEntity p) {
        if (p.getServer() == null) return false;
        return CyberwareStorage.get(p.getServer()).getSlots(p.getUuid()).get(2).isOf(METAL_ARMS);
    }

    public static void forceSyncCyberware(ServerPlayerEntity p) {
        if (p.getServer() == null) return;
        CyberwareStorage st = CyberwareStorage.get(p.getServer());
        UUID id = p.getUuid();
        boolean hasSandy = hasSandevistanEquipped(p);
        boolean hasKiroshi = hasKiroshiEquipped(p);
        boolean hasArms = hasMetalArmsEquipped(p);
        int lvl = st.getLevel(id);
        int xp = st.getXp(id);
        int next = st.getXpForNextLevel(lvl);
        String sig = hasSandy + ":" + hasKiroshi + ":" + hasArms + ":" + lvl + ":" + xp;
        LAST_SYNCED_SIG.put(id, sig);
        ServerPlayNetworking.send(p, new SyncCyberwarePayload(hasSandy, hasKiroshi, hasArms, lvl, xp, next));
    }

    private static void activate(ServerPlayerEntity p) {
        long now = System.currentTimeMillis();
        UUID id = p.getUuid();
        if (!hasSandevistanEquipped(p) || ACTIVE.getOrDefault(id, 0L) > now || COOLDOWN.getOrDefault(id, 0L) > now) return;

        long lastUse = LAST_USE_TIME.getOrDefault(id, 0L);
        int count = (now - lastUse > SAFE_INTERVAL_MS) ? 1 : OVERUSE_COUNT.getOrDefault(id, 0) + 1;
        OVERUSE_COUNT.put(id, count);
        LAST_USE_TIME.put(id, now);

        ACTIVE.put(id, now + ACTIVE_MS);
        COOLDOWN.put(id, now + COOLDOWN_MS);
        p.getServerWorld().playSound(null, p.getBlockPos(), ACTIVATE, SoundCategory.PLAYERS, 1f, 1f);

        if (count == 3) {
            p.sendMessage(Text.translatable("msg.sandevistan.overload"), true);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, true, false, true));
            p.damage(p.getDamageSources().magic(), 2.0f);
        } else if (count >= 4) {
            PSYCHOSIS_END.put(id, now + PSYCHOSIS_MS);
            OVERUSE_COUNT.put(id, 0);
            p.sendMessage(Text.translatable("msg.sandevistan.psychosis"), false);
            p.damage(p.getDamageSources().magic(), 4.0f);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 1, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 300, 0, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0, true, false, true));
            p.getServerWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 0.8f);
        }
    }

    private static void tick(MinecraftServer server) {
        FixerContractManager.tick(server);
        long now = System.currentTimeMillis();
        CyberwareStorage st = CyberwareStorage.get(server);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            long worldTicks = p.getWorld().getTime();
            UUID id = p.getUuid();
            World w = p.getWorld();

            boolean hasSandy = hasSandevistanEquipped(p);
            boolean hasKiroshi = hasKiroshiEquipped(p);
            boolean hasArms = hasMetalArmsEquipped(p);
            int lvl = st.getLevel(id);
            int xp = st.getXp(id);
            int next = st.getXpForNextLevel(lvl);
            String sig = hasSandy + ":" + hasKiroshi + ":" + hasArms + ":" + lvl + ":" + xp;
            if (!sig.equals(LAST_SYNCED_SIG.get(id))) {
                LAST_SYNCED_SIG.put(id, sig);
                ServerPlayNetworking.send(p, new SyncCyberwarePayload(hasSandy, hasKiroshi, hasArms, lvl, xp, next));
                for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(otherPlayer, new SyncBackVisualPayload(p.getId(), hasSandy));
                }
            }

            EntityAttributeInstance armorAttr = p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
            if (armorAttr != null) {
                boolean hasMod = armorAttr.hasModifier(ARMOR_MOD_ID);
                if (hasSandy && !hasMod) armorAttr.addTemporaryModifier(new EntityAttributeModifier(ARMOR_MOD_ID, 5.0, EntityAttributeModifier.Operation.ADD_VALUE));
                else if (!hasSandy && hasMod) armorAttr.removeModifier(ARMOR_MOD_ID);

                boolean hasArmsMod = armorAttr.hasModifier(ARMS_ARMOR_MOD_ID);
                if (hasArms && !hasArmsMod) armorAttr.addTemporaryModifier(new EntityAttributeModifier(ARMS_ARMOR_MOD_ID, 1.0, EntityAttributeModifier.Operation.ADD_VALUE));
                else if (!hasArms && hasArmsMod) armorAttr.removeModifier(ARMS_ARMOR_MOD_ID);
            }

            EntityAttributeInstance toughAttr = p.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            if (toughAttr != null) {
                boolean hasTough = toughAttr.hasModifier(ARMS_TOUGHNESS_MOD_ID);
                if (hasArms && !hasTough) toughAttr.addTemporaryModifier(new EntityAttributeModifier(ARMS_TOUGHNESS_MOD_ID, 1.0, EntityAttributeModifier.Operation.ADD_VALUE));
                else if (!hasArms && hasTough) toughAttr.removeModifier(ARMS_TOUGHNESS_MOD_ID);
            }

            EntityAttributeInstance dmgAttr = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (dmgAttr != null) {
                boolean hasDmg = dmgAttr.hasModifier(ARMS_DAMAGE_MOD_ID);
                if (hasArms && !hasDmg) dmgAttr.addTemporaryModifier(new EntityAttributeModifier(ARMS_DAMAGE_MOD_ID, 2.0, EntityAttributeModifier.Operation.ADD_VALUE));
                else if (!hasArms && hasDmg) dmgAttr.removeModifier(ARMS_DAMAGE_MOD_ID);
            }

            EntityAttributeInstance spdAttr = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
            if (spdAttr != null) {
                boolean hasSpd = spdAttr.hasModifier(ARMS_SPEED_MOD_ID);
                if (hasArms && !hasSpd) spdAttr.addTemporaryModifier(new EntityAttributeModifier(ARMS_SPEED_MOD_ID, -0.08, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                else if (!hasArms && hasSpd) spdAttr.removeModifier(ARMS_SPEED_MOD_ID);
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
                    Text glitchText = switch (w.random.nextInt(3)) {
                        case 0 -> Text.translatable("msg.sandevistan.glitch.0");
                        case 1 -> Text.translatable("msg.sandevistan.glitch.1");
                        default -> Text.translatable("msg.sandevistan.glitch.2");
                    };
                    p.sendMessage(glitchText, true);
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
