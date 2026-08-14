package org.helioviewer.jhv.opengl.model;

import java.nio.ByteBuffer;

public record ModelTexture(String name, int width, int height, ByteBuffer rgba, ModelSampler sampler) {

    public ModelTexture {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid texture size: " + width + 'x' + height);
        if (rgba.remaining() != Math.multiplyExact(Math.multiplyExact(width, height), 4))
            throw new IllegalArgumentException("Texture buffer size does not match its dimensions");
        rgba = rgba.slice().asReadOnlyBuffer();
    }

    @Override
    public ByteBuffer rgba() {
        return rgba.duplicate();
    }

}
