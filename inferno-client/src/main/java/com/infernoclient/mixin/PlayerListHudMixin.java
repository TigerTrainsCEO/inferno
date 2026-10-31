package com.infernoclient.mixin;

import com.infernoclient.InfernoClient;
import com.infernoclient.feature.FriendManager;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
	@Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
	private void inferno$friend(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
		if (!InfernoClient.config().social.highlightFriends) {
			return;
		}
		if (FriendManager.isFriend(entry.getProfile().name(), entry.getProfile().id())) {
			cir.setReturnValue(Text.literal("★ ").formatted(Formatting.GREEN).append(cir.getReturnValue()));
		}
	}
}
