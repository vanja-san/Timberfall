package mod.timberfall.leaf;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mod.timberfall.config.ConfigManager;
import mod.timberfall.util.BlockUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles accelerated leaf decay after a chop. Instead of waiting for the
 * vanilla distance recalculation, leaf distances are recomputed immediately
 * from the freshly broken logs and their decay is scheduled right away.
 */
public final class LeafDecayEngine {

	public static final int LEAF_DECAY_DISTANCE = 7;
	private static final int MAX_UPDATED_LEAVES = 4096;

	/** Six axis-aligned neighbours, used for both scans. */
	private static final BlockPos[] NEIGHBOR_OFFSETS = new BlockPos[] {
			new BlockPos(0, 0, -1),
			new BlockPos(0, 0, 1),
			new BlockPos(0, -1, 0),
			new BlockPos(0, 1, 0),
			new BlockPos(-1, 0, 0),
			new BlockPos(1, 0, 0)
	};

	/** Leaves waiting to drop, drained on the server tick so the item spawns
	 *  from a large chop stay flat instead of bursting all at once. */
	private static final Deque<PendingDecay> PENDING_DECAYS = new ArrayDeque<>();
	private static final Set<BlockPos> PENDING_POSITIONS = new HashSet<>();

	private LeafDecayEngine() {
	}

	/** A leaf waiting to drop. The captured {@link Block} guards against the
	 *  block being replaced before the drain reaches it. */
	private record PendingDecay(ServerLevel level, BlockPos pos, Block block) {
	}

	/**
	 * Recomputes distances for every leaf in the leaf-connected region that
	 * touches any of the removed log positions and schedules them to decay
	 * when they are too far from a remaining log.
	 *
	 * <p>The region is collected once (a whole merged canopy behind two
	 * neighbouring trunks is a single connected blob), then the distance to
	 * the felled logs and to any surviving log is derived for the entire blob
	 * with two multi-source breadth-first searches. That is O(leaves) instead
	 * of the previous one search per leaf, which kept large merged canopies
	 * from spiking the server tick.
	 *
	 * <p>When {@code leafDecayNearestTrunk} is enabled each leaf that sits
	 * closer to a felled log than to any surviving log is additionally
	 * considered felled, so a chopped tree whose canopy has grown into a
	 * neighbour's canopy loses exactly the leaves that belonged to it while
	 * the neighbour keeps every leaf leaning towards its own trunk.
	 */
	public static void onLogsRemoved(ServerLevel world, List<BlockPos> removedLogs) {
		if (!ConfigManager.get().instantLeafDecay || removedLogs.isEmpty()) {
			return;
		}

		boolean nearestTrunk = ConfigManager.get().leafDecayNearestTrunk;
		Set<BlockPos> felledLogs = nearestTrunk ? new HashSet<>(removedLogs) : Set.of();

		Set<BlockPos> blob = collectLeafBlob(world, removedLogs);
		if (blob.isEmpty()) {
			return;
		}

		Map<BlockPos, Integer> distanceToFelled = nearestTrunk
				? distancesFromSources(world, blob, felledLogs)
				: Map.of();
		Set<BlockPos> remainingLogs = findAdjacentLogs(world, blob, felledLogs);
		Map<BlockPos, Integer> distanceToRemaining = distancesFromSources(world, blob, remainingLogs);

		for (BlockPos pos : blob) {
			BlockState state = world.getBlockState(pos);
			if (!BlockUtil.isLeaf(state) || state.getValue(LeavesBlock.PERSISTENT)) {
				continue;
			}

			int toRemaining = distanceToRemaining.getOrDefault(pos, LEAF_DECAY_DISTANCE);
			boolean decays = toRemaining >= LEAF_DECAY_DISTANCE;
			boolean felledOwned = false;
			if (nearestTrunk) {
				int toFelled = distanceToFelled.getOrDefault(pos, LEAF_DECAY_DISTANCE);
				felledOwned = toFelled <= toRemaining;
				decays = decays || felledOwned;
			}

			if (decays || felledOwned) {
				world.setBlock(pos, state.setValue(LeavesBlock.DISTANCE, LEAF_DECAY_DISTANCE), Block.UPDATE_NONE);
				enqueueDecay(world, pos, state);
			} else if (state.getValue(LeavesBlock.DISTANCE) != toRemaining) {
				world.setBlock(pos, state.setValue(LeavesBlock.DISTANCE, toRemaining), Block.UPDATE_NONE);
			}
		}
	}

	/**
	 * Collects every non-persistent leaf reachable from the removed logs,
	 * bounded by {@link #MAX_UPDATED_LEAVES} so a pathological canopy can
	 * never stall the server.
	 */
	private static Set<BlockPos> collectLeafBlob(Level world, List<BlockPos> removedLogs) {
		Set<BlockPos> blob = new HashSet<>();
		Deque<BlockPos> frontier = new ArrayDeque<>();

		for (BlockPos logPos : removedLogs) {
			for (BlockPos offset : NEIGHBOR_OFFSETS) {
				BlockPos neighbor = logPos.offset(offset);
				if (isDecayableLeaf(world, neighbor) && blob.add(neighbor)) {
					frontier.add(neighbor);
				}
			}
		}

		while (!frontier.isEmpty() && blob.size() < MAX_UPDATED_LEAVES) {
			BlockPos pos = frontier.poll();
			for (BlockPos offset : NEIGHBOR_OFFSETS) {
				BlockPos neighbor = pos.offset(offset);
				if (isDecayableLeaf(world, neighbor) && blob.add(neighbor)) {
					frontier.add(neighbor);
				}
			}
		}

		return blob;
	}

