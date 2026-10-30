package com.infernoclient.mixin;

import com.infernoclient.InfernoClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Fire: push the burning overlay DOWN (negative Y) so it stops covering your view. */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
	@Inject(method = "renderFireOverlay", at = @At("HEAD"))
	private static void inferno$lowFireStart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
		if (InfernoClient.config().visual.lowFire) {
			matrices.push();
			matrices.translate(0.0, -InfernoClient.config().visual.lowFireAmount, 0.0);
		}
	}

	@Inject(method = "renderFireOverlay", at = @At("RETURN"))
	private static void inferno$lowFireEnd(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
		if (InfernoClient.config().visual.lowFire) {
			matrices.pop();
		}
	}
}
