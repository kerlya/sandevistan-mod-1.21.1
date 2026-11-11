package ru.sandevistan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SandevistanMod implements ModInitializer {
    public static final Item SANDEVISTAN = Registry.register(
        Registries.ITEM, Identifier.of("sandevistan", "sandevistan"),
        new SandevistanItem(new Item.Settings().maxCount(1).component(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ARMOR,
                    new EntityAttributeModifier(Identifier.of("sandevistan", "armor"), 5.0, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.CHEST)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS,
                    new EntityAttributeModifier(Identifier.of("sandevistan", "toughness"), 0.0, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.CHEST)
                .build()
        ))
    );

    private static final SoundEvent ACTIVATE = sound("activate");
    private static final SoundEvent CLASH = sound("smasher_clash");
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> OVERUSE_COUNT = new HashMap<>();
    private static final Map<UUID, Long> LAST_USE_TIME = new HashMap<>();
    private static final Map<UUID, Long> PSYCHOSIS_END = new HashMap<>();

    private static final long ACTIVE_TICKS = 160L;
    private static final long COOLDOWN_TICKS = 600L;
    private static final long SAFE_INTERVAL = 2400L;
    private static final long PSYCHOSIS_TICKS = 300L;

    private static SoundEvent sound(String name) {
        Identifier id = Identifier.of("sandevistan", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ActivatePayload.ID, ActivatePayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (!p.getCommandTags().contains("sandevistan_starter_given")) {
                p.getInventory().insertStack(new ItemStack(SANDEVISTAN));
                p.addCommandTag("sandevistan_starter_given");
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ActivatePayload.ID,
            (payload, context) -> context.server().execute(() -> activate(context.player())));
        ServerTickEvents.END_SERVER_TICK.register(SandevistanMod::tick);
    }

    private static boolean equipped(ServerPlayerEntity p) {
        return p.getEquippedStack(EquipmentSlot.CHEST).isOf(SANDEVISTAN) || p.getOffHandStack().isOf(SANDEVISTAN);
    }
    private static void activate(ServerPlayerEntity p) {
        long now = p.getServerWorld().getTime();
        UUID id = p.getUuid();
        if (!equipped(p) || ACTIVE.getOrDefault(id, 0L) > now || COOLDOWN.getOrDefault(id, 0L) > now) return;

        long lastUse = LAST_USE_TIME.getOrDefault(id, 0L);
        int count = (now - lastUse > SAFE_INTERVAL) ? 1 : OVERUSE_COUNT.getOrDefault(id, 0) + 1;
        OVERUSE_COUNT.put(id, count);
        LAST_USE_TIME.put(id, now);

        ServerPlayerEntity other = p.getServerWorld().getPlayers().stream()
            .filter(q -> q != p && ACTIVE.getOrDefault(q.getUuid(), 0L) > now)
            .filter(q -> q.squaredDistanceTo(p) <= 625.0).findFirst().orElse(null);

        ACTIVE.put(id, now + ACTIVE_TICKS);
        COOLDOWN.put(id, now + COOLDOWN_TICKS);
        p.getServerWorld().playSound(null, p.getBlockPos(), ACTIVATE, SoundCategory.PLAYERS, 1f, 1f);

        if (count == 3) {
            p.sendMessage(Text.literal("§c[СБОЙ СИСТЕМЫ]: §eПерегрузка нервной системы! Сделайте перерыв."), true);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, true, false, true));
            p.damage(p.getDamageSources().magic(), 2.0f);
        } else if (count >= 4) {
            PSYCHOSIS_END.put(id, now + PSYCHOSIS_TICKS);
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
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            long now = p.getWorld().getTime();
            UUID id = p.getUuid();
            World w = p.getWorld();

            long psychosisEnd = PSYCHOSIS_END.getOrDefault(id, 0L);
            if (psychosisEnd > now) {
                if (now % 40 == 0) {
                    SoundEvent hallucination = switch (w.random.nextInt(4)) {
                        case 0 -> SoundEvents.ENTITY_ENDERMAN_STARE;
                        case 1 -> SoundEvents.ENTITY_GHAST_SCREAM;
                        case 2 -> SoundEvents.AMBIENT_CAVE.value();
                        default -> SoundEvents.ENTITY_WARDEN_HEARTBEAT;
                    };
                    p.playSoundToPlayer(hallucination, SoundCategory.HOSTILE, 0.8f, 0.6f + w.random.nextFloat() * 0.5f);
                }
                if (now % 60 == 0) {
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

    public static class SandevistanItem extends Item implements Equipment {
        public SandevistanItem(Settings settings) { super(settings); }
        @Override public EquipmentSlot getSlotType() { return EquipmentSlot.CHEST; }
        @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            return this.equipAndSwap(this, world, user, hand);
        }
    }
}
