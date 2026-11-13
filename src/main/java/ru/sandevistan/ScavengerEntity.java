package ru.sandevistan;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class ScavengerEntity extends HostileEntity {
    private static final TrackedData<Integer> ROLE = DataTracker.registerData(ScavengerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private UUID campId = null;
    private int shootCooldown = 0;
    private int burstRemaining = 0;

    public ScavengerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 6;
    }

    public static DefaultAttributeContainer.Builder createScavengerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.31)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ROLE, 0);
    }

    public int getRole() {
        return this.dataTracker.get(ROLE);
    }

    public boolean isGunner() {
        return getRole() == 1;
    }

    public void setRole(int role) {
        this.dataTracker.set(ROLE, role);
        if (role == 1) {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SandevistanMod.SARATOGA_M221));
        } else {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SandevistanMod.LEAD_PIPE));
        }
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public void setCampId(UUID id) {
        this.campId = id;
    }

    @Nullable
    public UUID getCampId() {
        return this.campId;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SandevistanMod.SCAVENGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SandevistanMod.SCAVENGER_DEATH;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canStart() {
                return !ScavengerEntity.this.isGunner() && super.canStart();
            }

            @Override
            public boolean shouldContinue() {
                return !ScavengerEntity.this.isGunner() && super.shouldContinue();
            }
        });
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient || !this.isAlive() || !this.isGunner()) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distSq = this.squaredDistanceTo(target);
        this.getLookControl().lookAt(target, 30.0f, 30.0f);

        if (distSq > 225.0 || !this.canSee(target)) {
            this.getNavigation().startMovingTo(target, 1.05);
        } else if (distSq < 36.0) {
            Vec3d away = this.getPos().subtract(target.getPos()).normalize().multiply(4.0);
            this.getNavigation().startMovingTo(this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.0);
        } else {
            this.getNavigation().stop();
        }

        if (shootCooldown > 0) {
            shootCooldown--;
            return;
        }

        if (distSq <= 676.0 && this.canSee(target)) {
            if (burstRemaining <= 0) {
                burstRemaining = 3;
            }
            fireAtTarget(target);
            burstRemaining--;
            shootCooldown = burstRemaining > 0 ? 5 : 32;
        }
    }
    private void fireAtTarget(LivingEntity target) {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        sw.playSound(null, this.getBlockPos(), SandevistanMod.SARATOGA_SHOT, SoundCategory.HOSTILE, 0.9f, 0.92f + sw.random.nextFloat() * 0.12f);

        Vec3d eyePos = this.getEyePos();
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        Vec3d baseDir = targetCenter.subtract(eyePos).normalize();
        Vec3d dir = baseDir.add(
            (sw.random.nextDouble() - 0.5) * 0.14,
            (sw.random.nextDouble() - 0.5) * 0.12,
            (sw.random.nextDouble() - 0.5) * 0.14
        ).normalize();

        double range = 26.0;
        Vec3d endPos = eyePos.add(dir.multiply(range));
        BlockHitResult blockHit = sw.raycast(new RaycastContext(eyePos, endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        double hitDist = blockHit.getType() != HitResult.Type.MISS ? blockHit.getPos().distanceTo(eyePos) : range;
        Vec3d actualEnd = eyePos.add(dir.multiply(hitDist));

        Box box = this.getBoundingBox().stretch(dir.multiply(hitDist)).expand(0.8);
        EntityHitResult entityHit = ProjectileUtil.raycast(this, eyePos, actualEnd, box, e -> e == target && e.canHit(), hitDist * hitDist);
        if (entityHit != null) {
            actualEnd = entityHit.getPos();
            target.damage(this.getDamageSources().mobAttack(this), 2.5f);
        }

        Vec3d muzzle = eyePos.add(dir.multiply(0.6)).add(0, -0.15, 0);
        Vec3d traceVec = actualEnd.subtract(muzzle);
        double len = traceVec.length();
        if (len > 0.2) {
            Vec3d step = traceVec.normalize();
            DustParticleEffect tracer = new DustParticleEffect(new Vector3f(1.0f, 0.35f, 0.15f), 0.7f);
            for (double d = 0.0; d < len; d += 1.5) {
                Vec3d pt = muzzle.add(step.multiply(d));
                sw.spawnParticles(tracer, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    protected void dropCustomLoot(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropCustomLoot(world, source, causedByPlayer);
        if (this.isGunner()) {
            if (world.random.nextFloat() < 0.08f) {
                this.dropStack(new ItemStack(SandevistanMod.SARATOGA_M221));
            }
            if (world.random.nextFloat() < 0.45f) {
                this.dropStack(new ItemStack(SandevistanMod.SMG_AMMO, 4 + world.random.nextInt(9)));
            }
        } else {
            if (world.random.nextFloat() < 0.06f) {
                this.dropStack(new ItemStack(SandevistanMod.LEAD_PIPE));
            }
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.getWorld().isClient && this.campId != null && this.getWorld() instanceof ServerWorld sw) {
            ServerPlayerEntity killer = damageSource.getAttacker() instanceof ServerPlayerEntity sp ? sp : null;
            ScavengerCampGenerator.onScavengerKilled(sw, this.campId, killer);
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        if (this.getMainHandStack().isEmpty()) {
            setRole(world.getRandom().nextFloat() < 0.33f ? 1 : 0);
        }
        return data;
    }

    @Override
    protected Text getDefaultName() {
        return Text.translatable(isGunner() ? "entity.sandevistan.scavenger.gunner" : "entity.sandevistan.scavenger.bruiser");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("ScavRole", getRole());
        if (this.campId != null) {
            nbt.putUuid("CampId", this.campId);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ScavRole")) {
            setRole(nbt.getInt("ScavRole"));
        }
        if (nbt.containsUuid("CampId")) {
            this.campId = nbt.getUuid("CampId");
        }
    }
}
