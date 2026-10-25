package net.zennin.mushoku.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/** Coloca una estructura guardada (torre de fuego, arbol del libro de tierra, isla del cielo...). */
public class StructureFeature extends Feature<StructureFeatureConfig> {
    public StructureFeature() { super(StructureFeatureConfig.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<StructureFeatureConfig> ctx) {
        StructureFeatureConfig cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        Optional<StructureTemplate> opt = level.getLevel().getServer().getStructureManager().get(cfg.structure());
        if (opt.isEmpty()) return false;
        StructureTemplate t = opt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(cfg.randomRotation() ? Rotation.getRandom(random) : Rotation.NONE)
                .setMirror(cfg.randomMirror() ? Mirror.values()[random.nextInt(Mirror.values().length)] : Mirror.NONE)
                .setRandom(random)
                .setIgnoreEntities(false);
        if (!cfg.ignoredBlocks().isEmpty()) settings.addProcessor(new BlockIgnoreProcessor(cfg.ignoredBlocks()));
        BlockPos pos = ctx.origin().offset(cfg.offset());
        return t.placeInWorld(level, pos, pos, settings, random, Block.UPDATE_CLIENTS);
    }
}
