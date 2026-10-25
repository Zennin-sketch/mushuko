package net.zennin.mushoku.client;

import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.world.VoidDimension;

/** Dimension del vacio: cielo y niebla blancos, sin nubes, sin sol ni luna, sin lluvia. */
public final class VoidSkyEffects extends DimensionSpecialEffects {
    private static final Vec3 WHITE = new Vec3(1, 1, 1);

    private VoidSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, true, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) { return WHITE; }

    @Override
    public boolean isFoggyAt(int x, int z) { return false; }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) { return null; }

    public static void register() {
        DimensionRenderingRegistry.registerDimensionEffects(MushokuMod.id("void"), new VoidSkyEffects());
        DimensionRenderingRegistry.registerCloudRenderer(VoidDimension.LEVEL, ctx -> {});
        DimensionRenderingRegistry.registerWeatherRenderer(VoidDimension.LEVEL, ctx -> {});
    }
}
