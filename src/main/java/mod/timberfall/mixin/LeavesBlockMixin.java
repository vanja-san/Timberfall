package mod.timberfall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mod.timberfall.config.ConfigManager;
import mod.timberfall.leaf.LeafDecayEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Takes over the vanilla leaf {@code tick} when accelerated decay is enabled
 * so leaves right on the decay edge fall within a few ticks instead of the
 * usual several seconds.
 */
@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void timberfall$onTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
			CallbackInfo ci) {
		if (!ConfigManager.get().instantLeafDecay) {
			return;
		}
		if (state.getValue(LeavesBlock.PERSISTENT)) {
			return;
		}
		if (LeafDecayEngine.onTickLeaves(state, level, pos, random)) {
			ci.cancel();
		}
	}
}