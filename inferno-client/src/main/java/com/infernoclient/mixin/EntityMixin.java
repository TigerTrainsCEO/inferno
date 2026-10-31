package com.infernoclient.mixin;

import com.infernoclient.feature.PerspectiveController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
	private void inferno$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
		if (PerspectiveController.isActive() && (Object) this == MinecraftClient.getInstance().player) {
			PerspectiveController.updateCamera(cursorDeltaX, cursorDeltaY);
			ci.cancel();
		}
	}
}
