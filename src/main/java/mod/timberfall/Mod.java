package mod.timberfall;

import mod.timberfall.chop.ChopManager;
import mod.timberfall.config.ConfigManager;
import mod.timberfall.leaf.LeafDecayEngine;
import net.fabricmc.api.ModInitializer;

/**
 * Timberfall entry point. Kept intentionally slim: it only wires up the
 * configuration, the event-driven chop manager and the leaf decay engine.
 */
public final class Mod implements ModInitializer {

	public static final String MOD_ID = "timberfall";

	@Override
	public void onInitialize() {
		ConfigManager.initialize();
		ChopManager.register();
		LeafDecayEngine.register();
	}
}