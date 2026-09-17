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

		ServerLevel serverLevel = (ServerLevel) world;
		ChopPlanner.ChopPlan plan = ChopPlanner.plan(serverLevel, pos);
		if (!plan.isTree()) {
			return true;
		}

		if (player.isCreative() && !ConfigManager.get().applyInCreative) {
			return true;
		}

		ServerPlayer serverPlayer = (ServerPlayer) player;
		if (ACTIVE_TASKS.containsKey(serverPlayer.getUUID())) {
			// Already chopping a tree; swallow this break to keep things predictable.
			return false;
		}

		ACTIVE_TASKS.put(serverPlayer.getUUID(), new ChopTask(serverPlayer, serverLevel, pos, plan.logs()));
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

		if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
			return true;
		}

		Config cfg = ConfigManager.get();
		int brokenThisTick = 0;
		while (brokenThisTick < cfg.blocksPerTick) {
			BlockPos pos = task.nextLog();
			if (pos == null) {
				break;
			}
			if (breakLog(level, player, pos)) {
				task.recordRemoved(pos);
				brokenThisTick++;
			}
		}

		if (!task.isComplete()) {
			return false;
		}

		if (ConfigManager.get().autoPlantSapling) {
			ReplanterUtil.plant(level, task.origin(), task.sapling());
		}

		LeafDecayEngine.onLogsRemoved(level, task.removedLogs());
		LogCountCache.invalidate();
		return true;
	}

	/**
	 * Removes a single log block, producing its drops (unless Creative) and
	 * damaging the tool. Players in survival are stopped only by {@code
	 * damageTool} being enabled and the tool actually breaking.
	 */
	private static boolean breakLog(ServerLevel level, ServerPlayer player, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!BlockUtil.isLog(state) || state.isAir()) {
			return false;
		}

		Config cfg = ConfigManager.get();
		boolean creative = player.isCreative();

		if (!creative) {
			ItemStack item = player.getMainHandItem();
			if (cfg.damageTool && !item.isEmpty()) {
				state.getBlock().playerDestroy(level, player, pos, state, level.getBlockEntity(pos), item);
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
	 * Whether breaking {@code state} should trigger a full-tree chop. The same
	 * predicate drives the client-side cancel mixin so both sides stay in sync.
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
	 * Modifies the per-block destroy time so a large connected log group
	 * takes proportionally longer to fell. Mirrored on client and server.
	 */
	public static float modifyBlockBreakSpeed(float delta, Level world, BlockPos pos, Player player) {
		Config cfg = ConfigManager.get();
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