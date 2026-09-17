package mod.timberfall.chop;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Small time-bounded cache for the connected-log scan used by the break-speed
 * penalty. The BFS over all logs is expensive enough that it must not run for
 * every single progress tick.
 */
public final class LogCountCache {

	private static final long TTL_TICKS = 15;
	private static final int MAX_ENTRIES = 2048;

	private record CacheKey(ResourceKey<Level> dimension, BlockPos pos) {
	}

	private record Entry(long gameTime, int count) {
	}

	private static final Map<CacheKey, Entry> CACHE = new HashMap<>();

	private LogCountCache() {
	}

	/**
	 * Returns the number of logs connected to {@code pos} (capped at
	 * {@code limit}), using a cached value within a short time window.
	 */
	public static int count(Level world, BlockPos pos, int limit) {
		CacheKey key = new CacheKey(world.dimension(), pos.immutable());
		long now = world.getGameTime();

		synchronized (CACHE) {
			Entry cached = CACHE.get(key);
			if (cached != null && now - cached.gameTime < TTL_TICKS) {
				return cached.count;
			}
		}

		int count = ChopPlanner.countConnectedLogs(world, pos, limit);

		synchronized (CACHE) {
			if (CACHE.size() > MAX_ENTRIES) {
				CACHE.clear();
			}
			CACHE.put(key, new Entry(now, count));
		}
		return count;
	}

	/** Drops all cached entries. Called after a chop invalidates the world. */
	public static void invalidate() {
		synchronized (CACHE) {
			CACHE.clear();
		}
	}
}