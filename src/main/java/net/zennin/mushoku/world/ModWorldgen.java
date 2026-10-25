package net.zennin.mushoku.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.zennin.mushoku.MushokuMod;

/** Feature propia y los 6 "biome modifiers" de mundo del original, pasados a la API de Fabric. */
public final class ModWorldgen {
    public static final Feature<StructureFeatureConfig> STRUCTURE_FEATURE =
            Registry.register(BuiltInRegistries.FEATURE, MushokuMod.id("structure_feature"), new StructureFeature());

    private ModWorldgen() {}

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, MushokuMod.id(name));
    }

    @SafeVarargs
    private static void add(String feature, GenerationStep.Decoration step, ResourceKey<Biome>... biomes) {
        BiomeModifications.addFeature(biomes.length == 0 ? BiomeSelectors.foundInOverworld() : BiomeSelectors.includeByKey(biomes),
                step, placed(feature));
    }

    public static void init() {
        ResourceKey<Biome>[] green = new ResourceKey[]{Biomes.PLAINS, Biomes.FOREST, Biomes.MEADOW, Biomes.CHERRY_GROVE,
                Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST, Biomes.SUNFLOWER_PLAINS};
        add("magic_ore", GenerationStep.Decoration.UNDERGROUND_ORES);
        add("sky_island", GenerationStep.Decoration.SURFACE_STRUCTURES);
        add("earth_tree", GenerationStep.Decoration.SURFACE_STRUCTURES, green);
        add("teleporthouse", GenerationStep.Decoration.SURFACE_STRUCTURES, green);
        add("fire_tower", GenerationStep.Decoration.SURFACE_STRUCTURES, Biomes.BADLANDS, Biomes.DESERT, Biomes.ERODED_BADLANDS,
                Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU);
        add("water_book_ice", GenerationStep.Decoration.SURFACE_STRUCTURES, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA,
                Biomes.SNOWY_SLOPES, Biomes.ICE_SPIKES);
    }
}
