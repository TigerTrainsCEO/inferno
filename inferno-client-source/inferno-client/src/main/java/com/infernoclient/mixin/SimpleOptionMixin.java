package com.infernoclient.mixin;

import com.infernoclient.InfernoClient;
import com.infernoclient.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fullbright: the lightmap reads the gamma option every frame, so answer it with a high value. */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
	@SuppressWarnings("unchecked")
	@Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
	private void inferno$fullbright(CallbackInfoReturnable<T> cir) {
		ClientConfig config = InfernoClient.config();
		if (!config.visual.fullbright) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options == null) {
			return;
		}
		if ((Object) this == client.options.getGamma()) {
			cir.setReturnValue((T) Double.valueOf(config.visual.fullbrightGamma));
		}
	}
}
