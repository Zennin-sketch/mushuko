package dev.tr7zw.skinlayers.api;

import com.mojang.blaze3d.platform.NativeImage;

public interface MeshHelper {
    Mesh create3DMesh(NativeImage natImage, int width, int height, int depth, int textureU, int textureV, boolean topPivot, float rotationOffset);
}
