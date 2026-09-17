package mod.timberfall.util;

import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Maps a felled log back to the sapling that regrows the same tree. The
 * lookup goes through the vanilla per-family item tags (e.g.
 * {@code minecraft:oak_logs}) so stripped logs, wood blocks and modded woods
 * that join those tags all resolve to the right sapling.
 */
public final class SaplingUtil {

	private SaplingUtil() {
	}

	/** Returns the sapling {@link Block} for a log block, or {@code null}. */
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
}