package mod.timberfall.util;

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