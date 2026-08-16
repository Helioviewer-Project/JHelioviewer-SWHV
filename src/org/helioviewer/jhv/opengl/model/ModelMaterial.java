package org.helioviewer.jhv.opengl.model;

public record ModelMaterial(float red, float green, float blue, float alpha, int baseColorTexture, AlphaMode alphaMode, float alphaCutoff,
                            boolean doubleSided) {

    public static final int NO_TEXTURE = -1;

    public enum AlphaMode {
        OPAQUE,
        MASK,
        BLEND
    }

}
