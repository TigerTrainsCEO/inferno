package com.infernoclient.mixin;

import com.infernoclient.InfernoClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Shield: lower a held shield by translating DOWN (negative Y) around the first person render. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
	@Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
	private void inferno$lowShieldStart(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
		if (InfernoClient.config().visual.lowShield && item.isOf(Items.SHIELD)) {
			matrices.push();
			matrices.translate(0.0, -InfernoClient.config().visual.lowShieldAmount, 0.0);
		}
	}

	@Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
	private void inferno$lowShieldEnd(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
		if (InfernoClient.config().visual.lowShield && item.isOf(Items.SHIELD)) {
			matrices.pop();
		}
	}
}
