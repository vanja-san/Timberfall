package mod.timberfall.config;

/**
 * Runtime configuration for Timberfall.
 *
 * <p>All gameplay knobs live here. Values are persisted as JSON and can be
 * changed without recompiling the mod.
 */
public final class Config {

	/** Master switch for the entire mod. */
	public boolean enabled = true;

	// ----- Chop criteria -------------------------------------------------

	/** Require an axe in the main hand to trigger a tree chop. */
	public boolean requireAxe = true;

	/** Sneaking prevents the auto-chop and performs a normal single-block break. */
	public boolean sneakPreventsChopping = true;

	/** Minimum connected logs for a group to be treated as a tree. */
	public int minLogsToChop = 2;

	// ----- Chop limits ---------------------------------------------------

	/** Maximum logs that may be removed per tree. */
	public int maxLogsPerTree = 256;

	/** Maximum distance above the broken block that is still considered part of the tree. */
	public int heightLimit = 64;

	/** Maximum horizontal distance from the broken block for connected logs. */
	public int radiusLimit = 6;

	/** Logs removed every server tick while a chop is in progress. */
	public int blocksPerTick = 6;

	/** Allow the full-tree chop in Creative mode. */
	public boolean applyInCreative = true;

	/** Consume durability from the used tool for each removed log. */
	public boolean damageTool = true;

	// ----- Break speed ---------------------------------------------------

	/** How strongly each connected log slows down the first block (0..1). */
	public float breakSpeedFactor = 0.5f;

	/** Upper bound of connected logs taken into account for the speed penalty. */
	public int speedLimitConnectedLogs = 64;

	// ----- Sapling replant -----------------------------------------------

	/** After a tree is felled, plant a matching sapling where the trunk stood. */
	public boolean autoPlantSapling = true;

	// ----- Leaf decay ----------------------------------------------------

	/** Enable the accelerated leaf decay after a tree is felled. */
	public boolean instantLeafDecay = true;

	/**
	 * Assigns every leaf to the nearest trunk when canopies merge. A felled
	 * tree drops exactly the leaves that were closer to its own logs, while
	 * leaves leaning toward a surviving neighbouring tree stay. Disable for
	 * the vanilla rule where any nearby log keeps every leaf alive.
	 */
	public boolean leafDecayNearestTrunk = true;

	/** Search depth used when updating leaf distances after a chop. */
	public int leafDecaySearchAttempts = 8;

	/** Maximum leaves that drop in a single server tick after a chop. */
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
		leafDecaySearchAttempts = Math.max(1, leafDecaySearchAttempts);
		leafDecayPerTick = Math.max(1, leafDecayPerTick);
		breakSpeedFactor = Math.max(0.0f, Math.min(1.0f, breakSpeedFactor));
	}
}