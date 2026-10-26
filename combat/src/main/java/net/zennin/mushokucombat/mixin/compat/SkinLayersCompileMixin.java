package net.zennin.mushokucombat.mixin.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.zennin.mushokucombat.client.compat.LayerBendConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D Skin Layers: cada malla 3D se dibuja en compile(parte, pose, consumidor...). Si la parte esta doblada por Player
 * Animator (bendy-lib), se vuelve a llamar con un consumidor que dobla cada vertice igual que la parte.
 * Solo se aplica si 3D Skin Layers esta instalado (la config de este mixin no es obligatoria).
 */
@Mixin(targets = "dev.tr7zw.skinlayers.render.CustomizableModelPart", remap = false)
public abstract class SkinLayersCompileMixin {
    @Unique
    private static boolean mushokuCombat$inside;

    @Shadow(remap = false)
    private void compile(ModelPart vanillaModel, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, int color) {}

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true, remap = false)
    private void mushokuCombat$bend(ModelPart vanillaModel, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, int color,
                                    CallbackInfo ci) {
        if (mushokuCombat$inside) return;
        VertexConsumer bent = LayerBendConsumer.wrap(vanillaModel, pose, vc);
        if (bent == null) return;
        mushokuCombat$inside = true;
        try {
            compile(vanillaModel, pose, bent, light, overlay, color);
        } finally {
            mushokuCombat$inside = false;
        }
        ci.cancel();
    }
}
