package mod.timberfall.chop;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mod.timberfall.config.ConfigManager;
import mod.timberfall.config.Config;
import mod.timberfall.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Discovers every log connected to the block the player broke, bounded by the
 * configured height and radius limits so that a single chop can never wipe
 * out a whole forest or a build.
 */
public final class ChopPlanner {

	private ChopPlanner() {
	}

	/**
	 * Result of a scan. The logs list is already bounded and considered safe
	 * to remove.
	 */
	public record ChopPlan(List<BlockPos> logs) {

		/** Whether the connected group is big enough to count as a tree. */
		public boolean isTree() {
			return logs.size() >= ConfigManager.get().minLogsToChop;
		}

		public int size() {
			return logs.size();
		}
	}

	/**
	 * Breadth-first search from {@code origin}. Only log blocks within the
	 * configured relative height/radius bounds are followed.
	 */
	public static ChopPlan plan(Level world, BlockPos origin) {
		Config cfg = ConfigManager.get();

		int maxLogs = cfg.maxLogsPerTree;
		int heightLimit = cfg.heightLimit;
		int radiusLimit = cfg.radiusLimit;

		List<BlockPos> logs = new ArrayList<>();
		Deque<BlockPos> frontier = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();

		frontier.add(origin);
		visited.add(origin);

		while (!frontier.isEmpty() && logs.size() < maxLogs) {
			BlockPos pos = frontier.poll();
			if (BlockUtil.isLog(world.getBlockState(pos))) {
				logs.add(pos);
			}

			// Check the geometric bounds and log-ness before marking the
			// position visited, so the visited set stays small in dense areas.
			for (BlockPos offset : BlockUtil.NEIGHBORS_3X3) {
				BlockPos next = pos.offset(offset);
				int dy = next.getY() - origin.getY();
				if (Math.abs(dy) > heightLimit) {
					continue;
				}
				int dx = next.getX() - origin.getX();
				int dz = next.getZ() - origin.getZ();
				if (Math.abs(dx) > radiusLimit || Math.abs(dz) > radiusLimit) {
					continue;
				}
				if (!BlockUtil.isLog(world.getBlockState(next))) {
					continue;
				}
				if (visited.add(next)) {
					frontier.add(next);
				}
			}
		}

		return new ChopPlan(logs);
	}

	/** Counts connected logs but stops early, for use by the speed penalty. */
	public static int countConnectedLogs(Level world, BlockPos origin, int limit) {
		int count = 0;
		Deque<BlockPos> frontier = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();

		frontier.add(origin);
		visited.add(origin);

		while (!frontier.isEmpty() && count < limit) {
			BlockPos pos = frontier.poll();
			if (BlockUtil.isLog(world.getBlockState(pos))) {
				count++;
			}
			for (BlockPos offset : BlockUtil.NEIGHBORS_3X3) {
				BlockPos next = pos.offset(offset);
				if (visited.add(next) && BlockUtil.isLog(world.getBlockState(next))) {
					frontier.add(next);
				}
			}
		}
		return count;
	}
}