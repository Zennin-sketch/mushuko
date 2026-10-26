package dev.tr7zw.skinlayers.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;

/** Copia minima de 3D Skin Layers (solo para compilar el mixin; no va en el .jar). */
public class CustomizableModelPart {
    private void compile(ModelPart vanillaModel, PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color) {}
}
