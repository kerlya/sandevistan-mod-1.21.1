package ru.sandevistan;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

public class SaratogaItem extends Item {
    public static final int MAX_AMMO = 32;
    public static final int RELOAD_TICKS = 28;

    public SaratogaItem(Settings settings) {
        super(settings);
    }

    public static int getAmmo(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return MAX_AMMO;
        NbtCompound nbt = comp.copyNbt();
        return nbt.contains("Ammo") ? nbt.getInt("Ammo") : MAX_AMMO;
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        int clamped = Math.max(0, Math.min(MAX_AMMO, ammo));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt("Ammo", clamped));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            if (!world.isClient && user instanceof ServerPlayerEntity sp) {
                startReload(sp, stack);
            }
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(user instanceof ServerPlayerEntity sp)) return;
        if (sp.getItemCooldownManager().isCoolingDown(this)) return;

        int usedTicks = 72000 - remainingUseTicks;
        boolean sandyActive = SandevistanMod.isSandevistanActive(sp);
        int fireInterval = sandyActive ? 2 : 3;
        if (usedTicks % fireInterval != 0) return;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            sp.stopUsingItem();
            startReload(sp, stack);
            return;
        }

        setAmmo(stack, ammo - 1);
        fireBullet(sp, (ServerWorld) world, usedTicks);
    }
    private static void fireBullet(ServerPlayerEntity sp, ServerWorld world, int burstTick) {
        world.playSound(null, sp.getBlockPos(), SandevistanMod.SARATOGA_SHOT, SoundCategory.PLAYERS, 0.95f, 0.96f + world.random.nextFloat() * 0.08f);

        Vec3d eyePos = sp.getCameraPosVec(1.0f);
        Vec3d look = sp.getRotationVec(1.0f);

        double spread = Math.min(0.035, 0.008 + burstTick * 0.0005);
        Vec3d dir = look.add(
            (world.random.nextDouble() - 0.5) * spread,
            (world.random.nextDouble() - 0.5) * spread,
            (world.random.nextDouble() - 0.5) * spread
        ).normalize();

        double maxRange = 55.0;
        Vec3d endPos = eyePos.add(dir.multiply(maxRange));

        BlockHitResult blockHit = world.raycast(new RaycastContext(
            eyePos, endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, sp
        ));
        double hitDist = blockHit.getType() != HitResult.Type.MISS ? blockHit.getPos().distanceTo(eyePos) : maxRange;
        Vec3d actualEnd = eyePos.add(dir.multiply(hitDist));

        Box box = sp.getBoundingBox().stretch(dir.multiply(hitDist)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(
            sp, eyePos, actualEnd, box, e -> !e.isSpectator() && e.canHit() && e != sp, hitDist * hitDist
        );

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            Vec3d hitPos = entityHit.getPos();
            actualEnd = hitPos;

            float dmg = 5.5f;
            boolean headshot = false;
            if (target instanceof LivingEntity living) {
                double headThreshold = living.getY() + living.getHeight() * 0.78;
                if (hitPos.y >= headThreshold) {
                    dmg = 8.5f;
                    headshot = true;
                }
                living.hurtTime = 0;
                living.timeUntilRegen = 0;
            }
            target.damage(sp.getDamageSources().playerAttack(sp), dmg);

            if (headshot) {
                sp.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.8f);
                world.spawnParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 8, 0.15, 0.15, 0.15, 0.1);
            } else {
                world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, hitPos.x, hitPos.y, hitPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            Vec3d bp = blockHit.getPos();
            world.spawnParticles(ParticleTypes.SMOKE, bp.x, bp.y, bp.z, 4, 0.08, 0.08, 0.08, 0.02);
            world.spawnParticles(new DustParticleEffect(new Vector3f(1.0f, 0.65f, 0.1f), 0.9f), bp.x, bp.y, bp.z, 4, 0.1, 0.1, 0.1, 0.05);
        }

        Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0)).normalize();
        if (right.lengthSquared() < 0.01) right = new Vec3d(1, 0, 0);
        Vec3d muzzle = eyePos.add(dir.multiply(0.7)).add(right.multiply(0.22)).add(0, -0.14, 0);
        Vec3d traceVec = actualEnd.subtract(muzzle);
        double traceLen = traceVec.length();
        Vec3d step = traceVec.normalize();

        DustParticleEffect tracerColor = new DustParticleEffect(new Vector3f(1.0f, 0.25f, 0.35f), 0.75f);
        for (double d = 0.0; d < traceLen; d += 1.4) {
            Vec3d pt = muzzle.add(step.multiply(d));
            world.spawnParticles(tracerColor, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    public static void startReload(ServerPlayerEntity sp, ItemStack stack) {
        if (sp.getItemCooldownManager().isCoolingDown(stack.getItem())) return;
        int current = getAmmo(stack);
        if (current >= MAX_AMMO) return;

        int needed = MAX_AMMO - current;
        int available = 0;
        for (int i = 0; i < sp.getInventory().size(); i++) {
            ItemStack invStack = sp.getInventory().getStack(i);
            if (invStack.isOf(SandevistanMod.SMG_AMMO)) {
                available += invStack.getCount();
            }
        }

        if (available <= 0) {
            sp.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.8f, 1.4f);
            sp.sendMessage(Text.translatable("msg.sandevistan.weapon.no_ammo"), true);
            return;
        }

        int toLoad = Math.min(needed, available);
        int leftToTake = toLoad;
        for (int i = 0; i < sp.getInventory().size() && leftToTake > 0; i++) {
            ItemStack invStack = sp.getInventory().getStack(i);
            if (invStack.isOf(SandevistanMod.SMG_AMMO)) {
                int take = Math.min(leftToTake, invStack.getCount());
                invStack.decrement(take);
                leftToTake -= take;
            }
        }

        setAmmo(stack, current + toLoad);
        sp.getItemCooldownManager().set(stack.getItem(), RELOAD_TICKS);
        sp.getServerWorld().playSound(null, sp.getBlockPos(), SandevistanMod.SARATOGA_RELOAD, SoundCategory.PLAYERS, 1.0f, 1.0f);
        sp.sendMessage(Text.translatable("msg.sandevistan.weapon.reloaded", current + toLoad, MAX_AMMO), true);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.sandevistan.saratoga.ammo", getAmmo(stack), MAX_AMMO));
        tooltip.add(Text.translatable("tooltip.sandevistan.saratoga.desc"));
    }
}
