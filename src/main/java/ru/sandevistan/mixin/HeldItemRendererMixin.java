package ru.sandevistan.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sandevistan.SandevistanMod;
import ru.sandevistan.client.SandevistanClient;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float sandevistan$noSwingProgress(float swingProgress, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swing, ItemStack item) {
        if (hand == Hand.MAIN_HAND && item.isOf(SandevistanMod.SARATOGA_M221)) {
            return 0.0f;
        }
        return swingProgress;
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float sandevistan$noEquipDrop(float equipProgress, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swing, ItemStack item) {
        if (hand == Hand.MAIN_HAND && item.isOf(SandevistanMod.SARATOGA_M221)) {
            return 0.0f;
        }
        return equipProgress;
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void sandevistan$adsAndKickback(
        AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
        float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
        VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
    ) {
        if (hand == Hand.MAIN_HAND && item.isOf(SandevistanMod.SARATOGA_M221)) {
            float ads = SandevistanClient.adsProgress;
            if (ads > 0.001f) {
                boolean isRight = player.getMainArm() == Arm.RIGHT;
                float dir = isRight ? -1.0f : 1.0f;
                matrices.translate(dir * 0.44f * ads, 0.06f * ads, -0.08f * ads);
            }

            long sinceShot = System.currentTimeMillis() - SandevistanClient.lastClientShotMs;
            if (sinceShot >= 0L && sinceShot < 110L) {
                float kick = 1.0f - (sinceShot / 110.0f);
                matrices.translate(0.0f, 0.012f * kick, 0.045f * kick);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(2.2f * kick));
            }
        }
    }
}
