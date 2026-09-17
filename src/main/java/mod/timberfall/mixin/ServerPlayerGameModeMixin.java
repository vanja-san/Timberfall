package mod.timberfall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mod.timberfall.chop.ChopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies the connected-logs break-speed penalty on the logical server so a
 * large tree needs to be mined at a comparable pace.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

	@Shadow
	protected ServerLevel level;

	@WrapOperation(method = {"incrementDestroyProgress", "handleBlockBreakAction"}, at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
	))
	private float timberfall$modifyBlockBreakSpeed(BlockState state, Player player, BlockGetter blockGetter,
			BlockPos pos, Operation<Float> original) {
		float delta = original.call(state, player, blockGetter, pos);
		if (ChopManager.shouldApplyBreakModifier(player, state)) {
			delta = ChopManager.modifyBlockBreakSpeed(delta, level, pos, player);
		}
		return delta;
	}
}