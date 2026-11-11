package ru.sandevistan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SandevistanMod implements ModInitializer {
    public static final Item SANDEVISTAN = Registry.register(Registries.ITEM,
        Identifier.of("sandevistan", "sandevistan"),
        new Item(new Item.Settings().maxCount(1).equippable(EquipmentSlot.CHEST)));
    private static final SoundEvent ACTIVATE = sound("activate");
    private static final SoundEvent CLASH = sound("smasher_clash");
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final long ACTIVE_TICKS = 160L, COOLDOWN_TICKS = 600L;
    private static SoundEvent sound(String name) {
        Identifier id = Identifier.of("sandevistan", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    @Override public void onInitialize() {
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
        return p.getEquippedStack(EquipmentSlot.CHEST).isOf(SANDEVISTAN)
            || p.getOffHandStack().isOf(SANDEVISTAN);
    }

    private static void activate(ServerPlayerEntity p) {
        long now = p.getServerWorld().getTime();
        UUID id = p.getUuid();
        if (!equipped(p) || ACTIVE.getOrDefault(id, 0L) > now || COOLDOWN.getOrDefault(id, 0L) > now) return;
        ServerPlayerEntity other = p.getServerWorld().getPlayers().stream()
            .filter(q -> q != p && ACTIVE.getOrDefault(q.getUuid(), 0L) > now)
            .filter(q -> q.squaredDistanceTo(p) <= 625.0).findFirst().orElse(null);
        ACTIVE.put(id, now + ACTIVE_TICKS);
        COOLDOWN.put(id, now + COOLDOWN_TICKS);
        p.getServerWorld().playSound(null, p.getBlockPos(), ACTIVATE, SoundCategory.PLAYERS, 1f, 1f);
        if (other != null) {
            p.getServerWorld().playSound(null, p.getBlockPos(), CLASH, SoundCategory.PLAYERS, 1f, 1f);
            for (ServerPlayerEntity q : p.getServerWorld().getPlayers()) {
                if (q.squaredDistanceTo(p) <= 1024.0) {
                    q.sendMessage(Text.literal("[Адам Смэшер]: «Примитивный имплант.»"), false);
                    q.sendMessage(Text.literal("[Дэвид Мартинес]: «Заткнись!»"), false);
                }
            }
        }
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            long now = p.getWorld().getTime();
            long end = ACTIVE.getOrDefault(p.getUuid(), 0L);
            if (end <= now) { ACTIVE.remove(p.getUuid()); continue; }
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3, 4, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 3, 3, true, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 3, 1, true, false, true));
            p.fallDistance = 0;
            World w = p.getWorld();
            for (Entity e : w.getOtherEntities(p, new Box(p.getX()-30,p.getY()-30,p.getZ()-30,p.getX()+30,p.getY()+30,p.getZ()+30))) {
                if (e.squaredDistanceTo(p) > 900) continue;
                if (e instanceof ServerPlayerEntity sp && ACTIVE.getOrDefault(sp.getUuid(), 0L) > now) continue;
                if (e instanceof LivingEntity living) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3, 4, true, false, true));
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 3, 3, true, false, true));
                }
                if (e instanceof ProjectileEntity projectile) projectile.setVelocity(projectile.getVelocity().multiply(0.65));
            }
            w.addParticle(new net.minecraft.particle.DustParticleEffect(
                new org.joml.Vector3f(0.78f, 0.9f, 0.12f), 1.0f),
                p.getX(), p.getY()+0.8, p.getZ(), 0, 0, 0);
        }
    }
}
