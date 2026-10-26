package net.zennin.mushokucombat.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compatibilidad con 3D Skin Layers: la capa externa en 3D (chaqueta, mangas, pantalon) se dobla igual que el cuerpo
 * en las animaciones (codos, rodillas y cintura) y los brazos y la cabeza acompanian la inclinacion del torso.
 * Sin esto la capa 3D quedaba recta y "despegada" del personaje.
 */
public final class SkinLayersCompat {
    private static final Logger LOG = LoggerFactory.getLogger("mushoku_combat");

    private SkinLayersCompat() {}

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("skinlayers3d") || !FabricLoader.getInstance().isModLoaded("playeranimator")) return;
        try {
            SkinLayersBend.install();
            LOG.info("Compatibilidad con 3D Skin Layers activada (la capa 3D se dobla con las animaciones)");
        } catch (Throwable t) {
            LOG.warn("No se pudo activar la compatibilidad con 3D Skin Layers", t);
        }
    }
}
