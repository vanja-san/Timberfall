package mod.timberfall.config;

/**
 * Runtime configuration for Timberfall.
 *
 * <p>All gameplay knobs live here. Each field carries a {@link Comment} that is
 * written above the matching entry in the JSON5 file, and every value is
 * clamped by {@link #sanitize()} on load, so a hand-edited file can never break
 * the mod.
 */
public final class Config {

	@Comment("Master switch for the entire mod.")
	public boolean enabled = true;

	// ----- Chop criteria -------------------------------------------------

	@Comment("Require an axe in the main hand to trigger a tree chop.")
	public boolean requireAxe = true;

	@Comment("Sneaking prevents the auto-chop and performs a normal single-block break.")
	public boolean sneakPreventsChopping = true;

	@Comment("Minimum connected logs for a group to be treated as a tree.")
	public int minLogsToChop = 2;

	// ----- Chop limits ---------------------------------------------------

	@Comment("Maximum logs that may be removed per tree.")
	public int maxLogsPerTree = 256;

	@Comment("Maximum distance above the broken block that is still part of the tree.")
	public int heightLimit = 64;

	@Comment("Maximum horizontal distance from the broken block for connected logs.")
	public int radiusLimit = 6;

	@Comment("Logs removed every server tick while a chop is in progress.")
	public int blocksPerTick = 6;

	@Comment("Allow the full-tree chop in Creative mode.")
	public boolean applyInCreative = true;

	@Comment("Consume durability from the used tool for each removed log.")
	public boolean damageTool = true;

	// ----- Break speed ---------------------------------------------------

	@Comment("How strongly each connected log slows the first block down (0..1).")
	public float breakSpeedFactor = 0.5f;

	@Comment("Upper bound of connected logs counted for the speed penalty.")
	public int speedLimitConnectedLogs = 64;

	// ----- Sapling replant -----------------------------------------------

	@Comment("After a tree is felled, plant a matching sapling where the trunk stood.")
	public boolean autoPlantSapling = true;

	// ----- Leaf decay ----------------------------------------------------

	@Comment("Enable the accelerated leaf decay after a tree is felled.")
	public boolean instantLeafDecay = true;

	@Comment("Assign leaves to the nearest trunk when canopies merge; disable for the vanilla rule.")
	public boolean leafDecayNearestTrunk = true;

	@Comment("Maximum leaves that drop in a single server tick after a chop.")
	public int leafDecayPerTick = 64;

	/**
	 * Clamps every value to a safe, sane range so a hand-edited config file
	 * can never break the mod.
	 */
	public void sanitize() {
		minLogsToChop = Math.max(1, minLogsToChop);
		maxLogsPerTree = Math.max(1, maxLogsPerTree);
		heightLimit = Math.max(1, heightLimit);
		radiusLimit = Math.max(1, radiusLimit);
		blocksPerTick = Math.max(1, blocksPerTick);
		speedLimitConnectedLogs = Math.max(1, speedLimitConnectedLogs);
		leafDecayPerTick = Math.max(1, leafDecayPerTick);
		breakSpeedFactor = Math.max(0.0f, Math.min(1.0f, breakSpeedFactor));
	}
}
