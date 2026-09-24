package mod.timberfall.chop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mod.timberfall.config.ConfigManager;
import mod.timberfall.config.Config;
import mod.timberfall.leaf.LeafDecayEngine;
import mod.timberfall.util.BlockUtil;
import mod.timberfall.util.ToolUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Orchestrates the whole tree-felling feature.
 *
 * <p>Instead of instantly replacing the world with thousands of air blocks
 * inside one player event (as the original mod did, freezing the server for
 * big trees), each chop is queued and executed in small batches on the server
 * tick. That keeps the tick time flat and makes the mod safe in multiplayer.
 */
public final class ChopManager {

	/** Active chop per player. One at a time keeps the gameplay predictable. */
	private static final Map<UUID, ChopTask> ACTIVE_TASKS = new HashMap<>();

	/** Progress per tick for {@code constantChopSpeed}, so every log takes 4 ticks. */
	private static final float FIXED_LOG_BREAK_PROGRESS = 0.25f;

	private ChopManager() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register(ChopManager::onPlayerBlockBreak);
		ServerTickEvents.END_SERVER_TICK.register(ChopManager::onServerTick);
	}

	private static boolean onPlayerBlockBreak(Level world, Player player, BlockPos pos,
			BlockState state, @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		if (world.isClientSide()) {
			return true;
		}
		if (!BlockUtil.isLog(state)) {
			return true;
		}
		if (!canChopTree(world, player, state)) {
			return true;
		}

		// Cheap checks first: the full-tree scan below is the only expensive
		// step, so it must not run when the break will be a regular one anyway.
		Config cfg = ConfigManager.get();
		if (player.isCreative() && !cfg.applyInCreative) {
			return true;
		}

		ServerPlayer serverPlayer = (ServerPlayer) player;
		if (ACTIVE_TASKS.containsKey(serverPlayer.getUUID())) {
			// Already chopping a tree; swallow this break to keep things predictable.
			return false;
		}

		ServerLevel serverLevel = (ServerLevel) world;
		ChopPlanner.ChopPlan plan = ChopPlanner.plan(serverLevel, pos);
		if (!plan.isTree()) {
			return true;
		}

		ACTIVE_TASKS.put(serverPlayer.getUUID(),
				new ChopTask(serverPlayer, serverLevel, pos, plan.logs(), cfg.chainBreaking));
		return false;
	}

	/**
	 * Advances every active chop by a few blocks per tick. Finished tasks are
	 * finalised by triggering accelerated leaf decay.
	 */
	private static void onServerTick(MinecraftServer server) {
		if (ACTIVE_TASKS.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, ChopTask>> it = ACTIVE_TASKS.entrySet().iterator();
		while (it.hasNext()) {
			ChopTask task = it.next().getValue();
			if (advanceTask(server, task)) {
				it.remove();
			}
		}
	}

	/** Returns {@code true} when the task is done and should be removed. */
	private static boolean advanceTask(MinecraftServer server, ChopTask task) {
		ServerLevel level = task.level();
		ServerPlayer player = server.getPlayerList().getPlayer(task.playerId());

		// If the owner logged off, died or went into spectator, the chop must
		// still be finished server-side so the tree does not stay half-felled
		// with a floating canopy. Logs are then dropped plainly, without tool
		// damage or player attribution.
		boolean absent = player == null || player.isRemoved() || !player.isAlive() || player.isSpectator();

		Config cfg = ConfigManager.get();
		// Chain mode breaks a single log per tick (starting at the chopped
		// block and rippling outward with per-block particles and sound), so
		// the tree visibly falls along the trunk. Instant mode clears the
		// configured batch per tick instead.
		int perTick = cfg.chainBreaking ? 1 : cfg.blocksPerTick;
		int brokenThisTick = 0;
		while (brokenThisTick < perTick) {
			BlockPos pos = task.nextLog();
			if (pos == null) {
				break;
			}
			if (breakLog(level, absent ? null : player, pos, cfg)) {
				task.recordRemoved(pos);
				brokenThisTick++;
			}
		}

		if (!task.isComplete()) {
			return false;
		}

		if (cfg.autoPlantSapling) {
			ReplanterUtil.plant(level, task.replantSpots(), task.sapling());
		}

		LeafDecayEngine.onLogsRemoved(level, task.removedLogs());
		LogCountCache.invalidate();
		return true;
	}

	/**
	 * Removes a single log block, producing its drops (unless Creative and
	 * unless the owner is gone) and damaging the tool. Players in survival are
	 * stopped only by {@code damageTool} being enabled and the tool actually
	 * breaking. A {@code null} player means the owner abandoned the chop; the
	 * log is then dropped plainly so the tree still falls completely.
	 */
	private static boolean breakLog(ServerLevel level, @Nullable ServerPlayer player, BlockPos pos, Config cfg) {
		BlockState state = level.getBlockState(pos);
		if (!BlockUtil.isLog(state) || state.isAir()) {
			return false;
		}

		if (player == null) {
			Block.dropResources(state, level, pos);
		} else if (!player.isCreative()) {
			ItemStack item = player.getMainHandItem();
			state.getBlock().playerDestroy(level, player, pos, state, level.getBlockEntity(pos), item);
			if (cfg.damageTool && !item.isEmpty()) {
				EquipmentSlot slot = player.getEquipmentSlotForItem(item);
				item.hurtAndBreak(1, player, slot);
			}
		}

		if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
			return false;
		}

		level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
		return true;
	}

	/**
	 * Whether breaking {@code state} should trigger a full-tree chop. Used by
	 * the server break event; the client mirrors it via
	 * {@link #shouldSuppressClientBreak} so both sides stay in sync.
	 */
	public static boolean canChopTree(Level world, Player player, BlockState state) {
		if (world.isClientSide() || player.isSpectator()) {
			return false;
		}
		return shouldApplyBreakModifier(player, state);
	}

	/**
	 * Predicate shared by the server chop trigger and the break-speed mixins
	 * on both logical sides. Excludes the single-player spectator guard so it
	 * can safely run on the client.
	 */
	public static boolean shouldApplyBreakModifier(Player player, BlockState state) {
		Config cfg = ConfigManager.get();
		if (!cfg.enabled) {
			return false;
		}
		if (!BlockUtil.isLog(state)) {
			return false;
		}
		if (cfg.requireAxe && !ToolUtil.isAxe(player.getMainHandItem())) {
			return false;
		}
		return !cfg.sneakPreventsChopping || !player.isCrouching();
	}

	/**
	 * Whether the client should suppress its local single-block break
	 * prediction for this log. Mirrors the server's chop trigger as closely
	 * as the client can know it (the {@code applyInCreative} gate included;
	 * the tree-size scan stays a server decision), so a regular break that the
	 * server will not turn into a chop keeps its instant local feedback.
	 */
	public static boolean shouldSuppressClientBreak(Player player, BlockState state) {
		if (!shouldApplyBreakModifier(player, state)) {
			return false;
		}
		return !player.isCreative() || ConfigManager.get().applyInCreative;
	}

	/**
	 * Modifies the per-block destroy time so a large connected log group
	 * takes proportionally longer to fell. Mirrored on client and server.
	 *
	 * <p>With {@code constantChopSpeed} enabled the connected-group scan and
	 * the vanilla axe material no longer matter: every log breaks in the same
	 * fixed amount of time.
	 */
	public static float modifyBlockBreakSpeed(float delta, Level world, BlockPos pos, Player player) {
		Config cfg = ConfigManager.get();

		if (cfg.constantChopSpeed) {
			// Fixed progress per tick (≈ 4 ticks per log) regardless of tree
			// size or axe grade. Also skips the connected-log BFS entirely.
			return FIXED_LOG_BREAK_PROGRESS;
		}

		int connected = LogCountCache.count(world, pos, cfg.speedLimitConnectedLogs) - 1;
		if (connected < 0) {
			connected = 0;
		}

		if (cfg.damageTool) {
			// Logs that could not be broken anyway should not slow the break down.
			ItemStack item = player.getMainHandItem();
			int durability = item.getMaxDamage() - item.getDamageValue() - 1;
			if (durability < connected) {
				connected = durability;
			}
		}

		return delta / (1.0f + connected * cfg.breakSpeedFactor);
	}
}