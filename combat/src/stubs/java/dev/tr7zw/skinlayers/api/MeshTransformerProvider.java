package dev.tr7zw.skinlayers.api;

import net.minecraft.client.model.geom.ModelPart;

/** Copia minima de la API de 3D Skin Layers (solo para compilar; no va en el .jar). */
public interface MeshTransformerProvider {
    MeshTransformer prepareTransformer(ModelPart vanillaModel);
}
