package dev.tr7zw.skinlayers.api;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Copia minima de la API de 3D Skin Layers (solo para compilar; no va en el .jar). */
public interface MeshTransformer {
    void transform(Vector3f position, Vector4f[] vertexData);

    void transform(ModelPart.Cube cube);

    MeshTransformer EMPTY_TRANSFORMER = null;
}
