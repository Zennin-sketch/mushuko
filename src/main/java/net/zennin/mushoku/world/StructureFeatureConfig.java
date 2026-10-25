package net.zennin.mushoku.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

/** Configuracion de "remushoku:structure_feature" (mismo formato que el original de MCreator). */
public record StructureFeatureConfig(ResourceLocation structure, boolean randomRotation, boolean randomMirror,
                                     List<Block> ignoredBlocks, Vec3i offset) implements FeatureConfiguration {
    private static final Codec<List<Block>> BLOCKS = Codec.withAlternative(
            BuiltInRegistries.BLOCK.byNameCodec().listOf(),
            BuiltInRegistries.BLOCK.byNameCodec().xmap(List::of, l -> l.get(0)));

    public static final Codec<StructureFeatureConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("structure").forGetter(StructureFeatureConfig::structure),
            Codec.BOOL.optionalFieldOf("random_rotation", false).forGetter(StructureFeatureConfig::randomRotation),
            Codec.BOOL.optionalFieldOf("random_mirror", false).forGetter(StructureFeatureConfig::randomMirror),
            BLOCKS.optionalFieldOf("ignored_blocks", List.of()).forGetter(StructureFeatureConfig::ignoredBlocks),
            Vec3i.CODEC.optionalFieldOf("offset", BlockPos.ZERO).forGetter(StructureFeatureConfig::offset)
    ).apply(i, StructureFeatureConfig::new));
}
