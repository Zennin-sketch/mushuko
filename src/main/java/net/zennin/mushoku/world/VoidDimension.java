package net.zennin.mushoku.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.MushokuMod;

/** Dimension del vacio (la de Hitogami): mundo plano blanco, sin nada. El piso esta en y = 0. */
public final class VoidDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, MushokuMod.id("void"));
    /** Donde aparece el jugador: 6 bloques delante de Hitogami, mirandolo. */
    public static final double ARRIVE_X = 0.5, ARRIVE_Y = 1, ARRIVE_Z = 6.5;
    public static final float ARRIVE_YAW = 180f;

    private VoidDimension() {}

    public static boolean is(Level level) { return level.dimension() == LEVEL; }

    public static ServerLevel level(MinecraftServer server) { return server.getLevel(LEVEL); }
}
