package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.zennin.mushoku.superd.ThrownSpearEntity;

/** Dibuja la lanza arrojada con su propio modelo, apuntando hacia donde vuela. */
public class ThrownSpearRenderer extends EntityRenderer<ThrownSpearEntity> {
    public ThrownSpearRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(ThrownSpearEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot()) - 90f));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
        if (e.returning()) pose.mulPose(Axis.YP.rotationDegrees(180f)); // vuelve con la punta hacia el duenio
        // el modelo de la lanza esta en diagonal (como las espadas): se endereza para que la punta vaya adelante (+X)
        pose.mulPose(Axis.ZP.rotationDegrees(-45f));
        pose.scale(1.6f, 1.6f, 1.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(e.getItem(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, e.level(), e.getId());
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownSpearEntity e) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
}