	/**
	 * Multi-source breadth-first search over {@code blob} leaves, treating
	 * every position in {@code sources} as distance 0. Returns the distance
	 * for each reached leaf (capped at {@link #LEAF_DECAY_DISTANCE}); leaf
	 * positions that are absent were not reached.
	 */
	private static Map<BlockPos, Integer> distancesFromSources(Level world, Set<BlockPos> blob,
			Set<BlockPos> sources) {
		Map<BlockPos, Integer> distances = new HashMap<>();
		if (sources.isEmpty()) {
			return distances;
		}

		Deque<BlockPos> frontier = new ArrayDeque<>();
		for (BlockPos source : sources) {
			if (distances.putIfAbsent(source, 0) == null) {
				frontier.add(source);
			}
		}

		while (!frontier.isEmpty()) {
			BlockPos pos = frontier.poll();
			int distance = distances.get(pos);
			if (distance >= LEAF_DECAY_DISTANCE) {
				continue;
			}
			for (BlockPos offset : NEIGHBOR_OFFSETS) {
				BlockPos neighbor = pos.offset(offset);
				if (!blob.contains(neighbor) || distances.containsKey(neighbor)) {
					continue;
				}
				distances.put(neighbor, distance + 1);
				frontier.add(neighbor);
			}
		}

		return distances;
	}

	/** Logs bordering the blob that are not part of the felled tree. */
	private static Set<BlockPos> findAdjacentLogs(Level world, Set<BlockPos> blob, Set<BlockPos> excluded) {
		Set<BlockPos> logs = new HashSet<>();
		for (BlockPos pos : blob) {
			for (BlockPos offset : NEIGHBOR_OFFSETS) {
				BlockPos neighbor = pos.offset(offset);
				if (!excluded.contains(neighbor) && BlockUtil.isLog(world.getBlockState(neighbor))) {
					logs.add(neighbor);
				}
			}
		}
		return logs;
	}

	private static boolean isDecayableLeaf(Level world, BlockPos pos) {
		if (!world.isLoaded(pos)) {
			return false;
		}
		BlockState state = world.getBlockState(pos);
		return BlockUtil.isLeaf(state) && !state.getValue(LeavesBlock.PERSISTENT);
	}

	/**
	 * Registers the per-tick drain that actually performs the leaf drops, so
	 * the item spawns from a large chop are spread over several server ticks
	 * instead of all bursting in one.
	 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(LeafDecayEngine::drainDecays);
	}

	/** Drops up to the configured number of queued leaves per server tick. */
	private static void drainDecays(MinecraftServer server) {
		int budget = Math.max(1, ConfigManager.get().leafDecayPerTick);
		while (budget > 0 && !PENDING_DECAYS.isEmpty()) {
			PendingDecay pending = PENDING_DECAYS.poll();
			PENDING_POSITIONS.remove(pending.pos());
			budget--;

			ServerLevel level = pending.level();
			if (!level.isLoaded(pending.pos())) {
				continue;
			}
			BlockState state = level.getBlockState(pending.pos());
			if (BlockUtil.isLeaf(state) && !state.getValue(LeavesBlock.PERSISTENT)
					&& state.getBlock() == pending.block()) {
				decay(level, pending.pos(), state);
			}
		}
	}

	private static void enqueueDecay(ServerLevel level, BlockPos pos, BlockState state) {
		if (PENDING_POSITIONS.size() >= MAX_UPDATED_LEAVES || !PENDING_POSITIONS.add(pos)) {
			return;
		}
		PENDING_DECAYS.addLast(new PendingDecay(level, pos.immutable(), state.getBlock()));
	}

	private static boolean isPending(BlockPos pos) {
		return PENDING_POSITIONS.contains(pos);
	}

	/**
	 * Accelerated decay hook for the vanilla leaf tick. Leaves that already
	 * sit at the decay distance are dropped immediately; everything else is
	 * left to the vanilla O(1) neighbour update, which keeps this hook free
	 * of any graph search.
	 *
	 * @return {@code true} when this method handled the tick (caller cancels
	 *         the vanilla logic), {@code false} to let vanilla proceed.
	 */
	public static boolean onTickLeaves(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (isPending(pos)) {
			// The drain owns this leaf; swallow the vanilla tick so it is not
			// dropped twice while it waits in the queue.
			return true;
		}
		if (state.getValue(LeavesBlock.DISTANCE) >= LEAF_DECAY_DISTANCE) {
			decay(world, pos, state);
			return true;
		}
		return false;
	}

	private static boolean decay(Level world, BlockPos pos, BlockState state) {
		if (world.removeBlock(pos, false)) {
			Block.dropResources(state, world, pos);
			return true;
		}
		return false;
	}
}