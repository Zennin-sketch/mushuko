package dev.tr7zw.skinlayers.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

/** Copia minima de la API de 3D Skin Layers (solo para compilar; no va en el .jar). */
public class LayerFeatureTransformerAPI {
    public static LayerTransformer getTransformer() { return null; }

    public static void setLayerTransformer(LayerTransformer layerTransformer) {}

    @FunctionalInterface
    public interface LayerTransformer {
        void transform(AbstractClientPlayer player, PoseStack matrixStack, ModelPart modelPart);
    }
}
