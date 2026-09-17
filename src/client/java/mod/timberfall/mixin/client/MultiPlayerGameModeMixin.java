package mod.timberfall.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mod.timberfall.chop.ChopManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mirrors the server-side break-speed penalty on the client and prevents the
 * client from removing a single block itself while a chop is about to happen.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Shadow
	private Minecraft minecraft;

	@WrapOperation(method = "continueDestroyBlock", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
	))
	private float timberfall$modifyBlockBreakSpeed(BlockState state, Player player, BlockGetter blockGetter,
			BlockPos pos, Operation<Float> original) {
		float delta = original.call(state, player, blockGetter, pos);
		if (ChopManager.shouldApplyBreakModifier(player, state)) {
			delta = ChopManager.modifyBlockBreakSpeed(delta, minecraft.level, pos, player);
		}
		return delta;
	}

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void timberfall$cancelBlockDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		Player player = minecraft.player;
		if (player == null) {
			return;
		}
		BlockState state = minecraft.level.getBlockState(pos);
		if (ChopManager.shouldApplyBreakModifier(player, state)) {
			cir.setReturnValue(false);
		}
	}
}