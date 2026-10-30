package com.infernoclient.mixin;

import com.infernoclient.feature.PerspectiveController;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0, shift = At.Shift.BEFORE)
	)
	private void inferno$freelook(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
		if (PerspectiveController.isActive()) {
			this.setRotation(PerspectiveController.yaw(), PerspectiveController.pitch());
		}
	}
}
