package mod.timberfall.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Maps a felled log back to the sapling that regrows the same tree. The
 * primary lookup goes through the vanilla per-family item tags (e.g.
 * {@code minecraft:oak_logs}) so stripped logs, wood blocks and modded woods
 * that join those tags all resolve to the right sapling. Logs that no family
 * tag covers fall back to the generic reverse-map of every registered tree
 * feature (see {@link ModTreeSaplingMap}), which is what makes replanting
 * work for modded trees without any mod-specific configuration.
 */
public final class SaplingUtil {

	private SaplingUtil() {
	}

	/**
	 * Returns the sapling {@link Block} for a log block known through the
	 * vanilla family tags, or {@code null}. Use
	 * {@link #getSaplingForLog(BlockState, ServerLevel)} to also cover
	 * modded trees.
	 */
	@Nullable
	public static Block getSaplingForLog(BlockState logState) {
		Item item = logState.getBlock().asItem();
		if (item == null) {
			return null;
		}

		Holder<Item> holder = item.getDefaultInstance().typeHolder();
		if (holder.is(ItemTags.OAK_LOGS)) {
			return Blocks.OAK_SAPLING;
		}
		if (holder.is(ItemTags.BIRCH_LOGS)) {
			return Blocks.BIRCH_SAPLING;
		}
		if (holder.is(ItemTags.SPRUCE_LOGS)) {
			return Blocks.SPRUCE_SAPLING;
		}
		if (holder.is(ItemTags.JUNGLE_LOGS)) {
			return Blocks.JUNGLE_SAPLING;
		}
		if (holder.is(ItemTags.ACACIA_LOGS)) {
			return Blocks.ACACIA_SAPLING;
		}
		if (holder.is(ItemTags.DARK_OAK_LOGS)) {
			return Blocks.DARK_OAK_SAPLING;
		}
		if (holder.is(ItemTags.PALE_OAK_LOGS)) {
			return Blocks.PALE_OAK_SAPLING;
		}
		if (holder.is(ItemTags.CHERRY_LOGS)) {
			return Blocks.CHERRY_SAPLING;
		}
		if (holder.is(ItemTags.MANGROVE_LOGS)) {
			return Blocks.MANGROVE_PROPAGULE;
		}
		if (holder.is(ItemTags.POPLAR_LOGS)) {
			return Blocks.POPLAR_SAPLING;
		}
		return null;
	}

	/**
	 * Returns the sapling {@link Block} for a log block, or {@code null}.
	 * Resolves the vanilla family tags first and, for logs no tag covers,
	 * falls back to the generic reverse-map of every registered tree feature
	 * so modded trees are replanted without any mod-specific configuration.
	 */
	@Nullable
	public static Block getSaplingForLog(BlockState logState, @Nullable ServerLevel level) {
		Block sapling = getSaplingForLog(logState);
		if (sapling != null) {
			return sapling;
		}
		if (level != null) {
			return ModTreeSaplingMap.getSaplingFor(logState.getBlock(), level);
		}
		return null;
	}
}