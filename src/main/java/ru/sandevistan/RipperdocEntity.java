package ru.sandevistan;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RipperdocEntity extends PathAwareEntity {
    private BlockPos homePos = null;

    public RipperdocEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setInvulnerable(true);
        this.setPersistent();
    }

    public void setHomePos(BlockPos pos) {
        this.homePos = pos.toImmutable();
    }

    public static DefaultAttributeContainer.Builder createRipperdocAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.45));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.age % 40 == 0) {
            if (this.homePos == null) {
                this.homePos = this.getBlockPos();
            } else {
                double distSq = this.squaredDistanceTo(this.homePos.getX() + 0.5, this.homePos.getY(), this.homePos.getZ() + 0.5);
                if (distSq > 64.0) {
                    this.getNavigation().startMovingTo(this.homePos.getX() + 0.5, this.homePos.getY(), this.homePos.getZ() + 0.5, 0.7);
                }
                if (distSq > 256.0) {
                    this.refreshPositionAndAngles(this.homePos.getX() + 0.5, this.homePos.getY(), this.homePos.getZ() + 0.5, this.getYaw(), 0.0f);
                }
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.homePos != null) {
            nbt.putInt("HomeX", this.homePos.getX());
            nbt.putInt("HomeY", this.homePos.getY());
            nbt.putInt("HomeZ", this.homePos.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("HomeX")) {
            this.homePos = new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ"));
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SandevistanMod.RIPPERDOC_AMBIENT;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 240;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp) {
            sp.playSoundToPlayer(SandevistanMod.RIPPERDOC_INTERACT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            ServerPlayNetworking.send(sp, new OpenShopPayload());
        }
        return ActionResult.success(this.getWorld().isClient);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isSourceCreativePlayer()) {
            return super.damage(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
