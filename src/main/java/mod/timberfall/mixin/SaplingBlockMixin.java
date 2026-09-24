package mod.timberfall.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

/**
 * Exposes the protected {@link SaplingBlock#treeGrower} field so the generic
 * modded-sapling map can read which tree features a sapling can grow.
 */
@Mixin(SaplingBlock.class)
public interface SaplingBlockMixin {

	@Accessor("treeGrower")
	@Nullable
	TreeGrower timberfall$getTreeGrower();
}