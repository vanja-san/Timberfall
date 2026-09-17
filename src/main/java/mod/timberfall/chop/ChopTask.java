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
 * <p>Logs are processed top-down so leaves never float in the air for a
 * moment longer than necessary.
 */
public final class ChopTask {

	private final ServerLevel level;
	private final UUID playerId;
	private final BlockPos origin;
	private final Block sapling;
	private final Deque<BlockPos> remainingLogs;
	private final List<BlockPos> removedLogs = new ArrayList<>();
	private final int totalLogs;

	public ChopTask(ServerPlayer player, ServerLevel level, BlockPos origin, List<BlockPos> logs) {
		this.level = level;
		this.playerId = player.getUUID();
		this.origin = origin;
		this.sapling = ReplanterUtil.saplingFor(level, origin);
		this.totalLogs = logs.size();

		List<BlockPos> sorted = new ArrayList<>(logs);
		sorted.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
		this.remainingLogs = new ArrayDeque<>(sorted);
	}

	public ServerLevel level() {
		return level;
	}

	public UUID playerId() {
		return playerId;
	}

	public BlockPos origin() {
		return origin;
	}

	/** The sapling to replant after this chop, or {@code null} if unknown. */
	public Block sapling() {
		return sapling;
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