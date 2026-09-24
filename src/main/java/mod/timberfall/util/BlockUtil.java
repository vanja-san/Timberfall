package mod.timberfall.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block classification helpers. Relying on vanilla tags keeps the mod
 * compatible with every future wood set (including modded logs) without
 * hardcoding block IDs.
 */
public final class BlockUtil {

	/** All 26 neighbours of a block (full 3x3x3 shell), for graph scans. */
	public static final BlockPos[] NEIGHBORS_3X3 = new BlockPos[26];

	/** Six axis-aligned neighbours, for distance/canopy scans. */
	public static final BlockPos[] NEIGHBORS_AXIS = new BlockPos[] {
			new BlockPos(0, 0, -1),
			new BlockPos(0, 0, 1),
			new BlockPos(0, -1, 0),
			new BlockPos(0, 1, 0),
			new BlockPos(-1, 0, 0),
			new BlockPos(1, 0, 0)
	};

	static {
		int i = 0;
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (x == 0 && y == 0 && z == 0) {
						continue;
					}
					NEIGHBORS_3X3[i++] = new BlockPos(x, y, z);
				}
			}
		}
	}

	private BlockUtil() {
	}

	public static boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS);
	}

	public static boolean isLog(Block block) {
		return isLog(block.defaultBlockState());
	}

	/** Checks both the {@code LeavesBlock} class and the vanilla leaves tag. */
	public static boolean isLeaf(BlockState state) {
		return state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock;
	}

	public static boolean isLeaf(Block block) {
		return isLeaf(block.defaultBlockState());
	}

	public static boolean isLogOrLeaf(BlockState state) {
		return isLog(state) || isLeaf(state);
	}

	public static boolean isLogOrLeaf(Block block) {
		return isLogOrLeaf(block.defaultBlockState());
	}
}