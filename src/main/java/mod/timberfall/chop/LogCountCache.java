package mod.timberfall.chop;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Small time-bounded cache for the connected-log scan used by the break-speed
 * penalty. The BFS over all logs is expensive enough that it must not run for
 * every single progress tick.
 *
 * <p>The cache is shared between the client and server threads (both call
 * into it from the break-speed mixins in single-player), so access is
 * synchronized. Entries are bounded with an access-order LRU eviction instead
 * of a blunt full clear, keeping the working set stable under heavy use.
 */
public final class LogCountCache {

	private static final long TTL_TICKS = 15;
	private static final int MAX_ENTRIES = 2048;

	private record CacheKey(ResourceKey<Level> dimension, BlockPos pos) {
	}

	/** Cached scan result plus the game time it was computed at. */
	private record CachedCount(long gameTime, int count) {
	}

	private static final Map<CacheKey, CachedCount> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<CacheKey, CachedCount> eldest) {
			return size() > MAX_ENTRIES;
		}
	};

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
			CachedCount cached = CACHE.get(key);
			if (cached != null && now - cached.gameTime < TTL_TICKS) {
				return cached.count;
			}
		}

		// The BFS is computed outside the lock so a slow scan never stalls the
		// other logical side.
		int count = ChopPlanner.countConnectedLogs(world, pos, limit);

		synchronized (CACHE) {
			CACHE.put(key, new CachedCount(now, count));
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