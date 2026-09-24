package mod.timberfall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Exposes the private feature lists of {@link TreeGrower} so the generic
 * modded-sapling map can enumerate every tree feature a sapling may grow.
 */
@Mixin(TreeGrower.class)
public interface TreeGrowerMixin {

	@Accessor("trees")
	WeightedList<ResourceKey<Feature>> timberfall$getTrees();

	@Accessor("megaTrees")
	WeightedList<ResourceKey<Feature>> timberfall$getMegaTrees();

	@Accessor("flowerTrees")
	WeightedList<ResourceKey<Feature>> timberfall$getFlowerTrees();
}