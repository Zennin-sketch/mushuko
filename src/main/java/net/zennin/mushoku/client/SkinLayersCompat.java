package net.zennin.mushoku.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;

/**
 * Compatibilidad opcional con 3D Skin Layers: si el mod esta instalado, la capa exterior de la skin de los NPCs
 * (gorro, chaqueta, mangas, pantalon) se dibuja en 3D igual que en los jugadores. Si no esta, no hace nada.
 * Cualquier diferencia de version se atrapa y simplemente deja la capa en 2D.
 */
public final class SkinLayersCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("skinlayers3d");
    private static boolean broken = false;

    private SkinLayersCompat() {}

    public static void apply(PlayerModel<?> model, ResourceLocation texture, boolean slim) {
        if (!LOADED || broken) return;
        try {
            NativeImage img;
            try (var in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open()) {
                img = NativeImage.read(in);
            }
            if (img.getWidth() != 64 || img.getHeight() != 64) return;
            var helper = dev.tr7zw.skinlayers.api.SkinLayersAPI.getMeshHelper();
            inject(model.hat, helper.create3DMesh(img, 8, 8, 8, 32, 0, false, 0.6f), dev.tr7zw.skinlayers.api.OffsetProvider.HEAD);
            inject(model.jacket, helper.create3DMesh(img, 8, 12, 4, 16, 32, true, 0), dev.tr7zw.skinlayers.api.OffsetProvider.BODY);
            inject(model.leftPants, helper.create3DMesh(img, 4, 12, 4, 0, 48, true, 0f), dev.tr7zw.skinlayers.api.OffsetProvider.LEFT_LEG);
            inject(model.rightPants, helper.create3DMesh(img, 4, 12, 4, 0, 32, true, 0f), dev.tr7zw.skinlayers.api.OffsetProvider.RIGHT_LEG);
            int arm = slim ? 3 : 4;
            inject(model.leftSleeve, helper.create3DMesh(img, arm, 12, 4, 48, 48, true, -2f),
                    slim ? dev.tr7zw.skinlayers.api.OffsetProvider.LEFT_ARM_SLIM : dev.tr7zw.skinlayers.api.OffsetProvider.LEFT_ARM);
            inject(model.rightSleeve, helper.create3DMesh(img, arm, 12, 4, 40, 32, true, -2f),
                    slim ? dev.tr7zw.skinlayers.api.OffsetProvider.RIGHT_ARM_SLIM : dev.tr7zw.skinlayers.api.OffsetProvider.RIGHT_ARM);
        } catch (Throwable t) {
            broken = true;
            MushokuMod.LOG.warn("3D Skin Layers: no se pudo aplicar a los NPCs ({}), se usan capas 2D", t.toString());
        }
    }

    private static void inject(ModelPart part, dev.tr7zw.skinlayers.api.Mesh mesh, dev.tr7zw.skinlayers.api.OffsetProvider offset) {
        ((dev.tr7zw.skinlayers.accessor.ModelPartInjector) (Object) part).setInjectedMesh(mesh, offset);
    }
}
