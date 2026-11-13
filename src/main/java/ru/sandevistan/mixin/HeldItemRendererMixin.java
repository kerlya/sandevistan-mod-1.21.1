package ru.sandevistan.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sandevistan.SandevistanMod;
import ru.sandevistan.client.SandevistanClient;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void sandevistan$adsCenterWeapon(
        AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
        float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
        VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
    ) {
        if (hand == Hand.MAIN_HAND && item.isOf(SandevistanMod.SARATOGA_M221)) {
            float ads = SandevistanClient.adsProgress;
            if (ads > 0.001f) {
                boolean isRight = player.getMainArm() == Arm.RIGHT;
                float dir = isRight ? -1.0f : 1.0f;
                matrices.translate(dir * 0.52f * ads, 0.07f * ads, -0.10f * ads);
            }
        }
    }
}
