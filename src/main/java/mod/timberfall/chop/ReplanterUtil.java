package mod.timberfall.chop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import mod.timberfall.util.SaplingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replants saplings at the base of a felled tree. A 2x2 or wider trunk gets
 * one sapling per trunk column, so a giant jungle or dark oak can regrow
 * from its original footprint. The sapling type is derived from the wood
 * family found at the chopped block (falling back to any log of the plan),
 * so a mix of logs (e.g. a cherry tree over a giant jungle log) still yields
 * the sapling that matches the trunk that was actually chopped.
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
		Block sapling = SaplingUtil.getSaplingForLog(level.getBlockState(origin), level);
		if (sapling != null) {
			return sapling;
		}

		for (int dy = 1; dy <= 4; dy++) {
			BlockState below = level.getBlockState(origin.below(dy));
			if (below.isAir()) {
				continue;
			}
			sapling = SaplingUtil.getSaplingForLog(below, level);
			if (sapling != null) {
				return sapling;
			}
		}
		return null;
	}

	/**
	 * Like {@link #saplingFor(ServerLevel, BlockPos)}, but when the origin
	 * yields no sapling (its block and anything below are already gone) any
	 * log the plan captured for the tree is tried instead.
	 */
	@Nullable
	public static Block saplingFor(ServerLevel level, BlockPos origin, List<BlockPos> logs) {
		Block sapling = saplingFor(level, origin);
		if (sapling != null) {
			return sapling;
		}
		for (BlockPos pos : logs) {
			sapling = SaplingUtil.getSaplingForLog(level.getBlockState(pos), level);
			if (sapling != null) {
				return sapling;
			}
		}
		return null;
	}

	/**
	 * Returns one replant spot per trunk column: the lowest log position of
	 * every column that belongs to the tree's footprint (the origin column
	 * plus any within one block of it). A 2x2 jungle or dark oak therefore
	 * yields four spots, so the giant tree can regrow from a full 2x2 patch
	 * of saplings. The origin column is forced in even when the plan did not
	 * capture it (some break events already remove the block by then).
	 */
	public static List<BlockPos> replantSpots(ServerLevel level, List<BlockPos> logs, BlockPos origin) {
		Map<BlockPos, Integer> lowestByColumn = new HashMap<>();
		int ox = origin.getX();
		int oz = origin.getZ();
		for (BlockPos pos : logs) {
			int dx = pos.getX() - ox;
			int dz = pos.getZ() - oz;
			if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
				continue;
			}
			BlockPos column = column(pos);
			lowestByColumn.merge(column, pos.getY(), Math::min);
		}
		lowestByColumn.merge(column(origin), origin.getY(), Math::min);

		List<BlockPos> spots = new ArrayList<>();
		lowestByColumn.forEach((column, y) -> spots.add(new BlockPos(column.getX(), y, column.getZ())));
		return spots;
	}

	/** Plants {@code sapling} at every replant spot that is clear. */
	public static void plant(ServerLevel level, List<BlockPos> spots, Block sapling) {
		if (sapling == null || sapling == Blocks.AIR) {
			return;
		}
		for (BlockPos spot : spots) {
			BlockState saplingState = sapling.defaultBlockState();
			if (!level.getBlockState(spot).isAir() || !saplingState.canSurvive(level, spot)) {
				continue;
			}
			level.setBlock(spot, saplingState, Block.UPDATE_ALL);
		}
	}

	private static BlockPos column(BlockPos pos) {
		return new BlockPos(pos.getX(), 0, pos.getZ());
	}
}