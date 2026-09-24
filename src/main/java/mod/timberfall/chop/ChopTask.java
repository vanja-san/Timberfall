package mod.timberfall.chop;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

/**
 * A single in-progress tree chop: the queue of logs still to break, the
 * player who started it and a record of everything already removed (needed
 * later for leaf decay).
 *
 * <p>In chain mode the scan order (breadth-first from the chopped block) is
 * kept, so the tree visibly cracks and falls one block at a time away from
 * the point of impact. In instant mode logs are processed top-down so leaves
 * never float in the air for a moment longer than necessary.
 */
public final class ChopTask {

	private final ServerLevel level;
	private final UUID playerId;
	private final Block sapling;
	private final List<BlockPos> replantSpots;
	private final Deque<BlockPos> remainingLogs;
	private final List<BlockPos> removedLogs = new ArrayList<>();
	private final int totalLogs;

	/**
	 * {@code chainOrder} selects the break order: {@code true} keeps the
	 * breath-first scan order (chain reaction), {@code false} breaks the
	 * trunk from top to bottom (instant mode).
	 */
	public ChopTask(ServerPlayer player, ServerLevel level, BlockPos origin, List<BlockPos> logs, boolean chainOrder) {
		this.level = level;
		this.playerId = player.getUUID();
		this.sapling = ReplanterUtil.saplingFor(level, origin, logs);
		this.replantSpots = ReplanterUtil.replantSpots(level, logs, origin);
		this.totalLogs = logs.size();

		List<BlockPos> ordered = new ArrayList<>(logs);
		if (!chainOrder) {
			ordered.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
		}
		this.remainingLogs = new ArrayDeque<>(ordered);
	}

	public ServerLevel level() {
		return level;
	}

	public UUID playerId() {
		return playerId;
	}

	/** The sapling to replant after this chop, or {@code null} if unknown. */
	public Block sapling() {
		return sapling;
	}

	/** One replant spot per trunk column of the planned tree. */
	public List<BlockPos> replantSpots() {
		return replantSpots;
	}

	public int totalLogs() {
		return totalLogs;
	}

	public int removedCount() {
		return removedLogs.size();
	}

	/** Returns the next log to break, or {@code null} if the tree is done. */
	public BlockPos nextLog() {
		return remainingLogs.poll();
	}

	public void recordRemoved(BlockPos pos) {
		removedLogs.add(pos);
	}

	public boolean isComplete() {
		return remainingLogs.isEmpty();
	}

	public List<BlockPos> removedLogs() {
		return removedLogs;
	}
}