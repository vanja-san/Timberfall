package mod.timberfall.util;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/**
 * Tool classification helpers. Detects axes through the {@code minecraft:axes}
 * item tag, so axes added by other mods (and vanilla axes, which are no longer
 * a dedicated class in this version) are recognised out of the box.
 */
public final class ToolUtil {

	private ToolUtil() {
	}

	public static boolean isAxe(ItemStack stack) {
		return !stack.isEmpty() && stack.typeHolder().is(ItemTags.AXES);
	}
}