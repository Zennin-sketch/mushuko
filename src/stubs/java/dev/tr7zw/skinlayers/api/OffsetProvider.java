package dev.tr7zw.skinlayers.api;

import com.mojang.blaze3d.vertex.PoseStack;

public interface OffsetProvider {
    OffsetProvider HEAD = null, BODY = null, LEFT_LEG = null, RIGHT_LEG = null,
            LEFT_ARM = null, RIGHT_ARM = null, LEFT_ARM_SLIM = null, RIGHT_ARM_SLIM = null;

    void applyOffset(PoseStack stack, Mesh mesh);
}
