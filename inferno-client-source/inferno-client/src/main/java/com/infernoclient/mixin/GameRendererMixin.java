package com.infernoclient.mixin;

import com.infernoclient.InfernoClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void inferno$zoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		if (InfernoClient.zoomActive()) {
			cir.setReturnValue(cir.getReturnValue() * InfernoClient.fovMultiplier(tickProgress));
		}
	}
}
