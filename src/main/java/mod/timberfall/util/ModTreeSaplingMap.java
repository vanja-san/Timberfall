package mod.timberfall.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import mod.timberfall.mixin.SaplingBlockMixin;
import mod.timberfall.mixin.TreeGrowerMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;

/**
 * Reverse-maps tree features back to the sapling block that grows them, so
 * modded trees get replanted without any per-mod configuration. It walks
 * every sapling in the block registry, asks its grower which tree features
 * it can produce, resolves those features (which in MC 26.3 are
 * self-contained {@link Feature}s in the {@code worldgen/feature} registry)
 * and reads the trunk block they place. The result is cached once per game
 * instance.
 */
public final class ModTreeSaplingMap {

	private ModTreeSaplingMap() {
	}

	private static volatile @Nullable Map<Block, Block> LOG_TO_SAPLING;

	/**
	 * Returns the sapling that grows a tree whose trunk uses {@code log}, or
	 * {@code null} when no known sapling maps to it. {@code level} is only
	 * used the first time while the map is being built.
	 */
	@Nullable
	public static Block getSaplingFor(Block log, ServerLevel level) {
		Map<Block, Block> map = LOG_TO_SAPLING;
		if (map == null) {
			synchronized (ModTreeSaplingMap.class) {
				map = LOG_TO_SAPLING;
				if (map == null) {
					map = build(level);
					LOG_TO_SAPLING = map;
				}
			}
		}
		return map.get(log);
	}

	private static Map<Block, Block> build(ServerLevel level) {
		Map<Block, Block> byLog = new HashMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (!(block instanceof SaplingBlock sapling)) {
				continue;
			}
			TreeGrower grower = ((SaplingBlockMixin) sapling).timberfall$getTreeGrower();
			if (grower == null) {
				continue;
			}
			TreeGrowerMixin growerAccess = (TreeGrowerMixin) (Object) grower;
			collect(level, byLog, block, growerAccess.timberfall$getTrees());
			collect(level, byLog, block, growerAccess.timberfall$getMegaTrees());
			collect(level, byLog, block, growerAccess.timberfall$getFlowerTrees());
		}
		return byLog;
	}

	private static void collect(ServerLevel level, Map<Block, Block> byLog, Block sapling,
			WeightedList<ResourceKey<Feature>> featureList) {
		if (featureList.isEmpty()) {
			return;
		}
		HolderLookup.RegistryLookup<Feature> features = level.registryAccess().lookupOrThrow(Registries.FEATURE);
		for (Weighted<ResourceKey<Feature>> weighted : featureList.unwrap()) {
			ResourceKey<Feature> key = weighted.value();
			@Nullable
			Holder<Feature> holder = features.get(key).orElse(null);
			if (holder == null) {
				continue;
			}
			BlockStateProvider trunk = trunkProvider(holder.value());
			if (trunk == null) {
				continue;
			}
			for (Block log : trunkLogs(trunk, level)) {
				byLog.putIfAbsent(log, sapling);
			}
		}
	}

	@Nullable
	private static BlockStateProvider trunkProvider(Feature feature) {
		if (feature instanceof TreeFeature tree) {
			return tree.trunkProvider().value();
		}
		if (feature instanceof FallenTreeFeature fallen) {
			return fallen.trunkProvider().value();
		}
		return null;
	}

	private static List<Block> trunkLogs(BlockStateProvider trunk, ServerLevel level) {
		List<Block> blocks = new ArrayList<>();
		if (trunk instanceof SimpleStateProvider simple) {
			blocks.add(simple.state().getBlock());
			return blocks;
		}
		BlockState sampled = trunk.getState(level, level.getRandom(), BlockPos.ZERO);
		blocks.add(sampled.getBlock());
		return blocks;
	}
}