package org.helioviewer.jhv.opengl.model;

public record ModelSampler(MinFilter minFilter, MagFilter magFilter, Wrap wrapS, Wrap wrapT) {

    public enum MinFilter {
        NEAREST,
        LINEAR,
        NEAREST_MIPMAP_NEAREST,
        LINEAR_MIPMAP_NEAREST,
        NEAREST_MIPMAP_LINEAR,
        LINEAR_MIPMAP_LINEAR
    }

    public enum MagFilter {
        NEAREST,
        LINEAR
    }

    public enum Wrap {
        CLAMP_TO_EDGE,
        MIRRORED_REPEAT,
        REPEAT
    }

    public static final ModelSampler DEFAULT = new ModelSampler(MinFilter.LINEAR_MIPMAP_LINEAR, MagFilter.LINEAR, Wrap.REPEAT, Wrap.REPEAT);

}
