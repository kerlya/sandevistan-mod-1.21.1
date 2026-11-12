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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RipperdocEntity extends PathAwareEntity {
    public RipperdocEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setInvulnerable(true);
        this.setPersistent();
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
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(3, new LookAroundGoal(this));
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
