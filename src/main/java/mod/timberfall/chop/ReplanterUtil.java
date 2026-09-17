package mod.timberfall.chop;

import org.jetbrains.annotations.Nullable;

import mod.timberfall.util.SaplingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replants a sapling at the base of a felled tree. The sapling type is
 * derived from the wood family found right below the surface, so a mix of
 * logs (e.g. a cherry tree over a giant jungle log) still yields the sapling
 * that matches the trunk that was actually chopped.
 */
public final class ReplanterUtil {

	private ReplanterUtil() {
	}

	/**
	 * Determines the sapling that should regrow a tree whose trunk starts at
	 * {@code origin}. The {@code origin} block itself may already be gone at
	 * call time (it is broken as part of the chop), so the wood family is
	 * looked up from the logs immediately below it, falling back to the
	 * origin's own block state when nothing is left underneath.
	 */
	@Nullable
	public static Block saplingFor(ServerLevel level, BlockPos origin) {
		Block sapling = SaplingUtil.getSaplingForLog(level.getBlockState(origin));
		if (sapling != null) {
			return sapling;
		}

		for (int dy = 1; dy <= 4; dy++) {
			BlockState below = level.getBlockState(origin.below(dy));
			if (below.isAir()) {
				continue;
			}
			sapling = SaplingUtil.getSaplingForLog(below);
			if (sapling != null) {
				return sapling;
			}
		}
		return null;
	}

	/** Plants {@code sapling} at the base of the trunk, if the spot is clear. */
	public static void plant(ServerLevel level, BlockPos origin, Block sapling) {
		if (sapling == null || sapling == Blocks.AIR) {
			return;
		}

		BlockPos ground = findGroundPos(level, origin);
		if (ground == null) {
			return;
		}

		BlockState saplingState = sapling.defaultBlockState();
		BlockState groundState = level.getBlockState(ground);
		if (!groundState.isAir() || !saplingState.canSurvive(level, ground)) {
			return;
		}
		level.setBlock(ground, saplingState, Block.UPDATE_ALL);
	}

	/**
	 * Returns the first air position, scanning downwards from {@code origin},
	 * that has a solid (non-air) block directly below it — i.e. the ground
	 * where the trunk used to stand. Returns {@code null} if no ground is
	 * found within a few blocks.
	 */
	@Nullable
	private static BlockPos findGroundPos(ServerLevel level, BlockPos origin) {
		for (int dy = 0; dy <= 4; dy++) {
			BlockPos pos = origin.below(dy);
			if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
				return pos;
			}
		}
		return null;
	}
}